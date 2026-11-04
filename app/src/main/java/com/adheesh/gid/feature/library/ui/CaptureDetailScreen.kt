package com.adheesh.gid.feature.library.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.adheesh.gid.R
import com.adheesh.gid.core.database.CaptureKind
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurfaceHigh
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.util.formatMedium
import com.adheesh.gid.feature.library.viewmodel.CaptureDetailViewModel
import java.io.File
import java.time.LocalDate

// ── CaptureDetail (SPEC §2.8) ────────────────────────────────────────────────
@Composable
fun CaptureDetailScreen(
    viewModel : CaptureDetailViewModel,
    onBack    : () -> Unit,
    modifier  : Modifier = Modifier,
) {
    val capture     by viewModel.capture.collectAsState()
    val heading     by viewModel.heading.collectAsState()
    val subjectById by viewModel.subjectById.collectAsState()
    val context = LocalContext.current
    val current = capture ?: return

    var sortOpen   by rememberSaveable { mutableStateOf(false) }
    var deleteOpen by rememberSaveable { mutableStateOf(false) }
    val kind = CaptureKind.of(current.kind)
    val type = current.type?.let { LocalCategoryNames.current[it] }
    val subject = current.subjectId?.let { subjectById[it] }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back), tint = GIDText) }
            Text(current.displayTitle(), style = MaterialTheme.typography.titleLarge, color = GIDText, modifier = Modifier.weight(1f), maxLines = 2)
            IconButton(onClick = { deleteOpen = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.action_delete), tint = GIDCoral) }
        }

        if (kind == CaptureKind.PHOTO && current.filePath != null) {
            AsyncImage(
                model              = File(current.filePath),
                contentDescription = null,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.fillMaxWidth().heightIn(max = 520.dp).clip(MaterialTheme.shapes.medium),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (type != null) SmallChip(type, GIDSurfaceHigh, GIDText)
            if (subject != null) SmallChip(subject.name, subject.color(), OnCard)
            heading?.let { SmallChip(it.name, GIDSurfaceHigh, GIDText) }
        }
        Text(stringResource(R.string.capture_captured_at, current.whenText()), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
        current.dueEpochDay?.let {
            Text(stringResource(R.string.capture_due, LocalDate.ofEpochDay(it).formatMedium()), style = MaterialTheme.typography.bodyMedium, color = GIDCoral)
        }

        // Selectable so a pasted question or link can be copied back out.
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (current.title.isNotBlank() && current.note.isNotBlank()) {
                    Text(current.title, style = MaterialTheme.typography.titleMedium, color = GIDText)
                }
                if (current.note.isNotBlank()) Text(current.note, style = MaterialTheme.typography.bodyLarge, color = GIDText)
                current.url?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = GIDCyan) }
            }
        }

        current.url?.let { url ->
            Button(
                onClick  = { if (open(context, Intent(Intent.ACTION_VIEW, url.toUri()))) viewModel.markOpened() },
                shape    = CircleShape,
                colors   = ButtonDefaults.buttonColors(containerColor = GIDCyan, contentColor = OnCard),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.capture_open_link)) }
        }
        if ((kind == CaptureKind.PDF || kind == CaptureKind.DOCUMENT) && current.filePath != null) {
            Button(
                onClick  = {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", File(current.filePath))
                    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, current.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    if (open(context, intent)) viewModel.markOpened()
                },
                shape    = CircleShape,
                colors   = ButtonDefaults.buttonColors(containerColor = GIDCyan, contentColor = OnCard),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(if (kind == CaptureKind.PDF) R.string.capture_open_pdf else R.string.capture_open_document)) }
        }
        OutlinedButton(onClick = { sortOpen = true }, shape = CircleShape, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.capture_sort_again), color = GIDCyan)
        }
    }

    if (sortOpen) SortSheet(viewModel, listOf(current), onDismiss = { sortOpen = false })
    if (deleteOpen) {
        ConfirmDialog(
            title     = stringResource(R.string.capture_delete_title),
            body      = stringResource(R.string.capture_delete_body),
            confirm   = stringResource(R.string.action_delete),
            onDismiss = { deleteOpen = false },
            onConfirm = { deleteOpen = false; viewModel.delete(then = onBack) },
        )
    }
}

/** Starts another app; returns false (and says so) when none can handle it. */
private fun open(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    Toast.makeText(context, R.string.capture_no_app, Toast.LENGTH_SHORT).show()
    false
}
