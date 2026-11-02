package com.adheesh.gid.feature.capture.data

import android.net.Uri
import com.adheesh.gid.core.database.CaptureEntity
import com.adheesh.gid.core.util.newId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

/** A camera shot in progress: the file the camera app writes into and the Uri it is given. */
data class PendingCameraShot(val id: String, val file: File, val uri: Uri)

// ── Saving captures from Home, Library and the share sheet ───────────────────
// Saves run on the app-wide scope, not a screen's, so leaving the screen (or
// the share dialog closing) can never lose a capture half-way through.
class CaptureActions(
    private val repository : CaptureRepository,
    private val files      : CaptureFileStore,
    private val appScope   : CoroutineScope,
) {
    fun saveText(text: String, onSaved: (CaptureEntity?) -> Unit = {}) {
        appScope.launch { onSaved(repository.captureText(text)) }
    }

    fun saveFiles(uris: List<Pair<Uri, String?>>, onSaved: (List<CaptureEntity>) -> Unit = {}) {
        appScope.launch { onSaved(uris.mapNotNull { (uri, mime) -> repository.captureFile(uri, mime) }) }
    }

    fun newCameraShot(): PendingCameraShot = cameraShot(newId())

    /** Rebuilds a shot from its id (the file name follows from it), e.g. after a process restart. */
    fun cameraShot(id: String): PendingCameraShot {
        val (file, uri) = files.newCameraFile(id)
        return PendingCameraShot(id, file, uri)
    }

    /** [taken] is false when the user backed out of the camera. */
    fun finishCameraShot(shot: PendingCameraShot, taken: Boolean, onSaved: (CaptureEntity?) -> Unit = {}) {
        appScope.launch {
            if (taken && shot.file.length() > 0) onSaved(repository.captureCameraPhoto(shot.id, shot.file))
            else { repository.discardFile(shot.file); onSaved(null) }
        }
    }
}
