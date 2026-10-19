package com.adheesh.gid.feature.settings.ui

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.adheesh.gid.R
import com.adheesh.gid.core.designsystem.GIDCard
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDHolidayGreen
import com.adheesh.gid.core.designsystem.GIDMustard
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.SectionTitle
import com.adheesh.gid.core.scheduling.ExactAlarms

/** Live permission state; re-read every time the screen resumes. */
data class ReliabilityStatus(
    val notifications : Boolean,
    val exactAlarms   : Boolean,
    val fullScreen    : Boolean,
    val battery       : Boolean,
) {
    companion object {
        fun read(context: Context): ReliabilityStatus {
            val power = context.getSystemService(PowerManager::class.java)
            val fullScreen = if (Build.VERSION.SDK_INT >= 34) {
                context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
            } else true
            return ReliabilityStatus(
                notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
                exactAlarms   = ExactAlarms.canScheduleExact(context),
                fullScreen    = fullScreen,
                battery       = power.isIgnoringBatteryOptimizations(context.packageName),
            )
        }
    }
}

// ── Settings > Reliability (SPEC §7) ─────────────────────────────────────────
// Alarms only ring on time if Android lets them, so this shows each permission
// with a button to the exact system screen that changes it.
@Composable
fun ReliabilitySection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(ReliabilityStatus.read(context)) }
    // Coming back from a system screen: refresh the ticks.
    LifecycleResumeEffect(Unit) {
        status = ReliabilityStatus.read(context)
        onPauseOrDispose { }
    }

    GIDCard(modifier = modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.reliability_title))
        Spacer(Modifier.size(8.dp))
        StatusRow(
            label   = stringResource(R.string.reliability_notifications),
            ok      = status.notifications,
            onFix   = { context.startSafely(notificationSettings(context)) },
        )
        StatusRow(
            label   = stringResource(R.string.reliability_exact),
            ok      = status.exactAlarms,
            onFix   = { context.startSafely(exactAlarmSettings(context)) },
        )
        if (Build.VERSION.SDK_INT >= 34) {
            StatusRow(
                label = stringResource(R.string.reliability_full_screen),
                ok    = status.fullScreen,
                onFix = { context.startSafely(fullScreenSettings(context)) },
            )
        }
        StatusRow(
            label   = stringResource(R.string.reliability_battery),
            ok      = status.battery,
            onFix   = { context.startSafely(batterySettings(context)) },
        )
        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.reliability_oem_note), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
        TextButton(onClick = { context.startSafely(appDetails(context)) }) {
            Text(stringResource(R.string.reliability_open_app_info), color = GIDCyan)
        }
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean, onFix: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Icon(
            imageVector        = if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
            contentDescription = stringResource(if (ok) R.string.cd_status_ok else R.string.cd_status_problem),
            tint               = if (ok) GIDHolidayGreen else GIDMustard,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = GIDText)
            Text(
                stringResource(if (ok) R.string.reliability_ok else R.string.reliability_needs_attention),
                style = MaterialTheme.typography.bodySmall,
                color = GIDMutedText,
            )
        }
        TextButton(onClick = onFix) {
            Text(stringResource(if (ok) R.string.action_open else R.string.action_fix), color = GIDCyan)
        }
    }
}

private fun packageUri(context: Context) = Uri.fromParts("package", context.packageName, null)

private fun notificationSettings(context: Context) =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

private fun exactAlarmSettings(context: Context) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context))
    else appDetails(context)

private fun fullScreenSettings(context: Context) =
    if (Build.VERSION.SDK_INT >= 34) Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri(context))
    else appDetails(context)

// An alarm app may ask to be exempt from battery optimisation; the system shows a yes/no dialog.
@SuppressLint("BatteryLife")
private fun batterySettings(context: Context): Intent {
    val power = context.getSystemService(PowerManager::class.java)
    return if (power.isIgnoringBatteryOptimizations(context.packageName)) {
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    } else {
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri(context))
    }
}

private fun appDetails(context: Context) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))

/** Some phones lack a screen; fall back to the app's info page instead of crashing. */
private fun Context.startSafely(intent: Intent) {
    val withFlags = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(withFlags) }.onFailure {
        runCatching { startActivity(appDetails(this).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
