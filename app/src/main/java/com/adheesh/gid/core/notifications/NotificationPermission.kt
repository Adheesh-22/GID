package com.adheesh.gid.core.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.adheesh.gid.R
import com.adheesh.gid.core.database.GIDPreferences
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDText
import kotlinx.coroutines.launch

/**
 * Android 13+ asks before an app may post notifications. We explain why first
 * (once), then show the system prompt. Later changes go through Reliability.
 */
@Composable
fun NotificationPermissionPrompt(preferences: GIDPreferences) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val shown by preferences.notificationRationaleShown.collectAsState(initial = true)
    var dismissed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    if (shown || dismissed || Notifications.canPost(context)) return

    fun done() {
        dismissed = true
        scope.launch { preferences.setNotificationRationaleShown() }
    }

    AlertDialog(
        onDismissRequest = { done() },
        containerColor   = GIDSurface,
        shape            = MaterialTheme.shapes.medium,
        title            = { Text(stringResource(R.string.notif_rationale_title), color = GIDText) },
        text             = { Text(stringResource(R.string.notif_rationale_text), color = GIDMutedText) },
        confirmButton    = {
            TextButton(onClick = {
                done()
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) { Text(stringResource(R.string.action_continue), color = GIDCyan) }
        },
        dismissButton    = {
            TextButton(onClick = { done() }) { Text(stringResource(R.string.action_not_now), color = GIDMutedText) }
        },
    )
}
