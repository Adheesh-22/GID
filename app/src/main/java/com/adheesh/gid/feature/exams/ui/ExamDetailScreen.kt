package com.adheesh.gid.feature.exams.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.database.ExamKind
import com.adheesh.gid.core.designsystem.Dot
import com.adheesh.gid.core.designsystem.EmptyText
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDSurfaceHigh
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.designsystem.SectionTitle
import com.adheesh.gid.feature.exams.viewmodel.ExamDetailViewModel
import com.adheesh.gid.feature.library.ui.CaptureRow
import com.adheesh.gid.feature.library.ui.ConfirmDialog
import com.adheesh.gid.feature.library.ui.SmallChip
import com.adheesh.gid.feature.library.ui.color

// ── One exam or assignment (SPEC §2.10) ──────────────────────────────────────
// Details, 'Mark done' / 'Mark submitted', and the subject's saved Library
// items underneath so revision starts from one place.
@Composable
fun ExamDetailScreen(
    viewModel     : ExamDetailViewModel,
    onBack        : () -> Unit,
    onOpenCapture : (String) -> Unit,
    modifier      : Modifier = Modifier,
) {
    val exam        by viewModel.exam.collectAsState()
    val subjectById by viewModel.subjectById.collectAsState()
    val saved       by viewModel.savedItems.collectAsState()
    val current = exam ?: return
    val subject = subjectById[current.subjectId]
    val kind = ExamKind.of(current.kind)
    val done = current.completedAt != null

    var menuOpen   by rememberSaveable { mutableStateOf(false) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var deleteOpen by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier            = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding      = PaddingValues(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back), tint = GIDText)
                }
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.cd_more), tint = GIDText) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = GIDSurface) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.exam_edit), color = GIDText) }, onClick = { menuOpen = false; editorOpen = true })
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_delete), color = GIDCoral) }, onClick = { menuOpen = false; deleteOpen = true })
                    }
                }
            }
        }

        item {
            if (subject != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(subject.color(), size = 12.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(subject.name, style = MaterialTheme.typography.titleMedium, color = GIDMutedText)
                }
            }
        }
        item {
            Text(
                current.displayTitle(),
                style    = MaterialTheme.typography.displayMedium,
                color    = GIDText,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SmallChip(kind.label(), GIDSurfaceHigh, GIDText)
                if (done) SmallChip(stringResource(if (kind.isSubmitted) R.string.exam_submitted else R.string.exam_done), GIDCyan, OnCard)
                else CountdownChip(current.countdown())
            }
        }
        item { Text(current.whenText(), style = MaterialTheme.typography.bodyLarge, color = GIDText) }
        if (current.notes.isNotBlank()) {
            item { Text(current.notes, style = MaterialTheme.typography.bodyMedium, color = GIDMutedText) }
        }

        item {
            Button(
                onClick  = { viewModel.setCompleted(!done) },
                shape    = CircleShape,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (done) GIDSurfaceHigh else GIDCyan,
                    contentColor   = if (done) GIDText else OnCard,
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 4.dp),
            ) {
                Text(
                    stringResource(
                        when {
                            done             -> R.string.exam_mark_not_done
                            kind.isSubmitted -> R.string.exam_mark_submitted
                            else             -> R.string.exam_mark_done
                        }
                    )
                )
            }
        }

        item { SectionTitle(stringResource(R.string.exam_saved_items), Modifier.padding(top = 16.dp)) }
        if (saved.isEmpty()) item { EmptyText(stringResource(R.string.exam_saved_empty)) }
        items(saved, key = { it.id }) { capture ->
            CaptureRow(capture, subjectById, selected = false, onClick = { onOpenCapture(capture.id) })
        }
    }

    if (editorOpen) ExamEditorSheet(viewModel, initial = current, onDismiss = { editorOpen = false })
    if (deleteOpen) {
        ConfirmDialog(
            title     = stringResource(R.string.exam_delete_title),
            body      = stringResource(R.string.exam_delete_body),
            confirm   = stringResource(R.string.action_delete),
            onDismiss = { deleteOpen = false },
            onConfirm = { viewModel.delete(); deleteOpen = false; onBack() },
        )
    }
}
