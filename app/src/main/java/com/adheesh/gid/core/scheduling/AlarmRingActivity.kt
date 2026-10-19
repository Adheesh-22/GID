package com.adheesh.gid.core.scheduling

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.adheesh.gid.GIDApplication
import com.adheesh.gid.R
import com.adheesh.gid.core.database.ActivityCategory
import com.adheesh.gid.core.designsystem.GIDBackground
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDPeriwinkle
import com.adheesh.gid.core.designsystem.GIDTheme
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.OnCard
import kotlinx.coroutines.launch

// ── Full-screen ringing screen ───────────────────────────────────────────────
// Shown over the lock screen by the service's full-screen intent. It only
// sends Dismiss / Snooze to the service; the service owns the sound.
class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        setContent {
            GIDTheme {
                val ringing by AlarmRingService.ringing.collectAsState()
                // After a FOCUS alarm is dismissed we offer a session (SPEC §3).
                var focusOffer by remember { mutableStateOf<RingingAlarm?>(null) }
                var seenRinging by remember { mutableStateOf(false) }

                LaunchedEffect(ringing) {
                    if (ringing != null) seenRinging = true
                    // The service stopped (timeout, notification button): close.
                    if (ringing == null && seenRinging && focusOffer == null) finish()
                }

                Surface(color = GIDBackground, modifier = Modifier.fillMaxSize()) {
                    val offer = focusOffer
                    if (offer != null) {
                        FocusOffer(
                            onPick = { minutes -> startFocus(offer, minutes) },
                            onSkip = { finish() },
                        )
                    } else {
                        RingingContent(
                            alarm     = ringing,
                            onSnooze  = {
                                AlarmRingService.send(this, AlarmRingService.ACTION_SNOOZE)
                                finish()
                            },
                            onDismiss = {
                                val current = ringing
                                AlarmRingService.send(this, AlarmRingService.ACTION_DISMISS)
                                if (current?.category == ActivityCategory.FOCUS && current.typeId != null) {
                                    focusOffer = current
                                } else {
                                    finish()
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    private fun startFocus(alarm: RingingAlarm, minutes: Int) {
        val container = (application as GIDApplication).container
        lifecycleScope.launch {
            val type = alarm.typeId?.let { container.activityTypeRepository.getById(it) }
            if (type != null) container.sessionManager.startTimer(type, minutes)
            finish()
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
private fun RingingContent(alarm: RingingAlarm?, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.Alarm,
            contentDescription = null,
            tint = GIDPeriwinkle,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        Text(alarm?.timeText.orEmpty(), style = MaterialTheme.typography.displayLarge, color = GIDText)
        Spacer(Modifier.height(8.dp))
        Text(
            alarm?.label ?: stringResource(R.string.alarm_ringing_title),
            style = MaterialTheme.typography.titleLarge,
            color = GIDText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(containerColor = GIDCyan, contentColor = OnCard),
        ) { Text(stringResource(R.string.action_dismiss), style = MaterialTheme.typography.labelLarge) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onSnooze,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(
                stringResource(R.string.action_snooze_minutes, AlarmScheduler.SNOOZE_MINUTES),
                style = MaterialTheme.typography.labelLarge,
                color = GIDText,
            )
        }
    }
}

@Composable
private fun FocusOffer(onPick: (Int) -> Unit, onSkip: () -> Unit) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.focus_offer_title), style = MaterialTheme.typography.titleLarge, color = GIDText)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.focus_offer_text), style = MaterialTheme.typography.bodyMedium, color = GIDMutedText)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(25, 45, 60).forEach { minutes ->
                Button(
                    onClick = { scope.launch { onPick(minutes) } },
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = GIDPeriwinkle, contentColor = OnCard),
                ) { Text(stringResource(R.string.minutes_short, minutes)) }
            }
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onSkip) { Text(stringResource(R.string.action_no_thanks), color = GIDMutedText) }
    }
}
