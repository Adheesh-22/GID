package com.adheesh.gid.feature.library.ui

import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.Grade
import com.adheesh.gid.feature.semesters.ui.SemesterChip
import com.adheesh.gid.core.designsystem.LocalBackdropColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.database.SubjectEntity
import com.adheesh.gid.core.designsystem.Dot
import com.adheesh.gid.core.designsystem.EmptyText
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.GIDTextField
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.designsystem.ScreenTitle
import com.adheesh.gid.core.designsystem.SectionTitle
import com.adheesh.gid.feature.capture.data.CaptureActions
import com.adheesh.gid.feature.library.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch

// ── Library tab (SPEC §2.8) ──────────────────────────────────────────────────
// Search on top; below it either the results, or the Inbox and the subjects.
// Tapping an Inbox card sorts it; long-press starts selecting several.
@Composable
fun LibraryScreen(
    viewModel      : LibraryViewModel,
    captureActions : CaptureActions,
    onOpenSubject  : (String) -> Unit,
    onOpenCapture  : (String) -> Unit,
    onOpenExams    : () -> Unit,
    onOpenAssessments : () -> Unit,
    onOpenMarks    : () -> Unit,
    modifier       : Modifier = Modifier,
    requestedSort  : List<String> = emptyList(),
    onRequestUsed  : () -> Unit = {},
) {
    val inbox       by viewModel.inbox.collectAsState()
    val subjectList by viewModel.subjectList.collectAsState()
    val subjectById by viewModel.subjectById.collectAsState()
    val query       by viewModel.query.collectAsState()
    val onWallpaper = LocalBackdropColors.current
    val results     by viewModel.results.collectAsState()
    val selection   by viewModel.selection.collectAsState()
    val archived    by viewModel.archivedSubjects.collectAsState()
    val scope = rememberCoroutineScope()

    // Ids, not entities, so the sheet survives rotation and sees fresh rows.
    var sortingIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var deletingIds       by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var newSubjectOpen    by rememberSaveable { mutableStateOf(false) }
    var deletingSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var showArchived      by rememberSaveable { mutableStateOf(false) }
    // Opened from the share dialog's 'Sort now': go straight to sorting those.
    LaunchedEffect(requestedSort) {
        if (requestedSort.isNotEmpty()) sortingIds = requestedSort
        onRequestUsed()
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier            = Modifier.fillMaxSize(),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScreenTitle(stringResource(R.string.library_title), Modifier.weight(1f), color = onWallpaper.text)
                    SemesterChip()
                }
            }
            // Exams, Assessments and Marks each have their own page (SPEC §2.15, §2.16).
            item {
                Row {
                    TextButton(onClick = onOpenExams) {
                        Icon(Icons.Rounded.Event, contentDescription = null, tint = onWallpaper.accent)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.exams_title), color = onWallpaper.text)
                    }
                    TextButton(onClick = onOpenAssessments) {
                        Icon(Icons.AutoMirrored.Rounded.Assignment, contentDescription = null, tint = onWallpaper.accent)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.assessments_short), color = onWallpaper.text)
                    }
                    TextButton(onClick = onOpenMarks) {
                        Icon(Icons.Rounded.Grade, contentDescription = null, tint = onWallpaper.accent)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.marks_title), color = onWallpaper.text)
                    }
                }
            }
            item {
                GIDTextField(
                    value         = query,
                    onValueChange = viewModel::setQuery,
                    label         = stringResource(R.string.library_search),
                    backdrop      = onWallpaper,
                    leadingIcon   = { Icon(Icons.Rounded.Search, contentDescription = null, tint = onWallpaper.muted) },
                    trailingIcon  = if (query.isNotEmpty()) {
                        { IconButton(onClick = { viewModel.setQuery("") }) { Icon(Icons.Rounded.Close, stringResource(R.string.action_clear), tint = onWallpaper.muted) } }
                    } else null,
                )
            }

            val found = results
            if (found != null) {
                // ── Search results ───────────────────────────────────────────
                if (found.isEmpty()) item { EmptyText(stringResource(R.string.library_no_results, query.trim()), color = onWallpaper.muted) }
                items(found, key = { it.id }) { capture ->
                    CaptureRow(capture, subjectById, selected = false, onClick = { onOpenCapture(capture.id) })
                }
            } else {
                // ── Inbox ────────────────────────────────────────────────────
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        SectionTitle(stringResource(R.string.library_inbox), color = onWallpaper.text)
                        Spacer(Modifier.width(10.dp))
                        if (inbox.isNotEmpty()) {
                            Text(stringResource(R.string.library_inbox_count, inbox.size), color = onWallpaper.muted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (inbox.isEmpty()) item { EmptyText(stringResource(R.string.library_inbox_empty), color = onWallpaper.muted) }
                items(inbox, key = { it.id }) { capture ->
                    val selecting = selection.isNotEmpty()
                    CaptureRow(
                        capture     = capture,
                        subjectById = subjectById,
                        selected    = capture.id in selection,
                        onClick     = { if (selecting) viewModel.toggleSelected(capture.id) else sortingIds = listOf(capture.id) },
                        onLongClick = { viewModel.toggleSelected(capture.id) },
                        onDelete    = if (selecting) null else { { deletingIds = listOf(capture.id) } },
                    )
                }

                // ── Subjects ─────────────────────────────────────────────────
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                        SectionTitle(stringResource(R.string.library_subjects), Modifier.weight(1f), color = onWallpaper.text)
                        TextButton(onClick = { newSubjectOpen = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = onWallpaper.accent)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.sort_new_subject), color = onWallpaper.accent)
                        }
                    }
                }
                if (subjectList.isEmpty()) item { EmptyText(stringResource(R.string.library_subjects_empty), color = onWallpaper.muted) }
                items(subjectList, key = { it.subject.id }) { row ->
                    val subjectId = row.subject.id
                    SubjectRow(
                        subject  = row.subject,
                        trailing = pluralStringResource(R.plurals.library_item_count, row.itemCount, row.itemCount),
                        onClick  = { onOpenSubject(subjectId) },
                        menu     = listOf(
                            stringResource(R.string.subject_archive) to { viewModel.setArchived(subjectId, true) },
                            stringResource(R.string.subject_delete) to { deletingSubjectId = subjectId },
                        ),
                    )
                }

                // ── Archived subjects: hidden until asked for ────────────────
                if (archived.isNotEmpty()) {
                    item {
                        TextButton(onClick = { showArchived = !showArchived }) {
                            Text(stringResource(R.string.library_archived, archived.size), color = onWallpaper.muted)
                        }
                    }
                    if (showArchived) {
                        items(archived, key = { "a-${it.id}" }) { subject ->
                            SubjectRow(
                                subject  = subject,
                                trailing = null,
                                onClick  = { onOpenSubject(subject.id) },
                                menu     = listOf(
                                    stringResource(R.string.subject_restore) to { viewModel.setArchived(subject.id, false) },
                                    stringResource(R.string.subject_delete) to { deletingSubjectId = subject.id },
                                ),
                            )
                        }
                    }
                }
            }
        }

        // ── Bottom-right: batch sort while selecting, else the capture button ──
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            if (selection.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = viewModel::clearSelection) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.cd_clear_selection), tint = GIDText)
                    }
                    TextButton(onClick = { deletingIds = selection.toList() }) {
                        Text(stringResource(R.string.library_delete_selected, selection.size), color = GIDCoral)
                    }
                    Button(
                        onClick = { sortingIds = selection.toList() },
                        shape   = CircleShape,
                        colors  = ButtonDefaults.buttonColors(containerColor = GIDCyan, contentColor = OnCard),
                    ) { Text(stringResource(R.string.library_sort_selected, selection.size)) }
                }
            } else {
                CaptureButton(captureActions, onSaved = {})
            }
        }
    }

    if (deletingIds.isNotEmpty()) {
        ConfirmDialog(
            title     = pluralStringResource(R.plurals.capture_delete_many_title, deletingIds.size, deletingIds.size),
            body      = stringResource(R.string.capture_delete_many_body),
            confirm   = stringResource(R.string.action_delete),
            onDismiss = { deletingIds = emptyList() },
            onConfirm = { viewModel.deleteCaptures(deletingIds); deletingIds = emptyList() },
        )
    }
    if (newSubjectOpen) {
        NameDialog(stringResource(R.string.sort_new_subject), onDismiss = { newSubjectOpen = false }) { name ->
            scope.launch { viewModel.createSubject(name) }
        }
    }
    (subjectList.map { it.subject } + archived).firstOrNull { it.id == deletingSubjectId }?.let { subject ->
        ConfirmDialog(
            title     = stringResource(R.string.subject_delete),
            body      = stringResource(R.string.subject_delete_body, subject.name),
            confirm   = stringResource(R.string.action_delete),
            onDismiss = { deletingSubjectId = null },
            onConfirm = { viewModel.deleteSubject(subject.id); deletingSubjectId = null },
        )
    }

    if (sortingIds.isNotEmpty()) {
        val sorting = inbox.filter { it.id in sortingIds }
        SortSheet(
            viewModel = viewModel,
            captures  = sorting,
            onDismiss = { sortingIds = emptyList() },
            onSorted  = { viewModel.clearSelection() },
        )
        // Everything got sorted elsewhere (or deleted): nothing left to show.
        LaunchedEffect(sorting.isEmpty()) { if (sorting.isEmpty()) sortingIds = emptyList() }
    }
}

/** A subject in the list: color dot, name, an optional count and a menu (last item shown in red). */
@Composable
private fun SubjectRow(
    subject  : SubjectEntity,
    trailing : String?,
    onClick  : () -> Unit,
    menu     : List<Pair<String, () -> Unit>>,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(GIDSurface)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
    ) {
        Dot(subject.color(), size = 12.dp)
        Spacer(Modifier.width(12.dp))
        Text(subject.name, style = MaterialTheme.typography.bodyLarge, color = GIDText, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
        Box {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.cd_more), tint = GIDMutedText) }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = GIDSurface) {
                menu.forEachIndexed { i, (label, action) ->
                    DropdownMenuItem(
                        text    = { Text(label, color = if (i == menu.lastIndex) GIDCoral else GIDText) },
                        onClick = { menuOpen = false; action() },
                    )
                }
            }
        }
    }
}
