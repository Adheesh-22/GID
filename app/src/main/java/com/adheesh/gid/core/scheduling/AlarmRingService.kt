package com.adheesh.gid.core.scheduling

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.adheesh.gid.GIDApplication
import com.adheesh.gid.R
import com.adheesh.gid.core.database.AlarmEntity
import com.adheesh.gid.core.notifications.Notifications
import com.adheesh.gid.core.util.formatMinutesOfDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.ceil

/** What the ringing screen shows. */
data class RingingAlarm(
    val alarmId        : String,
    val label          : String,
    val timeText       : String,
    /** Dismiss and Snooze stay hidden until the pattern is drawn (SPEC §2.6). */
    val requirePattern : Boolean,
    val snoozeMinutes  : Int,
    /** A finished timer rings the same way, but has nothing to snooze. */
    val isTimer        : Boolean = false,
)

// ── AlarmRingService ─────────────────────────────────────────────────────────
// A foreground service keeps the process alive while the alarm sound plays,
// even with the app swiped away. Its notification carries a full-screen
// intent, which Android uses to show AlarmRingActivity over the lock screen.
//
// Nothing here may end in silence: a wake lock is taken before anything else,
// a slow database can't hold up the sound, and every sound has a fallback.
class AlarmRingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: MediaPlayer? = null
    private var tone: ToneGenerator? = null
    private var toneJob: Job? = null
    private var timeoutJob: Job? = null
    private var focusRequest: AudioFocusRequest? = null
    private var restoreVolume: Int? = null

    private val container get() = (application as GIDApplication).container

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START       -> start(intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_ID), intent.getBooleanExtra(EXTRA_IS_SNOOZE, false))
            ACTION_START_TIMER -> startTimer(intent.getStringExtra(EXTRA_SESSION_ID))
            ACTION_DISMISS     -> dismiss()
            ACTION_SNOOZE      -> snooze()
            else               -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun start(alarmId: String?, isSnooze: Boolean) {
        if (alarmId == null) { stopSelf(); return }
        holdWakeLock(this)
        // Android requires startForeground within seconds, so post a generic
        // notification first and fill in the label once the database answers.
        goForeground(buildNotification(getString(R.string.alarm_ringing_title), "", showDismiss = false, showSnooze = false))
        Notifications.cancel(this, Notifications.ID_ALARM_BACKUP)
        AlarmDiagnostics.log(this, "Service started")

        scope.launch {
            // The sound can't wait on the database: after a moment, ring the default tone.
            val read = async { runCatching { container.alarmRepository.getById(alarmId) }.getOrNull() }
            val quick = withTimeoutOrNull(DB_WAIT_MS) { read.await() }
            startSound(quick?.ringtoneUri)
            val alarm = quick ?: read.await()

            val ringing = RingingAlarm(
                alarmId        = alarmId,
                label          = alarm?.label?.ifBlank { null } ?: getString(R.string.alarm_ringing_title),
                timeText       = alarm?.let { formatMinutesOfDay(it.hour * 60 + it.minute) }.orEmpty(),
                requirePattern = alarm?.requirePattern ?: false,
                snoozeMinutes  = alarm?.snoozeMinutes ?: AlarmEntity.DEFAULT_SNOOZE_MINUTES,
            )
            _ringing.value = ringing
            val text = if (ringing.requirePattern) getString(R.string.alarm_tap_to_stop) else ringing.timeText
            val free = !ringing.requirePattern
            goForeground(buildNotification(ringing.label, text, showDismiss = free, showSnooze = free))

            // Arm the next occurrence now; a one-off alarm switches itself off.
            if (alarm != null && !isSnooze) {
                if (alarm.repeatDaysMask == 0) {
                    container.alarmRepository.save(alarm.copy(enabled = false, updatedAt = System.currentTimeMillis()))
                } else {
                    container.alarmScheduler.schedule(alarm)
                }
            }
        }
        armTimeout()
    }

    /** A timer reached its end: ring until it is stopped, with nothing to snooze. */
    private fun startTimer(sessionId: String?) {
        if (sessionId == null) { stopSelf(); return }
        holdWakeLock(this)
        // A ringing alarm's pattern must not be skipped because a timer ended, and
        // Android still expects startForeground for this start: repeat its notification.
        val current = _ringing.value
        val busyWithAlarm = current?.isTimer == false
        goForeground(
            if (current != null && busyWithAlarm) {
                val free = !current.requirePattern
                val text = if (free) current.timeText else getString(R.string.alarm_tap_to_stop)
                buildNotification(current.label, text, showDismiss = free, showSnooze = free)
            } else {
                buildNotification(getString(R.string.alarm_ringing_title), "", showDismiss = false, showSnooze = false)
            }
        )
        AlarmDiagnostics.log(this, "Timer service started")

        scope.launch {
            val session = runCatching { container.sessionRepository.getById(sessionId) }.getOrNull()
            val name = session?.let { runCatching { container.activityTypeRepository.getById(it.typeId) }.getOrNull()?.name }.orEmpty()
            if (busyWithAlarm) {
                Notifications.showTimerDone(this@AlarmRingService, sessionId, name)
                return@launch
            }
            startSound(null)
            val label = getString(R.string.notif_timer_done, name).trim()
            _ringing.value = RingingAlarm(sessionId, label, "", requirePattern = false, snoozeMinutes = 0, isTimer = true)
            goForeground(buildNotification(label, getString(R.string.notif_timer_done_text), showDismiss = true, showSnooze = false))
        }
        if (!busyWithAlarm) armTimeout()
    }

    /** Stop ringing after 10 minutes so a forgotten alarm doesn't drain the battery. */
    private fun armTimeout() {
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
            // Waking up ends any sleep still being counted (SPEC §3).
            if (ringing?.isTimer == false) container.sessionManager.closeSleepSessions()
            stopEverything()
        }
    }

    private fun snooze() {
        val ringing = _ringing.value
        if (ringing == null || ringing.isTimer) return
        container.alarmScheduler.snooze(ringing.alarmId, ringing.snoozeMinutes)
        stopEverything()
    }

    private fun stopEverything() {
        stopSound()
        timeoutJob?.cancel()
        _ringing.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        releaseWakeLock()
        stopSelf()
    }

    override fun onDestroy() {
        stopSound()
        _ringing.value = null
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    // ── Sound and vibration ──────────────────────────────────────────────────

    /** Plays the alarm's own tone, then each fallback; a beep if none of them will play. */
    private fun startSound(ringtoneUri: String?) {
        stopSound()
        val audio = getSystemService(AudioManager::class.java)
        raiseVolume(audio)
        requestFocus(audio)

        val attempts = listOfNotNull(
            ringtoneUri?.toUri(),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            firstSystemAlarmTone(),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
        )
        player = attempts.firstNotNullOfOrNull { play(it) }
        if (player == null) startBeeping()
        AlarmDiagnostics.log(this, if (player != null) "Sound playing, alarm volume ${audio.getStreamVolume(AudioManager.STREAM_ALARM)}/${audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)}" else "No tone would play: beeping")

        vibrator().vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 800), 0))
    }

    private fun play(uri: Uri): MediaPlayer? {
        val player = MediaPlayer()
        return runCatching {
            player.apply {
                // USAGE_ALARM uses the alarm volume, which Do Not Disturb lets through by default.
                setAudioAttributes(ALARM_ATTRIBUTES)
                setDataSource(this@AlarmRingService, uri)
                isLooping = true
                prepare()
                start()
            }
        }.getOrElse { error ->
            AlarmDiagnostics.log(this, "Tone failed ($uri): ${error.javaClass.simpleName}")
            player.release()
            null
        }
    }

    private fun firstSystemAlarmTone(): Uri? = runCatching {
        val manager = RingtoneManager(this).apply { setType(RingtoneManager.TYPE_ALARM) }
        manager.cursor.use { if (it.moveToFirst()) manager.getRingtoneUri(it.position) else null }
    }.getOrNull()

    private fun startBeeping() {
        tone = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME) }.getOrNull()
        toneJob = scope.launch {
            while (isActive) {
                tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, BEEP_MILLIS)
                delay(BEEP_MILLIS * 2L)
            }
        }
    }

    /**
     * The alarm slider is separate from media volume and can sit at zero while the
     * phone looks loud. Lifted only for this ring; the old level comes back after.
     */
    private fun raiseVolume(audio: AudioManager) {
        runCatching {
            val floor = ceil(audio.getStreamMaxVolume(AudioManager.STREAM_ALARM) * MIN_VOLUME_SHARE).toInt()
            val current = audio.getStreamVolume(AudioManager.STREAM_ALARM)
            if (current < floor) {
                audio.setStreamVolume(AudioManager.STREAM_ALARM, floor, 0)
                restoreVolume = current
            }
        }.onFailure { AlarmDiagnostics.log(this, "Couldn't raise alarm volume: ${it.javaClass.simpleName}") }
    }

    private fun requestFocus(audio: AudioManager) {
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(ALARM_ATTRIBUTES)
            .build()
            .also { runCatching { audio.requestAudioFocus(it) } }
    }

    private fun stopSound() {
        player?.runCatching { stop(); release() }
        player = null
        toneJob?.cancel()
        toneJob = null
        tone?.runCatching { release() }
        tone = null
        vibrator().cancel()
        val audio = getSystemService(AudioManager::class.java)
        focusRequest?.let { request -> runCatching { audio.abandonAudioFocusRequest(request) } }
        focusRequest = null
        restoreVolume?.let { level -> runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, level, 0) } }
        restoreVolume = null
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

    /** With a pattern required both actions stay off, so the shade can't skip it. */
    private fun buildNotification(title: String, text: String, showDismiss: Boolean, showSnooze: Boolean): Notification {
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
            .apply {
                if (showSnooze) addAction(0, getString(R.string.action_snooze), serviceIntent(this@AlarmRingService, ACTION_SNOOZE))
                if (showDismiss) addAction(0, getString(R.string.action_dismiss), serviceIntent(this@AlarmRingService, ACTION_DISMISS))
            }
            .build()
    }

    companion object {
        const val ACTION_START       = "com.adheesh.gid.action.ALARM_START"
        const val ACTION_START_TIMER = "com.adheesh.gid.action.TIMER_RING"
        const val ACTION_DISMISS     = "com.adheesh.gid.action.ALARM_DISMISS"
        const val ACTION_SNOOZE      = "com.adheesh.gid.action.ALARM_SNOOZE"
        const val EXTRA_IS_SNOOZE    = "is_snooze"
        const val EXTRA_SESSION_ID   = "session_id"
        private const val RING_TIMEOUT_MS = 10L * 60 * 1000
        private const val DB_WAIT_MS = 1_500L
        private const val BEEP_MILLIS = 800
        private const val MIN_VOLUME_SHARE = 0.7

        private val ALARM_ATTRIBUTES = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        private val _ringing = MutableStateFlow<RingingAlarm?>(null)
        /** The alarm currently ringing, observed by AlarmRingActivity. */
        val ringing: StateFlow<RingingAlarm?> = _ringing.asStateFlow()

        private var wakeLock: PowerManager.WakeLock? = null

        /**
         * Keeps the CPU on from the moment the alarm fires: once a receiver returns,
         * nothing else stops a locked phone sleeping before the sound has begun.
         */
        @Synchronized
        @SuppressLint("WakelockTimeout") // it has one: a little longer than the ring itself
        fun holdWakeLock(context: Context) {
            val lock = wakeLock ?: context.applicationContext.getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GID:alarm")
                .apply { setReferenceCounted(false) }
                .also { wakeLock = it }
            lock.acquire(RING_TIMEOUT_MS + 60_000L)
        }

        @Synchronized
        private fun releaseWakeLock() {
            wakeLock?.takeIf { it.isHeld }?.release()
        }

        /**
         * Starts the ring. False if Android refused, as some phones do in the
         * background; the caller then falls back to a loud notification.
         */
        fun startRinging(context: Context, intent: Intent): Boolean =
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure {
                    AlarmDiagnostics.log(context, "Service refused: ${it.javaClass.simpleName}")
                    releaseWakeLock()
                }
                .isSuccess

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
