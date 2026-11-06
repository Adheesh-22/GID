package com.adheesh.gid.core.scheduling

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Alarm log (SPEC §2.6) ────────────────────────────────────────────────────
// What happened on the way to the sound, kept on the phone and shown under
// 'Test alarm', because a silent alarm leaves no other trace. SharedPreferences
// because a receiver can write to it straight away, without a coroutine.
object AlarmDiagnostics {

    private const val TAG = "GIDAlarm"
    private const val PREFS = "alarm_log"
    private const val KEY = "lines"
    private const val MAX_LINES = 20

    @Synchronized
    fun log(context: Context, message: String) {
        Log.i(TAG, message)
        val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMM HH:mm:ss", Locale.getDefault()))
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lines = (prefs.getString(KEY, "").orEmpty().lines().filter { it.isNotBlank() } + "$time  $message").takeLast(MAX_LINES)
        prefs.edit { putString(KEY, lines.joinToString("\n")) }
    }

    /** Newest first. */
    fun read(context: Context): List<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()
            .lines().filter { it.isNotBlank() }.reversed()
}
