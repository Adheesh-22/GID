package com.adheesh.gid.feature.library.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.adheesh.gid.R
import com.adheesh.gid.core.database.CaptureEntity
import com.adheesh.gid.core.database.CaptureKind
import com.adheesh.gid.core.database.CaptureType
import com.adheesh.gid.core.database.SubjectEntity
import com.adheesh.gid.core.designsystem.ActivityTypePalette
import com.adheesh.gid.core.designsystem.Dot
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDDialog
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDSurfaceHigh
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.GIDTextField
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.designsystem.colorFromHex
import com.adheesh.gid.core.util.formatShort
import com.adheesh.gid.core.util.formatTimeOfMillis
import com.adheesh.gid.core.util.toLocalDate
import com.adheesh.gid.feature.capture.data.CaptureActions
import com.adheesh.gid.feature.capture.data.PendingCameraShot
import java.io.File

// ── Small pieces shared by the Library screens, Home and the share dialog ────

@Composable
fun CaptureType.label(): String = stringResource(
    when (this) {
        CaptureType.EXAM_QUESTION -> R.string.type_exam_question
        CaptureType.RESOURCE      -> R.string.type_resource
        CaptureType.NOTE          -> R.string.type_note
        CaptureType.DEADLINE      -> R.string.type_deadline
        CaptureType.IDEA          -> R.string.type_idea
        CaptureType.NOTICE        -> R.string.type_notice
    }
)

/** A subject's own color, or a steady pick from the palette so it never changes between screens. */
fun SubjectEntity.color(): Color =
    colorHex?.let { colorFromHex(it) } ?: ActivityTypePalette[Math.floorMod(id.hashCode(), ActivityTypePalette.size)].second

/** What to call a capture in lists: its title, else the start of its note, its link's site, or its kind. */
@Composable
fun CaptureEntity.displayTitle(): String = title.ifBlank {
    note.lineSequence().firstOrNull { it.isNotBlank() }?.trim()
        ?: url?.let { runCatching { it.toUri().host?.removePrefix("www.") }.getOrNull() ?: it }
        ?: stringResource(
            when (CaptureKind.of(kind)) {
                CaptureKind.PHOTO -> R.string.capture_untitled_photo
                CaptureKind.PDF   -> R.string.capture_untitled_pdf
                else              -> R.string.capture_untitled_text
            }
        )
}

fun CaptureEntity.whenText(): String = "${capturedAt.toLocalDate().formatShort()} · ${formatTimeOfMillis(capturedAt)}"

/** Photo thumbnail, or an icon for links, text and PDFs. */
@Composable
fun CaptureThumb(capture: CaptureEntity, size: Dp, modifier: Modifier = Modifier) {
    val kind = CaptureKind.of(capture.kind)
    Box(
        modifier = modifier.size(size).clip(MaterialTheme.shapes.small).background(GIDSurfaceHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (kind == CaptureKind.PHOTO && capture.filePath != null) {
            AsyncImage(
                model              = File(capture.filePath),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.size(size),
            )
        } else {
            val icon: ImageVector = when (kind) {
                CaptureKind.LINK -> Icons.Rounded.Link
                CaptureKind.PDF  -> Icons.Rounded.PictureAsPdf
                else             -> Icons.AutoMirrored.Rounded.Notes
            }
            Icon(icon, contentDescription = null, tint = GIDCyan)
        }
    }
}

/** One row in the Inbox, a subject page or search results. Long-press selects. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CaptureRow(
    capture     : CaptureEntity,
    subjectById : Map<String, SubjectEntity>,
    selected    : Boolean,
    onClick     : () -> Unit,
    modifier    : Modifier = Modifier,
    onLongClick : (() -> Unit)? = null,
    onDelete    : (() -> Unit)? = null,
) {
    val type = CaptureType.ofOrNull(capture.type)
    val subject = (capture.subjectId ?: capture.suggestedSubjectId)?.let { subjectById[it] }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(GIDSurface)
            .then(if (selected) Modifier.border(2.dp, GIDCyan, MaterialTheme.shapes.small) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(10.dp),
    ) {
        CaptureThumb(capture, 52.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                capture.displayTitle(),
                style    = MaterialTheme.typography.bodyLarge,
                color    = GIDText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(capture.whenText(), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (type != null) SmallChip(type.label(), GIDSurfaceHigh, GIDText)
                if (subject != null) {
                    val isSuggestion = capture.subjectId == null
                    SmallChip(
                        text      = if (isSuggestion) stringResource(R.string.library_suggested, subject.name) else subject.name,
                        container = if (isSuggestion) GIDSurfaceHigh else subject.color(),
                        content   = if (isSuggestion) GIDMutedText else OnCard,
                        dot       = if (isSuggestion) subject.color() else null,
                    )
                }
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.cd_delete_capture), tint = GIDMutedText)
            }
        }
    }
}

@Composable
fun SmallChip(text: String, container: Color, content: Color, dot: Color? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(CircleShape).background(container).padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        if (dot != null) { Dot(dot, size = 6.dp); Spacer(Modifier.width(4.dp)) }
        Text(text, style = MaterialTheme.typography.labelSmall, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ── Capture button: camera, photo/PDF, or pasted text (SPEC §2.8) ────────────
// The activity-result launchers live here, in a composable that stays on screen,
// not inside the menu dialog: the dialog closes before the camera returns.
@Composable
fun CaptureButton(
    actions  : CaptureActions,
    onSaved  : (List<CaptureEntity>) -> Unit,
    modifier : Modifier = Modifier,
) {
    var menuOpen  by rememberSaveable { mutableStateOf(false) }
    var pasteOpen by rememberSaveable { mutableStateOf(false) }
    // Only the id survives a process restart while the camera is open; the file path follows from it.
    var cameraShotId by rememberSaveable { mutableStateOf<String?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        cameraShotId?.let { id -> actions.finishCameraShot(actions.cameraShot(id), taken) { onSaved(listOfNotNull(it)) } }
        cameraShotId = null
    }
    val documents = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) actions.saveFiles(uris.map { it to null }, onSaved)
    }

    FloatingActionButton(
        onClick        = { menuOpen = true },
        containerColor = GIDCyan,
        contentColor   = OnCard,
        shape          = CircleShape,
        modifier       = modifier,
    ) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.cd_capture)) }

    if (menuOpen) {
        GIDDialog(title = stringResource(R.string.cd_capture), onDismiss = { menuOpen = false }) {
            CaptureMenuItem(Icons.Rounded.CameraAlt, stringResource(R.string.capture_camera)) {
                menuOpen = false
                val shot: PendingCameraShot = actions.newCameraShot()
                cameraShotId = shot.id
                camera.launch(shot.uri)
            }
            CaptureMenuItem(Icons.Rounded.PhotoLibrary, stringResource(R.string.capture_photo)) {
                menuOpen = false
                documents.launch(arrayOf("image/*", "application/pdf"))
            }
            CaptureMenuItem(Icons.Rounded.ContentPaste, stringResource(R.string.capture_paste)) {
                menuOpen = false
                pasteOpen = true
            }
        }
    }

    if (pasteOpen) {
        var text by rememberSaveable { mutableStateOf("") }
        GIDDialog(
            title     = stringResource(R.string.capture_paste),
            onDismiss = { pasteOpen = false },
            actions   = {
                TextButton(
                    enabled = text.isNotBlank(),
                    onClick = {
                        actions.saveText(text) { onSaved(listOfNotNull(it)) }
                        pasteOpen = false
                    },
                ) { Text(stringResource(R.string.action_save), color = GIDCyan) }
            },
        ) {
            GIDTextField(
                value           = text,
                onValueChange   = { text = it },
                label           = stringResource(R.string.capture_paste_hint),
                singleLine      = false,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
        }
    }
}

@Composable
internal fun CaptureMenuItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = GIDCyan)
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = GIDText)
    }
}
