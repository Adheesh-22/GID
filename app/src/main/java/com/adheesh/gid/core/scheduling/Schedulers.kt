package com.adheesh.gid.core.scheduling

import com.adheesh.gid.feature.attendance.data.AttendanceRepository
import com.adheesh.gid.feature.attendance.domain.AttendancePromptPlanner
import com.adheesh.gid.feature.attendance.domain.ClassSlot
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.adheesh.gid.core.database.AlarmEntity
import com.adheesh.gid.core.database.GIDPreferences
import com.adheesh.gid.core.notifications.Notifications
import com.adheesh.gid.core.util.today
import com.adheesh.gid.feature.alarm.data.AlarmRepository
import com.adheesh.gid.feature.alarm.domain.AlarmTimeCalculator
import com.adheesh.gid.feature.calendar.data.HolidayRepository
import com.adheesh.gid.feature.exams.data.ExamRepository
import com.adheesh.gid.feature.exams.domain.ExamReminderPlanner
import com.adheesh.gid.feature.exams.domain.PlannedExamReminder
import com.adheesh.gid.feature.timetable.data.TimetableRepository
import com.adheesh.gid.feature.timetable.domain.ReminderPlanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

// ── Exact-alarm helpers ──────────────────────────────────────────────────────
// Android 12+ can deny exact alarms. We always check first: when exact alarms
// are not allowed we fall back to an inexact alarm that still fires while the
// phone is idle, and Settings > Reliability tells the user how to fix it.
object ExactAlarms {
    fun canScheduleExact(context: Context): Boolean {
        val manager = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
    }

    /** Exact-while-idle when allowed, otherwise the best inexact option. */
    fun setWhileIdle(context: Context, triggerAt: Long, operation: PendingIntent) {
        val manager = context.getSystemService(AlarmManager::class.java)
        if (canScheduleExact(context)) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    /** setAlarmClock is the most reliable API: it survives Doze and shows the alarm icon. */
    fun setAlarmClock(context: Context, triggerAt: Long, operation: PendingIntent) {
        val manager = context.getSystemService(AlarmManager::class.java)
        if (canScheduleExact(context)) {
            val show = Notifications.openAppIntent(context, requestCode = 7)
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, show), operation)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    fun cancel(context: Context, operation: PendingIntent) {
        context.getSystemService(AlarmManager::class.java).cancel(operation)
    }
}

private const val PI_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

/** Lets a receiver run suspend code; Android keeps the process alive until finish(). */
fun BroadcastReceiver.launchAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try { block() } finally { pending.finish() }
    }
}

// ── Alarms (Phase 6) ─────────────────────────────────────────────────────────
class AlarmScheduler(
    private val context : Context,
    private val alarms  : AlarmRepository,
) {
    fun schedule(alarm: AlarmEntity) {
        if (!alarm.enabled) { cancel(alarm.id); return }
        val next = AlarmTimeCalculator.nextTrigger(alarm.hour, alarm.minute, alarm.repeatDaysMask, ZonedDateTime.now())
        ExactAlarms.setAlarmClock(context, next.toInstant().toEpochMilli(), ringIntent(alarm.id, snooze = false))
    }

    fun snooze(alarmId: String, minutes: Int = SNOOZE_MINUTES) {
        val at = System.currentTimeMillis() + minutes * 60_000L
        ExactAlarms.setAlarmClock(context, at, ringIntent(alarmId, snooze = true))
    }

    fun cancel(alarmId: String) {
        ExactAlarms.cancel(context, ringIntent(alarmId, snooze = false))
        ExactAlarms.cancel(context, ringIntent(alarmId, snooze = true))
    }

    suspend fun rescheduleAll() {
        alarms.observeAll().first().forEach { schedule(it) }
    }

    private fun ringIntent(alarmId: String, snooze: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(if (snooze) AlarmReceiver.ACTION_SNOOZE_RING else AlarmReceiver.ACTION_RING)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        return PendingIntent.getBroadcast(context, alarmId.hashCode(), intent, PI_FLAGS)
    }

    companion object {
        const val SNOOZE_MINUTES = 2
    }
}

// ── Timers (Phase 6) ─────────────────────────────────────────────────────────
// The end time lives in the session row; this only asks Android to wake us
// then, so a timer never depends on a running coroutine (SPEC §7).
class TimerScheduler(private val context: Context) {
    fun schedule(sessionId: String, endsAt: Long) =
        ExactAlarms.setWhileIdle(context, endsAt, intent(sessionId))

    fun cancel(sessionId: String) = ExactAlarms.cancel(context, intent(sessionId))

    private fun intent(sessionId: String): PendingIntent {
        val intent = Intent(context, TimerReceiver::class.java)
            .setAction(TimerReceiver.ACTION_TIMER_DONE)
            .putExtra(TimerReceiver.EXTRA_SESSION_ID, sessionId)
        return PendingIntent.getBroadcast(context, sessionId.hashCode(), intent, PI_FLAGS)
    }
}

// ── Class reminders (Phase 5) ────────────────────────────────────────────────
class ClassReminderScheduler(
    private val context     : Context,
    private val timetable   : TimetableRepository,
    private val holidays    : HolidayRepository,
    private val preferences : GIDPreferences,
) {
    /** Cancels the previous plan and schedules the next 7 days again. */
    suspend fun replan() {
        val previous = preferences.scheduledReminderCount.first()
        repeat(previous) { ExactAlarms.cancel(context, intent(it, null)) }

        val start = today().toEpochDay()
        val holidayDays = holidays.getHolidayDaysBetween(start, start + PLAN_DAYS).toSet()
        val plan = ReminderPlanner.plan(timetable.getAllEnabled(), holidayDays, ZonedDateTime.now(), PLAN_DAYS)
            .take(MAX_REMINDERS)

        plan.forEachIndexed { index, reminder ->
            ExactAlarms.setWhileIdle(context, reminder.triggerAt, intent(index, reminder))
        }
        preferences.setScheduledReminderCount(plan.size)
    }

    private fun intent(index: Int, reminder: com.adheesh.gid.feature.timetable.domain.PlannedReminder?): PendingIntent {
        val intent = Intent(context, ClassReminderReceiver::class.java).setAction(ClassReminderReceiver.ACTION_REMIND)
        if (reminder != null) {
            intent.putExtra(ClassReminderReceiver.EXTRA_INDEX, index)
                .putExtra(ClassReminderReceiver.EXTRA_SUBJECT, reminder.subject)
                .putExtra(ClassReminderReceiver.EXTRA_ROOM, reminder.room)
                .putExtra(ClassReminderReceiver.EXTRA_START, reminder.startMinutes)
        }
        return PendingIntent.getBroadcast(context, REQUEST_BASE + index, intent, PI_FLAGS)
    }

    private companion object {
        const val PLAN_DAYS = 7
        // Android caps alarms per app (about 500); a week of classes is far less.
        const val MAX_REMINDERS = 100
        const val REQUEST_BASE = 50_000
    }
}

// ── Exam reminders (SPEC §3 Exams) ───────────────────────────────────────────
// Same idea as class reminders: forget the previous plan, then arm the next
// reminders from scratch. The receiver reads the exam fresh when it fires.
class ExamReminderScheduler(
    private val context     : Context,
    private val exams       : ExamRepository,
    private val preferences : GIDPreferences,
) {
    suspend fun replan() {
        val previous = preferences.scheduledExamReminderCount.first()
        repeat(previous) { ExactAlarms.cancel(context, intent(it, null)) }

        val plan = ExamReminderPlanner.plan(exams.getUpcoming(), ZonedDateTime.now())
        plan.forEachIndexed { index, reminder ->
            ExactAlarms.setWhileIdle(context, reminder.triggerAt, intent(index, reminder))
        }
        preferences.setScheduledExamReminderCount(plan.size)
    }

    private fun intent(index: Int, reminder: PlannedExamReminder?): PendingIntent {
        val intent = Intent(context, ExamReminderReceiver::class.java).setAction(ExamReminderReceiver.ACTION_REMIND)
        if (reminder != null) {
            intent.putExtra(ExamReminderReceiver.EXTRA_EXAM_ID, reminder.examId)
                .putExtra(ExamReminderReceiver.EXTRA_STEP, reminder.step.name)
        }
        return PendingIntent.getBroadcast(context, REQUEST_BASE + index, intent, PI_FLAGS)
    }

    private companion object {
        const val REQUEST_BASE = 60_000
    }
}

// ── After-class attendance prompts (SPEC §2.12) ──────────────────────────────
// One 'Were you in …?' per class end for the next week. Re-planned after any
// mark or timetable change, so a class marked early loses its prompt.
class AttendancePromptScheduler(
    private val context     : Context,
    private val timetable   : TimetableRepository,
    private val holidays    : HolidayRepository,
    private val attendance  : () -> AttendanceRepository,
    private val preferences : GIDPreferences,
) {
    suspend fun replan() {
        val previous = preferences.scheduledAttendancePromptCount.first()
        repeat(previous) { ExactAlarms.cancel(context, intent(it, null)) }

        val start = today().toEpochDay()
        val holidayDays = holidays.getHolidayDaysBetween(start, start + PLAN_DAYS).toSet()
        val marked = attendance().markedBetween(start, start + PLAN_DAYS)
        val plan = AttendancePromptPlanner.plan(timetable.getAllEnabled(), holidayDays, marked, ZonedDateTime.now(), PLAN_DAYS)
        plan.forEachIndexed { index, prompt ->
            ExactAlarms.setWhileIdle(context, prompt.triggerAt, intent(index, prompt.slot))
        }
        preferences.setScheduledAttendancePromptCount(plan.size)
    }

    private fun intent(index: Int, slot: ClassSlot?): PendingIntent {
        val intent = Intent(context, AttendancePromptReceiver::class.java).setAction(AttendancePromptReceiver.ACTION_PROMPT)
        if (slot != null) AttendancePromptReceiver.putSlot(intent, slot)
        return PendingIntent.getBroadcast(context, REQUEST_BASE + index, intent, PI_FLAGS)
    }

    private companion object {
        const val PLAN_DAYS = 7
        const val REQUEST_BASE = 70_000
    }
}

// ── Evening prompt (Phase 9) ─────────────────────────────────────────────────
class EveningPromptScheduler(
    private val context     : Context,
    private val preferences : GIDPreferences,
) {
    suspend fun schedule() {
        val minutes = preferences.eveningPromptTime.first()
        val next = AlarmTimeCalculator.nextTrigger(minutes / 60, minutes % 60, 0, ZonedDateTime.now())
        ExactAlarms.setWhileIdle(context, next.toInstant().toEpochMilli(), intent())
    }

    private fun intent(): PendingIntent {
        val intent = Intent(context, EveningPromptReceiver::class.java).setAction(EveningPromptReceiver.ACTION_EVENING)
        return PendingIntent.getBroadcast(context, 0, intent, PI_FLAGS)
    }
}
