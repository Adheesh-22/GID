package com.adheesh.gid.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.adheesh.gid.MainActivity
import com.adheesh.gid.R
import com.adheesh.gid.core.database.ExamEntity
import com.adheesh.gid.core.database.ExamKind
import com.adheesh.gid.core.scheduling.SessionActionReceiver
import com.adheesh.gid.core.util.formatMinutesOfDay
import com.adheesh.gid.feature.exams.domain.ExamReminderStep

// ── Notifications ────────────────────────────────────────────────────────────
// One place that owns channels, ids and the notification builders, so
// receivers and services stay short. A channel is the user-visible category
// Android uses for per-type notification settings (sound, importance).
object Notifications {

    const val CHANNEL_ALARMS   = "alarms"
    const val CHANNEL_TIMERS   = "timers"
    const val CHANNEL_CLASSES  = "class_reminders"
    const val CHANNEL_SESSIONS = "sessions"
    const val CHANNEL_EVENING  = "evening_prompt"
    const val CHANNEL_EXAMS    = "exam_reminders"

    const val ID_ALARM_RINGING = 1000
    const val ID_EVENING       = 3000
    private const val ID_CLASS_BASE = 2000
    private const val ID_EXAM_BASE  = 4000

    /** Extra on MainActivity intents telling it which dialog to open. */
    const val EXTRA_OPEN       = "com.adheesh.gid.OPEN"
    const val OPEN_PROGRESS    = "update_progress"
    const val OPEN_LIBRARY     = "library"
    /** With OPEN_LIBRARY: captures to open the Sort sheet for (share dialog's 'Sort now'). */
    const val EXTRA_CAPTURE_IDS = "com.adheesh.gid.CAPTURE_IDS"
    /** Opens ExamDetail for [EXTRA_EXAM_ID]. */
    const val OPEN_EXAM        = "exam"
    const val EXTRA_EXAM_ID    = "com.adheesh.gid.EXAM_ID"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val soundAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        manager.createNotificationChannels(
            listOf(
                // The ringing sound is played by AlarmRingService, so the
                // channel itself is silent to avoid two sounds at once.
                NotificationChannel(CHANNEL_ALARMS, context.getString(R.string.channel_alarms), NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(null, null)
                    enableVibration(false)
                },
                NotificationChannel(CHANNEL_TIMERS, context.getString(R.string.channel_timers), NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(alarmSound, soundAttrs)
                },
                NotificationChannel(CHANNEL_CLASSES, context.getString(R.string.channel_classes), NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CHANNEL_SESSIONS, context.getString(R.string.channel_sessions), NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(CHANNEL_EVENING, context.getString(R.string.channel_evening), NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_EXAMS, context.getString(R.string.channel_exams), NotificationManager.IMPORTANCE_HIGH),
            )
        )
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Opens the app; [open] picks a dialog to show (see MainActivity). */
    fun openAppIntent(context: Context, requestCode: Int = 0, open: String? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (open != null) putExtra(EXTRA_OPEN, open)
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun base(context: Context, channel: String) =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_gid)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))

    @Suppress("MissingPermission") // canPost() is checked first
    private fun post(context: Context, id: Int, builder: NotificationCompat.Builder) {
        if (!canPost(context)) return
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    fun cancel(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)

    // ── Class reminders (Phase 5) ────────────────────────────────────────────
    fun showClassReminder(context: Context, index: Int, subject: String, room: String?, startMinutes: Int) {
        val time = formatMinutesOfDay(startMinutes)
        val text = if (room.isNullOrBlank()) context.getString(R.string.notif_class_text, time)
        else context.getString(R.string.notif_class_text_room, time, room)
        post(
            context, ID_CLASS_BASE + index,
            base(context, CHANNEL_CLASSES)
                .setContentTitle(subject)
                .setContentText(text)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openAppIntent(context))
                .setAutoCancel(true),
        )
    }

    // ── Timers and sessions (Phase 6) ────────────────────────────────────────
    fun showTimerRunning(context: Context, sessionId: String, typeName: String, endsAt: Long) {
        post(
            context, sessionId.hashCode(),
            base(context, CHANNEL_SESSIONS)
                .setContentTitle(context.getString(R.string.notif_timer_running, typeName))
                .setContentText(context.getString(R.string.notif_timer_ends))
                .setWhen(endsAt)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setOngoing(true)
                .setContentIntent(openAppIntent(context))
                .addAction(
                    0, context.getString(R.string.action_stop),
                    SessionActionReceiver.pendingIntent(context, SessionActionReceiver.ACTION_STOP_TIMER, sessionId),
                ),
        )
    }

    fun showTimerDone(context: Context, sessionId: String, typeName: String) {
        post(
            context, sessionId.hashCode(),
            base(context, CHANNEL_TIMERS)
                .setContentTitle(context.getString(R.string.notif_timer_done, typeName))
                .setContentText(context.getString(R.string.notif_timer_done_text))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openAppIntent(context))
                .setAutoCancel(true),
        )
    }

    fun showSleeping(context: Context, sessionId: String, typeName: String, startedAt: Long) {
        post(
            context, sessionId.hashCode(),
            base(context, CHANNEL_SESSIONS)
                .setContentTitle(context.getString(R.string.notif_sleeping, typeName))
                .setContentText(context.getString(R.string.notif_sleeping_text))
                .setWhen(startedAt)
                .setUsesChronometer(true)
                .setOngoing(true)
                .setContentIntent(openAppIntent(context))
                .addAction(
                    0, context.getString(R.string.action_im_awake),
                    SessionActionReceiver.pendingIntent(context, SessionActionReceiver.ACTION_IM_AWAKE, sessionId),
                ),
        )
    }

    // ── Exam reminders (SPEC §3 Exams) ───────────────────────────────────────
    fun showExamReminder(context: Context, exam: ExamEntity, subjectName: String?, step: ExamReminderStep, savedItems: Int) {
        val submitted = ExamKind.of(exam.kind).isSubmitted
        val title = listOfNotNull(subjectName, exam.title.ifBlank { null }).joinToString(" · ")
        val details = listOfNotNull(exam.timeMinutes?.let { formatMinutesOfDay(it) }, exam.room).joinToString(" · ")
        val headline = context.getString(
            when (step) {
                ExamReminderStep.WEEK_BEFORE -> R.string.notif_exam_week
                ExamReminderStep.DAY_BEFORE  -> if (submitted) R.string.notif_due_tomorrow else R.string.notif_exam_tomorrow
                ExamReminderStep.DAY_OF      -> if (submitted) R.string.notif_due_today else R.string.notif_exam_today
            }
        )
        val text = buildString {
            append(headline)
            if (details.isNotEmpty() && step != ExamReminderStep.WEEK_BEFORE) append(" · ").append(details)
            if (step == ExamReminderStep.WEEK_BEFORE && savedItems > 0) {
                append("\n").append(context.resources.getQuantityString(R.plurals.notif_exam_saved, savedItems, savedItems))
            }
        }
        val id = ID_EXAM_BASE + (exam.id.hashCode() and 0x0FFF)
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN, OPEN_EXAM)
            putExtra(EXTRA_EXAM_ID, exam.id)
        }
        post(
            context, id,
            base(context, CHANNEL_EXAMS)
                .setContentTitle(title.ifBlank { context.getString(R.string.exams_title) })
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                .setAutoCancel(true),
        )
    }

    // ── Evening prompt (Phase 9) ─────────────────────────────────────────────
    // With unsorted captures waiting, it also asks to clear the Inbox (SPEC §2.8).
    fun showEveningPrompt(context: Context, unsortedCaptures: Int) {
        val text = if (unsortedCaptures > 0) {
            context.getString(R.string.notif_evening_text) + "\n" +
                context.resources.getQuantityString(R.plurals.notif_evening_inbox, unsortedCaptures, unsortedCaptures)
        } else context.getString(R.string.notif_evening_text)
        val builder = base(context, CHANNEL_EVENING)
            .setContentTitle(context.getString(R.string.notif_evening_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(context, requestCode = ID_EVENING, open = OPEN_PROGRESS))
            .setAutoCancel(true)
        if (unsortedCaptures > 0) {
            builder.addAction(0, context.getString(R.string.action_sort_inbox), openAppIntent(context, requestCode = ID_EVENING + 1, open = OPEN_LIBRARY))
        }
        post(context, ID_EVENING, builder)
    }
}
