package com.adheesh.gid.feature.library.ui

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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.designsystem.Dot
import com.adheesh.gid.core.designsystem.EmptyText
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

// ── Library tab (SPEC §2.8) ──────────────────────────────────────────────────
// Search on top; below it either the results, or the Inbox and the subjects.
// Tapping an Inbox card sorts it; long-press starts selecting several.
@Composable
fun LibraryScreen(
    viewModel      : LibraryViewModel,
    captureActions : CaptureActions,
    onOpenSubject  : (String) -> Unit,
    onOpenCapture  : (String) -> Unit,
    modifier       : Modifier = Modifier,
    requestedSort  : List<String> = emptyList(),
    onRequestUsed  : () -> Unit = {},
) {
    val inbox       by viewModel.inbox.collectAsState()
    val subjectList by viewModel.subjectList.collectAsState()
    val subjectById by viewModel.subjectById.collectAsState()
    val query       by viewModel.query.collectAsState()
    val results     by viewModel.results.collectAsState()
    val selection   by viewModel.selection.collectAsState()

    // Ids, not entities, so the sheet survives rotation and sees fresh rows.
    var sortingIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
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
            item { ScreenTitle(stringResource(R.string.library_title)) }
            item {
                GIDTextField(
                    value         = query,
                    onValueChange = viewModel::setQuery,
                    label         = stringResource(R.string.library_search),
                    leadingIcon   = { Icon(Icons.Rounded.Search, contentDescription = null, tint = GIDMutedText) },
                    trailingIcon  = if (query.isNotEmpty()) {
                        { IconButton(onClick = { viewModel.setQuery("") }) { Icon(Icons.Rounded.Close, stringResource(R.string.action_clear), tint = GIDMutedText) } }
                    } else null,
                )
            }

            val found = results
            if (found != null) {
                // ── Search results ───────────────────────────────────────────
                if (found.isEmpty()) item { EmptyText(stringResource(R.string.library_no_results, query.trim())) }
                items(found, key = { it.id }) { capture ->
                    CaptureRow(capture, subjectById, selected = false, onClick = { onOpenCapture(capture.id) })
                }
            } else {
                // ── Inbox ────────────────────────────────────────────────────
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        SectionTitle(stringResource(R.string.library_inbox))
                        Spacer(Modifier.width(10.dp))
                        if (inbox.isNotEmpty()) {
                            Text(stringResource(R.string.library_inbox_count, inbox.size), color = GIDMutedText, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (inbox.isEmpty()) item { EmptyText(stringResource(R.string.library_inbox_empty)) }
                items(inbox, key = { it.id }) { capture ->
                    val selecting = selection.isNotEmpty()
                    CaptureRow(
                        capture     = capture,
                        subjectById = subjectById,
                        selected    = capture.id in selection,
                        onClick     = { if (selecting) viewModel.toggleSelected(capture.id) else sortingIds = listOf(capture.id) },
                        onLongClick = { viewModel.toggleSelected(capture.id) },
                    )
                }

                // ── Subjects ─────────────────────────────────────────────────
                item { SectionTitle(stringResource(R.string.library_subjects), Modifier.padding(top = 16.dp)) }
                if (subjectList.isEmpty()) item { EmptyText(stringResource(R.string.library_subjects_empty)) }
                items(subjectList, key = { it.subject.id }) { row ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(GIDSurface)
                            .clickable { onOpenSubject(row.subject.id) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Dot(row.subject.color(), size = 12.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(row.subject.name, style = MaterialTheme.typography.bodyLarge, color = GIDText, modifier = Modifier.weight(1f))
                        Text(pluralStringResource(R.plurals.library_item_count, row.itemCount, row.itemCount), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
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
