package com.adheesh.gid.feature.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.database.HeadingEntity
import com.adheesh.gid.core.database.SubjectEntity
import com.adheesh.gid.core.designsystem.ActivityTypePalette
import com.adheesh.gid.core.designsystem.Dot
import com.adheesh.gid.core.designsystem.EmptyText
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDDialog
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.SectionTitle
import com.adheesh.gid.feature.library.viewmodel.SubjectDetailViewModel

// ── SubjectDetail (SPEC §2.8) ────────────────────────────────────────────────
// Headings in order, each with its items, then Unfiled. The menu renames,
// recolors or archives the subject; each heading has its own small menu.
@Composable
fun SubjectDetailScreen(
    viewModel     : SubjectDetailViewModel,
    onBack        : () -> Unit,
    onOpenCapture : (String) -> Unit,
    modifier      : Modifier = Modifier,
) {
    val subject  by viewModel.subject.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val headings by viewModel.headings.collectAsState()
    val current = subject ?: return
    val subjectById = remember(current) { mapOf(current.id to current) }

    var menuOpen       by rememberSaveable { mutableStateOf(false) }
    var renameOpen     by rememberSaveable { mutableStateOf(false) }
    var colorOpen      by rememberSaveable { mutableStateOf(false) }
    var archiveOpen    by rememberSaveable { mutableStateOf(false) }
    var deleteOpen     by rememberSaveable { mutableStateOf(false) }
    var addHeadingOpen by rememberSaveable { mutableStateOf(false) }
    var renamingHeadingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingHeadingId by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier            = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding      = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back), tint = GIDText)
                }
                Dot(current.color(), size = 14.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    current.name,
                    style    = MaterialTheme.typography.displayMedium,
                    color    = GIDText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.cd_more), tint = GIDText) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = GIDSurface) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.subject_rename), color = GIDText) }, onClick = { menuOpen = false; renameOpen = true })
                        DropdownMenuItem(text = { Text(stringResource(R.string.subject_color), color = GIDText) }, onClick = { menuOpen = false; colorOpen = true })
                        DropdownMenuItem(text = { Text(stringResource(R.string.subject_archive), color = GIDCoral) }, onClick = { menuOpen = false; archiveOpen = true })
                        DropdownMenuItem(text = { Text(stringResource(R.string.subject_delete), color = GIDCoral) }, onClick = { menuOpen = false; deleteOpen = true })
                    }
                }
            }
        }

        if (sections.all { it.items.isEmpty() }) item { EmptyText(stringResource(R.string.subject_empty)) }

        sections.forEach { section ->
            val heading = section.heading
            item(key = "h-${heading?.id ?: "unfiled"}") {
                HeadingHeader(
                    heading  = heading,
                    isFirst  = heading != null && headings.firstOrNull()?.id == heading.id,
                    isLast   = heading != null && headings.lastOrNull()?.id == heading.id,
                    onRename = { heading?.let { renamingHeadingId = it.id } },
                    onMove   = { up -> heading?.let { viewModel.moveHeading(it, up) } },
                    onDelete = { heading?.let { deletingHeadingId = it.id } },
                )
            }
            items(section.items, key = { it.id }) { capture ->
                CaptureRow(capture, subjectById, selected = false, onClick = { onOpenCapture(capture.id) })
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(CircleShape)
                    .border(1.dp, GIDCyan, CircleShape)
                    .clickable { addHeadingOpen = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = GIDCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.sort_new_heading), color = GIDCyan, style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────
    if (renameOpen) NameDialog(stringResource(R.string.subject_rename), initial = current.name, onDismiss = { renameOpen = false }) { viewModel.rename(it) }
    if (addHeadingOpen) NameDialog(stringResource(R.string.sort_new_heading), onDismiss = { addHeadingOpen = false }) { viewModel.addHeading(it) }
    headings.firstOrNull { it.id == renamingHeadingId }?.let { h ->
        NameDialog(stringResource(R.string.heading_rename), initial = h.name, onDismiss = { renamingHeadingId = null }) { viewModel.renameHeading(h, it) }
    }
    if (colorOpen) ColorDialog(current, onDismiss = { colorOpen = false }) { viewModel.setColor(it); colorOpen = false }
    if (deleteOpen) {
        ConfirmDialog(
            title     = stringResource(R.string.subject_delete),
            body      = stringResource(R.string.subject_delete_body, current.name),
            confirm   = stringResource(R.string.action_delete),
            onDismiss = { deleteOpen = false },
            onConfirm = { viewModel.delete(); deleteOpen = false; onBack() },
        )
    }
    if (archiveOpen) {
        ConfirmDialog(
            title     = stringResource(R.string.subject_archive),
            body      = stringResource(R.string.subject_archive_body),
            confirm   = stringResource(R.string.subject_archive),
            onDismiss = { archiveOpen = false },
            onConfirm = { viewModel.archive(); archiveOpen = false; onBack() },
        )
    }
    deletingHeadingId?.let { id ->
        ConfirmDialog(
            title     = stringResource(R.string.heading_delete_title),
            body      = stringResource(R.string.heading_delete_body),
            confirm   = stringResource(R.string.action_delete),
            onDismiss = { deletingHeadingId = null },
            onConfirm = { viewModel.deleteHeading(id); deletingHeadingId = null },
        )
    }
}

@Composable
private fun HeadingHeader(
    heading  : HeadingEntity?,
    isFirst  : Boolean,
    isLast   : Boolean,
    onRename : () -> Unit,
    onMove   : (up: Boolean) -> Unit,
    onDelete : () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        SectionTitle(heading?.name ?: stringResource(R.string.subject_unfiled), Modifier.weight(1f), color = if (heading == null) GIDMutedText else GIDText)
        if (heading != null) {
            Box {
                IconButton(onClick = { open = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.cd_more), tint = GIDMutedText) }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = GIDSurface) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.heading_rename), color = GIDText) }, onClick = { open = false; onRename() })
                    if (!isFirst) DropdownMenuItem(text = { Text(stringResource(R.string.cd_move_up), color = GIDText) }, onClick = { open = false; onMove(true) })
                    if (!isLast) DropdownMenuItem(text = { Text(stringResource(R.string.cd_move_down), color = GIDText) }, onClick = { open = false; onMove(false) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_delete), color = GIDCoral) }, onClick = { open = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun ColorDialog(subject: SubjectEntity, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    GIDDialog(title = stringResource(R.string.subject_color), onDismiss = onDismiss) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            ActivityTypePalette.forEach { (hex, color) ->
                val picked = subject.colorHex.equals(hex, ignoreCase = true)
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(if (picked) Modifier.border(3.dp, GIDText, CircleShape) else Modifier)
                        .clickable { onPick(hex) }
                )
            }
        }
    }
}

@Composable
fun ConfirmDialog(title: String, body: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = GIDSurface,
        shape            = MaterialTheme.shapes.medium,
        title            = { Text(title, color = GIDText) },
        text             = { Text(body, color = GIDMutedText) },
        confirmButton    = { TextButton(onClick = onConfirm) { Text(confirm, color = GIDCoral) } },
        dismissButton    = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = GIDMutedText) } },
    )
}
