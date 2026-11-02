package com.adheesh.gid.feature.capture.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adheesh.gid.MainActivity
import com.adheesh.gid.R
import com.adheesh.gid.core.database.CaptureEntity
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.notifications.Notifications
import com.adheesh.gid.feature.subjects.data.SubjectRepository

/** What a capture card outside the app (share sheet, widget, tile) is showing. */
sealed interface CaptureSaveState {
    data object Saving : CaptureSaveState
    data class Saved(val captures: List<CaptureEntity>, val suggestedName: String?) : CaptureSaveState
    data object Failed : CaptureSaveState
}

/** Turns saved captures into the 'Saved' state, naming the suggested subject if there is one. */
suspend fun savedState(saved: List<CaptureEntity>, subjects: SubjectRepository): CaptureSaveState =
    if (saved.isEmpty()) CaptureSaveState.Failed
    else CaptureSaveState.Saved(saved, saved.firstNotNullOfOrNull { it.suggestedSubjectId }?.let { subjects.get(it)?.name })

// ── A card at the bottom of a see-through activity (SPEC §2.8, §2.11) ────────
// Tapping outside the card closes it, like a dialog.
@Composable
fun BottomCardHost(onOutside: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOutside),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
                .imePadding()
                .background(GIDSurface, MaterialTheme.shapes.medium)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .padding(20.dp),
            content = content,
        )
    }
}

/** 'Saving…', then 'Saved to Inbox · DBMS suggested' with Done and Sort now. */
@Composable
fun ColumnScope.SaveStateContent(state: CaptureSaveState, onDone: () -> Unit, onSortNow: (List<String>) -> Unit) {
    when (state) {
        CaptureSaveState.Saving -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = GIDCyan, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.capture_saved), color = GIDText, style = MaterialTheme.typography.titleMedium)
        }
        CaptureSaveState.Failed -> {
            Text(stringResource(R.string.capture_failed), color = GIDText, style = MaterialTheme.typography.titleMedium)
            Button(onClick = onDone, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = GIDCyan, contentColor = OnCard)) {
                Text(stringResource(R.string.action_done))
            }
        }
        is CaptureSaveState.Saved -> {
            Text(
                state.suggestedName?.let { stringResource(R.string.capture_saved_suggested, it) } ?: stringResource(R.string.capture_saved),
                color = GIDText,
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDone, shape = CircleShape, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_done), color = GIDText)
                }
                Button(
                    onClick  = { onSortNow(state.captures.map { it.id }) },
                    shape    = CircleShape,
                    colors   = ButtonDefaults.buttonColors(containerColor = GIDCyan, contentColor = OnCard),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.capture_sort_now)) }
            }
        }
    }
}

/** 'Sort now': open GID's Library with the Sort sheet for these captures, then close. */
fun Activity.openLibraryToSort(captureIds: List<String>) {
    startActivity(
        Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(Notifications.EXTRA_OPEN, Notifications.OPEN_LIBRARY)
            .putExtra(Notifications.EXTRA_CAPTURE_IDS, captureIds.toTypedArray())
    )
    finish()
}
