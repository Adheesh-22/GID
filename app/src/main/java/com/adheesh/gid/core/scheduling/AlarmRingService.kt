package com.adheesh.gid.core.scheduling

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.adheesh.gid.GIDApplication
import com.adheesh.gid.R
import com.adheesh.gid.core.database.ActivityCategory
import com.adheesh.gid.core.notifications.Notifications
import com.adheesh.gid.core.util.formatMinutesOfDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the ringing screen shows. */
data class RingingAlarm(
    val alarmId  : String,
    val label    : String,
    val timeText : String,
    val typeId   : String?,
    val category : ActivityCategory,
)

// ── AlarmRingService ─────────────────────────────────────────────────────────
// A foreground service keeps the process alive while the alarm sound plays,
// even with the app swiped away. Its notification carries a full-screen
// intent, which Android uses to show AlarmRingActivity over the lock screen.
class AlarmRingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: MediaPlayer? = null
    private var timeoutJob: Job? = null

    private val container get() = (application as GIDApplication).container

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START   -> start(intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_ID), intent.getBooleanExtra(EXTRA_IS_SNOOZE, false))
            ACTION_DISMISS -> dismiss()
            ACTION_SNOOZE  -> snooze()
            else           -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun start(alarmId: String?, isSnooze: Boolean) {
        if (alarmId == null) { stopSelf(); return }
        // Android requires startForeground within seconds, so post a generic
        // notification first and fill in the label once the database answers.
        goForeground(buildNotification(getString(R.string.alarm_ringing_title), ""))
        startSound()

        scope.launch {
            val alarm = container.alarmRepository.getById(alarmId)
            val type  = alarm?.let { container.activityTypeRepository.getById(it.typeId) }
            val ringing = RingingAlarm(
                alarmId  = alarmId,
                label    = alarm?.label?.ifBlank { null } ?: type?.name ?: getString(R.string.alarm_ringing_title),
                timeText = alarm?.let { formatMinutesOfDay(it.hour * 60 + it.minute) }.orEmpty(),
                typeId   = type?.id,
                category = type?.let { ActivityCategory.of(it.category) } ?: ActivityCategory.OTHER,
            )
            _ringing.value = ringing
            goForeground(buildNotification(ringing.label, ringing.timeText))

            // Arm the next occurrence now; a one-off alarm switches itself off.
            if (alarm != null && !isSnooze) {
                if (alarm.repeatDaysMask == 0) {
                    container.alarmRepository.save(alarm.copy(enabled = false, updatedAt = System.currentTimeMillis()))
                } else {
                    container.alarmScheduler.schedule(alarm)
                }
            }
        }

        // Stop ringing after 10 minutes so a forgotten alarm doesn't drain the battery.
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(RING_TIMEOUT_MS)
            stopEverything()
        }
    }

    private fun dismiss() {
        val ringing = _ringing.value
        stopSound()
        scope.launch {
            // SPEC §3: dismissing a SLEEP/NAP alarm closes the running sleep.
            if (ringing?.category == ActivityCategory.SLEEP || ringing?.category == ActivityCategory.NAP) {
                container.sessionManager.closeSleepSessions()
            }
            stopEverything()
        }
    }

    private fun snooze() {
        _ringing.value?.let { container.alarmScheduler.snooze(it.alarmId) }
        stopEverything()
    }

    private fun stopEverything() {
        stopSound()
        timeoutJob?.cancel()
        _ringing.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopSound()
        _ringing.value = null
        scope.cancel()
        super.onDestroy()
    }

    // ── Sound and vibration ──────────────────────────────────────────────────

    private fun startSound() {
        stopSound()
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = runCatching {
            MediaPlayer().apply {
                // USAGE_ALARM uses the alarm volume, which Do Not Disturb lets through by default.
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmRingService, uri)
                isLooping = true
                prepare()
                start()
            }
        }.getOrNull()
        vibrator().vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 800), 0))
    }

    private fun stopSound() {
        player?.runCatching { stop(); release() }
        player = null
        vibrator().cancel()
    }

    private fun vibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    // ── Notification ─────────────────────────────────────────────────────────

    private fun goForeground(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, Notifications.ID_ALARM_RINGING, notification, type)
    }

    private fun buildNotification(title: String, text: String): Notification {
        val fullScreen = PendingIntent.getActivity(
            this, 0,
            Intent(this, AlarmRingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, Notifications.CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_stat_gid)
            .setColor(ContextCompat.getColor(this, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(0, getString(R.string.action_snooze), serviceIntent(this, ACTION_SNOOZE))
            .addAction(0, getString(R.string.action_dismiss), serviceIntent(this, ACTION_DISMISS))
            .build()
    }

    companion object {
        const val ACTION_START    = "com.adheesh.gid.action.ALARM_START"
        const val ACTION_DISMISS  = "com.adheesh.gid.action.ALARM_DISMISS"
        const val ACTION_SNOOZE   = "com.adheesh.gid.action.ALARM_SNOOZE"
        const val EXTRA_IS_SNOOZE = "is_snooze"
        private const val RING_TIMEOUT_MS = 10L * 60 * 1000

        private val _ringing = MutableStateFlow<RingingAlarm?>(null)
        /** The alarm currently ringing, observed by AlarmRingActivity. */
        val ringing: StateFlow<RingingAlarm?> = _ringing.asStateFlow()

        fun serviceIntent(context: Context, action: String): PendingIntent =
            PendingIntent.getService(
                context, action.hashCode(),
                Intent(context, AlarmRingService::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        fun send(context: Context, action: String) {
            context.startService(Intent(context, AlarmRingService::class.java).setAction(action))
        }
    }
}
