package com.adheesh.gid.feature.capture.ui

import com.adheesh.gid.feature.capture.domain.CaptureFileTypes
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.adheesh.gid.GIDApplication
import com.adheesh.gid.R
import com.adheesh.gid.core.database.CaptureEntity
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.GIDTextField
import com.adheesh.gid.core.designsystem.GIDTheme
import com.adheesh.gid.feature.library.ui.CaptureMenuItem
import kotlinx.coroutines.launch

// ── Quick capture from the widget and the Quick Settings tile (SPEC §2.11) ───
// A see-through activity over whatever the user was doing. It opens the camera,
// the file picker or a paste box straight away (or a small menu first), saves
// through the same path as the share sheet, and shows the same 'Saved' card.
// The fragment-version lint check is about FragmentActivity; this is a plain
// ComponentActivity, whose own result registry is what the launchers use.
@SuppressLint("InvalidFragmentVersionForActivityResult")
class QuickCaptureActivity : ComponentActivity() {

    private enum class Step { MENU, PASTE, WAITING, RESULT }

    private val container get() = (application as GIDApplication).container
    private val actions get() = container.captureActions

    private var step by mutableStateOf(Step.MENU)
    private var saveState by mutableStateOf<CaptureSaveState>(CaptureSaveState.Saving)
    private var pasteText by mutableStateOf("")
    private var clipboardChecked = false
    // Only the id survives a process restart while the camera is open; the file follows from it.
    private var cameraShotId: String? = null

    private val camera = registerForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val id = cameraShotId ?: return@registerForActivityResult finish()
        cameraShotId = null
        if (!taken) {
            actions.finishCameraShot(actions.cameraShot(id), taken = false)
            finish()
        } else {
            showSaving()
            actions.finishCameraShot(actions.cameraShot(id), taken = true) { onSaved(listOfNotNull(it)) }
        }
    }

    private val documents = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) finish()
        else {
            showSaving()
            actions.saveFiles(uris.map { it to null }, ::onSaved)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            // Recreated after Android killed us behind the camera or picker: wait for its result.
            cameraShotId = savedInstanceState.getString(KEY_SHOT)
            pasteText = savedInstanceState.getString(KEY_PASTE).orEmpty()
            step = Step.valueOf(savedInstanceState.getString(KEY_STEP) ?: Step.WAITING.name)
            // A save that was running is not ours to finish any more: just close.
            if (step == Step.RESULT) { finish(); return }
        } else {
            when (intent.getStringExtra(EXTRA_MODE)) {
                MODE_CAMERA -> startCamera()
                MODE_PHOTO  -> startPicker()
                MODE_PASTE  -> step = Step.PASTE
                else        -> step = Step.MENU
            }
        }

        setContent {
            GIDTheme {
                when (step) {
                    Step.WAITING -> Unit   // the camera or picker is on top
                    Step.MENU -> BottomCardHost(onOutside = ::finish) {
                        Text(stringResource(R.string.cd_capture), style = MaterialTheme.typography.titleLarge, color = GIDText)
                        CaptureMenuItem(Icons.Rounded.CameraAlt, stringResource(R.string.capture_camera), ::startCamera)
                        CaptureMenuItem(Icons.Rounded.PhotoLibrary, stringResource(R.string.capture_photo), ::startPicker)
                        CaptureMenuItem(Icons.Rounded.ContentPaste, stringResource(R.string.capture_paste)) { step = Step.PASTE }
                    }
                    Step.PASTE -> BottomCardHost(onOutside = ::finish) {
                        Text(stringResource(R.string.capture_paste), style = MaterialTheme.typography.titleLarge, color = GIDText)
                        GIDTextField(
                            value           = pasteText,
                            onValueChange   = { pasteText = it },
                            label           = stringResource(R.string.capture_paste_hint),
                            singleLine      = false,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = ::finish) { Text(stringResource(R.string.action_cancel), color = GIDMutedText) }
                            TextButton(enabled = pasteText.isNotBlank(), onClick = ::savePaste) {
                                Text(stringResource(R.string.action_save), color = GIDCyan)
                            }
                        }
                    }
                    Step.RESULT -> BottomCardHost(onOutside = ::finish) {
                        SaveStateContent(saveState, onDone = ::finish, onSortNow = ::openLibraryToSort)
                    }
                }
            }
        }
    }

    // Android only lets the app in front read the clipboard, so wait for focus.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || step != Step.PASTE || clipboardChecked || pasteText.isNotEmpty()) return
        clipboardChecked = true
        val clip = getSystemService(ClipboardManager::class.java)?.primaryClip
        val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
        if (!text.isNullOrBlank()) pasteText = text
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SHOT, cameraShotId)
        outState.putString(KEY_STEP, step.name)
        outState.putString(KEY_PASTE, pasteText)
    }

    private fun startCamera() {
        val shot = actions.newCameraShot()
        cameraShotId = shot.id
        step = Step.WAITING
        try { camera.launch(shot.uri) } catch (_: ActivityNotFoundException) { cameraShotId = null; showFailed() }
    }

    private fun startPicker() {
        step = Step.WAITING
        try { documents.launch(CaptureFileTypes.PICKABLE) } catch (_: ActivityNotFoundException) { showFailed() }
    }

    private fun savePaste() {
        val text = pasteText
        showSaving()
        actions.saveText(text) { onSaved(listOfNotNull(it)) }
    }

    private fun showSaving() { saveState = CaptureSaveState.Saving; step = Step.RESULT }
    private fun showFailed() { saveState = CaptureSaveState.Failed; step = Step.RESULT }

    private fun onSaved(saved: List<CaptureEntity>) {
        lifecycleScope.launch { saveState = savedState(saved, container.subjectRepository) }
    }

    companion object {
        const val EXTRA_MODE  = "com.adheesh.gid.CAPTURE_MODE"
        const val MODE_CAMERA = "camera"
        const val MODE_PHOTO  = "photo"
        const val MODE_PASTE  = "paste"
        const val MODE_MENU   = "menu"

        private const val KEY_SHOT  = "camera_shot_id"
        private const val KEY_STEP  = "step"
        private const val KEY_PASTE = "paste_text"

        fun intent(context: Context, mode: String): Intent =
            Intent(context, QuickCaptureActivity::class.java)
                .putExtra(EXTRA_MODE, mode)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
