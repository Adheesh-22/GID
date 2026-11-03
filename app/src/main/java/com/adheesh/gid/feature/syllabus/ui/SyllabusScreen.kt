package com.adheesh.gid.feature.syllabus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.database.SyllabusTopicEntity
import com.adheesh.gid.core.database.SyllabusUnitEntity
import com.adheesh.gid.core.designsystem.EmptyText
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.GIDTextField
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.designsystem.ScreenTitle
import com.adheesh.gid.feature.library.ui.NameDialog
import com.adheesh.gid.feature.syllabus.domain.SyllabusParser
import com.adheesh.gid.feature.syllabus.viewmodel.SyllabusViewModel

/** Which name dialog is open: adding or renaming a unit or topic. */
private sealed interface NameRequest {
    data object NewUnit : NameRequest
    data class NewTopic(val unit: SyllabusUnitEntity) : NameRequest
    data class RenameUnit(val unit: SyllabusUnitEntity) : NameRequest
    data class RenameTopic(val topic: SyllabusTopicEntity) : NameRequest
}

// ── Syllabus page (SPEC §2.13) ───────────────────────────────────────────────
// Units as sections with their topics to tick; '+ Topic' under each unit,
// '+ Unit' and 'Paste list' at the bottom.
@Composable
fun SyllabusScreen(
    viewModel : SyllabusViewModel,
    onBack    : () -> Unit,
    modifier  : Modifier = Modifier,
) {
    val subject  by viewModel.subject.collectAsState()
    val units    by viewModel.units.collectAsState()
    val progress by viewModel.progress.collectAsState()

    var request   by remember { mutableStateOf<NameRequest?>(null) }
    var pasteOpen by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier            = modifier.fillMaxSize(),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back), tint = GIDText)
                }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    ScreenTitle(stringResource(R.string.syllabus_title))
                    subject?.let { Text(it.name, style = MaterialTheme.typography.bodyMedium, color = GIDMutedText, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
        item {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(progress.text(), style = MaterialTheme.typography.bodyMedium, color = GIDText, modifier = Modifier.padding(bottom = 6.dp))
                if (progress.total > 0) SyllabusBar(progress, progress.level().color())
            }
        }
        if (units.isEmpty()) item { EmptyText(stringResource(R.string.syllabus_empty)) }

        units.forEachIndexed { index, group ->
            item(key = "u-${group.unit.id}") {
                UnitHeader(
                    name      = group.unit.name,
                    percent   = group.progress.percent,
                    canMoveUp = index > 0,
                    canMoveDown = index < units.lastIndex,
                    onRename  = { request = NameRequest.RenameUnit(group.unit) },
                    onMove    = { up -> viewModel.moveUnit(group.unit, up) },
                    onDelete  = { viewModel.deleteUnit(group.unit) },
                )
            }
            items(group.topics, key = { "t-${it.id}" }) { topic ->
                val i = group.topics.indexOf(topic)
                TopicRow(topic, onDone = { viewModel.setDone(topic, it) }) {
                    ItemMenu(
                        canMoveUp   = i > 0,
                        canMoveDown = i < group.topics.lastIndex,
                        onRename    = { request = NameRequest.RenameTopic(topic) },
                        onMove      = { up -> viewModel.moveTopic(topic, up) },
                        onDelete    = { viewModel.deleteTopic(topic) },
                    )
                }
            }
            item(key = "add-${group.unit.id}") {
                TextButton(onClick = { request = NameRequest.NewTopic(group.unit) }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = GIDCyan)
                    Text(stringResource(R.string.syllabus_add_topic), color = GIDCyan)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                OutlinedButton(onClick = { request = NameRequest.NewUnit }, shape = CircleShape, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.syllabus_add_unit), color = GIDCyan)
                }
                OutlinedButton(onClick = { pasteOpen = true }, shape = CircleShape, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.syllabus_paste), color = GIDCyan)
                }
            }
        }
    }

    when (val r = request) {
        null -> Unit
        NameRequest.NewUnit -> NameDialog(stringResource(R.string.syllabus_add_unit), onDismiss = { request = null }) { viewModel.addUnit(it) }
        is NameRequest.NewTopic -> NameDialog(stringResource(R.string.syllabus_add_topic), onDismiss = { request = null }) { viewModel.addTopic(r.unit, it) }
        is NameRequest.RenameUnit -> NameDialog(stringResource(R.string.syllabus_rename), initial = r.unit.name, onDismiss = { request = null }) {
            viewModel.renameUnit(r.unit, it)
        }
        is NameRequest.RenameTopic -> NameDialog(stringResource(R.string.syllabus_rename), initial = r.topic.name, onDismiss = { request = null }) {
            viewModel.renameTopic(r.topic, it)
        }
    }
    if (pasteOpen) PasteSheet(onDismiss = { pasteOpen = false }, onSave = viewModel::importText)
}

@Composable
private fun UnitHeader(
    name        : String,
    percent     : Int?,
    canMoveUp   : Boolean,
    canMoveDown : Boolean,
    onRename    : () -> Unit,
    onMove      : (up: Boolean) -> Unit,
    onDelete    : () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
        Text(name, style = MaterialTheme.typography.titleLarge, color = GIDText, modifier = Modifier.weight(1f))
        percent?.let { Text(stringResource(R.string.syllabus_percent, it), style = MaterialTheme.typography.labelLarge, color = GIDMutedText) }
        ItemMenu(canMoveUp, canMoveDown, onRename, onMove, onDelete)
    }
}

@Composable
private fun ItemMenu(
    canMoveUp   : Boolean,
    canMoveDown : Boolean,
    onRename    : () -> Unit,
    onMove      : (up: Boolean) -> Unit,
    onDelete    : () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.cd_more), tint = GIDMutedText) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = GIDSurface) {
            DropdownMenuItem(text = { Text(stringResource(R.string.syllabus_rename), color = GIDText) }, onClick = { open = false; onRename() })
            if (canMoveUp) DropdownMenuItem(text = { Text(stringResource(R.string.syllabus_move_up), color = GIDText) }, onClick = { open = false; onMove(true) })
            if (canMoveDown) DropdownMenuItem(text = { Text(stringResource(R.string.syllabus_move_down), color = GIDText) }, onClick = { open = false; onMove(false) })
            DropdownMenuItem(text = { Text(stringResource(R.string.syllabus_delete), color = GIDCoral) }, onClick = { open = false; onDelete() })
        }
    }
}

/** Paste a whole syllabus; the preview shows how it will be split before saving. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasteSheet(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val parsed = remember(text) { SyllabusParser.parse(text) }
    val topicCount = parsed.sumOf { it.topics.size }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = GIDSurface,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
        ) {
            Text(stringResource(R.string.syllabus_paste), style = MaterialTheme.typography.titleLarge, color = GIDText)
            Text(stringResource(R.string.syllabus_paste_hint), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
            GIDTextField(
                value         = text,
                onValueChange = { text = it },
                label         = stringResource(R.string.syllabus_paste_field),
                singleLine    = false,
                modifier      = Modifier.heightIn(min = 160.dp),
            )
            if (parsed.isNotEmpty()) {
                Text(
                    pluralStringResource(R.plurals.syllabus_preview_units, parsed.size, parsed.size) + " · " +
                        pluralStringResource(R.plurals.syllabus_preview_topics, topicCount, topicCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = GIDText,
                )
                parsed.forEach { unit ->
                    Text("${unit.name} (${unit.topics.size})", style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
                }
            }
            Button(
                enabled  = topicCount > 0,
                onClick  = { onSave(text); onDismiss() },
                shape    = CircleShape,
                colors   = ButtonDefaults.buttonColors(containerColor = GIDCyan, contentColor = OnCard),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.action_save)) }
        }
    }
}
