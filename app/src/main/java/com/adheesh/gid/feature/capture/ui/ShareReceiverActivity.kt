package com.adheesh.gid.feature.capture.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.adheesh.gid.GIDApplication
import com.adheesh.gid.MainActivity
import com.adheesh.gid.R
import com.adheesh.gid.core.database.CaptureEntity
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.GIDTheme
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.notifications.Notifications
import kotlinx.coroutines.launch

/** What the share dialog is showing. */
private sealed interface ShareState {
    data object Saving : ShareState
    data class Saved(val captures: List<CaptureEntity>, val suggestedName: String?) : ShareState
    data object Failed : ShareState
}

// ── Share sheet entry (SPEC §2.8) ────────────────────────────────────────────
// GID appears in Android's share menu for text, links, images and PDFs. The
// capture is saved at once (on the app scope, so closing this dialog can't
// interrupt it) and a small card confirms it, with 'Sort now' to jump into GID.
class ShareReceiverActivity : ComponentActivity() {

    private val container get() = (application as GIDApplication).container
    private var state by mutableStateOf<ShareState>(ShareState.Saving)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Rotation doesn't recreate this activity (manifest configChanges). Being
        // recreated means Android killed us in the background: the capture was
        // already saved the first time, so just close instead of saving it twice.
        if (savedInstanceState != null) { finish(); return }
        save(intent)

        setContent {
            GIDTheme {
                // Tapping outside the card closes it, like a dialog.
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { finish() },
                ) {
                    ShareCard()
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ShareCard() {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
                .background(GIDSurface, MaterialTheme.shapes.medium)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .padding(20.dp),
        ) {
            when (val s = state) {
                ShareState.Saving -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = GIDCyan, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.capture_saved), color = GIDText, style = MaterialTheme.typography.titleMedium)
                }
                ShareState.Failed -> {
                    Text(stringResource(R.string.capture_failed), color = GIDText, style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { finish() }, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = GIDCyan, contentColor = OnCard)) {
                        Text(stringResource(R.string.action_done))
                    }
                }
                is ShareState.Saved -> {
                    Text(
                        s.suggestedName?.let { stringResource(R.string.capture_saved_suggested, it) } ?: stringResource(R.string.capture_saved),
                        color = GIDText,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { finish() }, shape = CircleShape, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.action_done), color = GIDText)
                        }
                        Button(
                            onClick  = { openLibrary(s.captures.map { it.id }) },
                            shape    = CircleShape,
                            colors   = ButtonDefaults.buttonColors(containerColor = GIDCyan, contentColor = OnCard),
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.capture_sort_now)) }
                    }
                }
            }
        }
    }

    private fun save(intent: Intent) {
        val actions = container.captureActions
        val onSaved: (List<CaptureEntity>) -> Unit = { saved ->
            lifecycleScope.launch {
                state = if (saved.isEmpty()) ShareState.Failed else {
                    val suggestion = saved.firstNotNullOfOrNull { it.suggestedSubjectId }
                    ShareState.Saved(saved, suggestion?.let { container.subjectRepository.get(it)?.name })
                }
            }
        }
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val stream = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (stream != null) {
                    actions.saveFiles(listOf(stream to intent.type), onSaved)
                } else {
                    val text = sharedText(intent)
                    if (text.isNullOrBlank()) state = ShareState.Failed
                    else actions.saveText(text) { onSaved(listOfNotNull(it)) }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
                if (uris.isEmpty()) state = ShareState.Failed
                else actions.saveFiles(uris.map { it to null }, onSaved)
            }
            else -> state = ShareState.Failed
        }
    }

    // Browsers share the page title as EXTRA_SUBJECT and the bare link as
    // EXTRA_TEXT; putting them together gives the capture a useful title.
    private fun sharedText(intent: Intent): String? {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        return when {
            text.isNullOrBlank() -> subject
            subject.isNullOrBlank() || text.contains(subject) -> text
            else -> "$subject\n$text"
        }
    }

    private fun openLibrary(captureIds: List<String>) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Notifications.EXTRA_OPEN, Notifications.OPEN_LIBRARY)
                .putExtra(Notifications.EXTRA_CAPTURE_IDS, captureIds.toTypedArray())
        )
        finish()
    }
}
