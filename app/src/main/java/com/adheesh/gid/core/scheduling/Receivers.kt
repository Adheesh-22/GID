package com.adheesh.gid.core.scheduling

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.adheesh.gid.GIDApplication
import com.adheesh.gid.app.AppContainer
import com.adheesh.gid.core.database.AttendanceStatus
import com.adheesh.gid.core.notifications.Notifications
import com.adheesh.gid.feature.attendance.domain.ClassSlot
import com.adheesh.gid.feature.exams.domain.ExamReminderStep
import java.util.concurrent.TimeUnit

private val Context.container: AppContainer
    get() = (applicationContext as GIDApplication).container

/** setAlarmClock fired: start the ringing service (allowed from an alarm). */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: return
        AlarmDiagnostics.log(context, "Alarm fired")
        // Before anything else: Android only keeps the phone awake until this method returns.
        AlarmRingService.holdWakeLock(context)
        val service = Intent(context, AlarmRingService::class.java)
            .setAction(AlarmRingService.ACTION_START)
            .putExtra(EXTRA_ALARM_ID, alarmId)
            .putExtra(AlarmRingService.EXTRA_IS_SNOOZE, intent.action == ACTION_SNOOZE_RING)
        if (!AlarmRingService.startRinging(context, service)) {
            // Android refused the service: a loud notification opens the ringing screen,
            // which may start the service from the foreground.
            Notifications.showAlarmBackup(context, alarmId)
            launchAsync { context.container.rearm(alarmId) }
        }
    }

    companion object {
        const val ACTION_RING        = "com.adheesh.gid.action.RING"
        const val ACTION_SNOOZE_RING = "com.adheesh.gid.action.SNOOZE_RING"
        const val EXTRA_ALARM_ID     = "alarm_id"
    }
}

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        AlarmDiagnostics.log(context, "Timer fired")
        AlarmRingService.holdWakeLock(context)
        val service = Intent(context, AlarmRingService::class.java)
            .setAction(AlarmRingService.ACTION_START_TIMER)
            .putExtra(AlarmRingService.EXTRA_SESSION_ID, sessionId)
        val ringing = AlarmRingService.startRinging(context, service)
        // Saves the session; the plain notification is only for when it can't ring.
        launchAsync { context.container.sessionManager.finishTimer(sessionId, notify = !ringing) }
    }

    companion object {
        const val ACTION_TIMER_DONE = "com.adheesh.gid.action.TIMER_DONE"
        const val EXTRA_SESSION_ID  = "session_id"
    }
}

class ClassReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val subject = intent.getStringExtra(EXTRA_SUBJECT) ?: return
        Notifications.showClassReminder(
            context,
            index        = intent.getIntExtra(EXTRA_INDEX, 0),
            subject      = subject,
            room         = intent.getStringExtra(EXTRA_ROOM),
            startMinutes = intent.getIntExtra(EXTRA_START, 0),
        )
    }

    companion object {
        const val ACTION_REMIND = "com.adheesh.gid.action.CLASS_REMINDER"
        const val EXTRA_INDEX   = "index"
        const val EXTRA_SUBJECT = "subject"
        const val EXTRA_ROOM    = "room"
        const val EXTRA_START   = "start_minutes"
    }
}

class ExamReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val examId = intent.getStringExtra(EXTRA_EXAM_ID) ?: return
        val step = ExamReminderStep.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_STEP) } ?: return
        launchAsync {
            val container = context.container
            // Read it now: it may have been edited, done or deleted since it was planned.
            val exam = container.examRepository.get(examId)?.takeIf { it.completedAt == null } ?: return@launchAsync
            val subject = container.subjectRepository.get(exam.subjectId)
            val saved = container.database.captureDao().countForSubject(exam.subjectId)
            Notifications.showExamReminder(context, exam, subject?.name, step, saved)
        }
    }

    companion object {
        const val ACTION_REMIND = "com.adheesh.gid.action.EXAM_REMINDER"
        const val EXTRA_EXAM_ID = "exam_id"
        const val EXTRA_STEP    = "step"
    }
}

class EveningPromptReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        launchAsync {
            Notifications.showEveningPrompt(context, context.container.captureRepository.inboxCount())
            // Arm tomorrow's prompt.
            context.container.eveningPromptScheduler.schedule()
        }
    }

    companion object {
        const val ACTION_EVENING = "com.adheesh.gid.action.EVENING_PROMPT"
    }
}

/** A class just ended: ask 'Were you in …?' unless it is already marked. */
class AttendancePromptReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val slot = slotOf(intent) ?: return
        launchAsync {
            val container = context.container
            // Read it now: the class, holidays and marks may have changed since it was planned.
            val entry = container.timetableRepository.get(slot.entryId)?.takeIf { it.enabled } ?: return@launchAsync
            if (container.holidayRepository.getHoliday(slot.dateEpochDay) != null) return@launchAsync
            if (container.attendanceRepository.isMarked(slot)) return@launchAsync
            Notifications.showAttendancePrompt(context, slot, entry.subject, entry.startMinutes, entry.endMinutes)
        }
    }

    companion object {
        const val ACTION_PROMPT = "com.adheesh.gid.action.ATTENDANCE_PROMPT"
        private const val EXTRA_ENTRY = "entry_id"
        private const val EXTRA_DAY   = "day"
        private const val EXTRA_START = "start_minutes"

        fun putSlot(intent: Intent, slot: ClassSlot): Intent = intent
            .putExtra(EXTRA_ENTRY, slot.entryId)
            .putExtra(EXTRA_DAY, slot.dateEpochDay)
            .putExtra(EXTRA_START, slot.startMinutes)

        fun slotOf(intent: Intent): ClassSlot? {
            val entryId = intent.getStringExtra(EXTRA_ENTRY) ?: return null
            return ClassSlot(entryId, intent.getLongExtra(EXTRA_DAY, 0), intent.getIntExtra(EXTRA_START, 0))
        }
    }
}

/** Present / Absent / Cancelled on the after-class notification. */
class AttendanceActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val slot = AttendancePromptReceiver.slotOf(intent) ?: return
        val status = AttendanceStatus.of(intent.getStringExtra(EXTRA_STATUS)) ?: return
        launchAsync {
            val container = context.container
            val entry = container.timetableRepository.get(slot.entryId) ?: return@launchAsync
            val mark = container.attendanceRepository.mark(entry, slot.dateEpochDay, status)
            val percent = container.attendanceRepository.summaryFor(mark.subjectId)?.summary?.percent
            Notifications.showAttendanceMarked(context, slot, entry.subject, status, percent)
        }
    }

    companion object {
        private const val ACTION_MARK  = "com.adheesh.gid.action.ATTENDANCE_MARK"
        private const val EXTRA_STATUS = "status"

        fun pendingIntent(context: Context, slot: ClassSlot, status: AttendanceStatus): PendingIntent {
            val intent = AttendancePromptReceiver.putSlot(
                Intent(context, AttendanceActionReceiver::class.java).setAction(ACTION_MARK),
                slot,
            ).putExtra(EXTRA_STATUS, status.name)
            // One request code per class and button, so the three buttons don't overwrite each other.
            return PendingIntent.getBroadcast(
                context, slot.hashCode() * 4 + status.ordinal, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}

/** Notification buttons: 'I'm awake' and 'Stop' on timers. */
class SessionActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        launchAsync { context.container.sessionManager.stop(sessionId) }
    }

    companion object {
        const val ACTION_IM_AWAKE   = "com.adheesh.gid.action.IM_AWAKE"
        const val ACTION_STOP_TIMER = "com.adheesh.gid.action.STOP_TIMER"
        private const val EXTRA_SESSION_ID = "session_id"

        fun pendingIntent(context: Context, action: String, sessionId: String): PendingIntent {
            val intent = Intent(context, SessionActionReceiver::class.java)
                .setAction(action)
                .putExtra(EXTRA_SESSION_ID, sessionId)
            return PendingIntent.getBroadcast(
                context, sessionId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}

/**
 * Android forgets every AlarmManager alarm on reboot, and times shift when the
 * clock or time zone changes, so we rebuild the whole plan (SPEC §7).
 */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
            -> launchAsync { context.container.rescheduleEverything() }
        }
    }
}

/** Once a day: re-plan the rolling 7-day reminders and close stale sessions. */
class DailyMaintenanceWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        applicationContext.container.rescheduleEverything()
        return Result.success()
    }

    companion object {
        private const val NAME = "daily_maintenance"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyMaintenanceWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
