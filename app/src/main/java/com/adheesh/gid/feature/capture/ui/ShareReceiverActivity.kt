package com.adheesh.gid.feature.capture.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.adheesh.gid.GIDApplication
import com.adheesh.gid.core.database.CaptureEntity
import com.adheesh.gid.core.designsystem.GIDTheme
import kotlinx.coroutines.launch

// ── Share sheet entry (SPEC §2.8) ────────────────────────────────────────────
// GID appears in Android's share menu for text, links, images and PDFs. The
// capture is saved at once (on the app scope, so closing this dialog can't
// interrupt it) and a small card confirms it, with 'Sort now' to jump into GID.
class ShareReceiverActivity : ComponentActivity() {

    private val container get() = (application as GIDApplication).container
    private var state by mutableStateOf<CaptureSaveState>(CaptureSaveState.Saving)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Rotation doesn't recreate this activity (manifest configChanges). Being
        // recreated means Android killed us in the background: the capture was
        // already saved the first time, so just close instead of saving it twice.
        if (savedInstanceState != null) { finish(); return }
        save(intent)

        setContent {
            GIDTheme {
                BottomCardHost(onOutside = ::finish) {
                    SaveStateContent(state, onDone = ::finish, onSortNow = ::openLibraryToSort)
                }
            }
        }
    }

    private fun save(intent: Intent) {
        val actions = container.captureActions
        val onSaved: (List<CaptureEntity>) -> Unit = { saved ->
            lifecycleScope.launch { state = savedState(saved, container.subjectRepository) }
        }
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val stream = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (stream != null) {
                    actions.saveFiles(listOf(stream to intent.type), onSaved)
                } else {
                    val text = sharedText(intent)
                    if (text.isNullOrBlank()) state = CaptureSaveState.Failed
                    else actions.saveText(text) { onSaved(listOfNotNull(it)) }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
                if (uris.isEmpty()) state = CaptureSaveState.Failed
                else actions.saveFiles(uris.map { it to null }, onSaved)
            }
            else -> state = CaptureSaveState.Failed
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
}
