package com.adheesh.gid.core.scheduling

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.adheesh.gid.GIDApplication
import com.adheesh.gid.app.AppContainer
import com.adheesh.gid.core.notifications.Notifications
import java.util.concurrent.TimeUnit

private val Context.container: AppContainer
    get() = (applicationContext as GIDApplication).container

/** setAlarmClock fired: start the ringing service (allowed from an alarm). */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: return
        val service = Intent(context, AlarmRingService::class.java)
            .setAction(AlarmRingService.ACTION_START)
            .putExtra(EXTRA_ALARM_ID, alarmId)
            .putExtra(AlarmRingService.EXTRA_IS_SNOOZE, intent.action == ACTION_SNOOZE_RING)
        ContextCompat.startForegroundService(context, service)
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
        launchAsync { context.container.sessionManager.finishTimer(sessionId) }
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

class EveningPromptReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Notifications.showEveningPrompt(context)
        // Arm tomorrow's prompt.
        launchAsync { context.container.eveningPromptScheduler.schedule() }
    }

    companion object {
        const val ACTION_EVENING = "com.adheesh.gid.action.EVENING_PROMPT"
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
