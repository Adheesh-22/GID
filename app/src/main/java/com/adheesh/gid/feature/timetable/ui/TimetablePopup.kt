package com.adheesh.gid.feature.timetable.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.database.TimetableEntryEntity
import com.adheesh.gid.core.designsystem.EmptyText
import com.adheesh.gid.core.designsystem.Error
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDDialog
import com.adheesh.gid.core.designsystem.GIDMustard
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.GIDTextField
import com.adheesh.gid.core.designsystem.GIDTimePickerDialog
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.util.formatMinutesOfDay
import com.adheesh.gid.feature.tasks.ui.PickerField
import com.adheesh.gid.feature.timetable.viewmodel.ClassDraft
import com.adheesh.gid.feature.timetable.viewmodel.TimetableViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

// ── TimetablePopup (SPEC §2.7) ───────────────────────────────────────────────
// The weekly timetable grouped by day; 'Change timetable' turns on edit mode.
@Composable
fun TimetablePopup(viewModel: TimetableViewModel, onDismiss: () -> Unit) {
    val byDay    by viewModel.byDay.collectAsState()
    val editMode by viewModel.editMode.collectAsState()
    val defaultReminder by viewModel.defaultReminder.collectAsState()

    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId  by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    val all = byDay.values.flatten()

    GIDDialog(
        title     = stringResource(R.string.timetable_title),
        onDismiss = onDismiss,
        actions   = {
            TextButton(onClick = viewModel::toggleEditMode) {
                Text(stringResource(if (editMode) R.string.action_done else R.string.timetable_change), color = GIDCyan)
            }
        },
    ) {
        Column(
            Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (byDay.isEmpty()) EmptyText(stringResource(R.string.timetable_empty))
            byDay.forEach { (day, entries) ->
                Text(dayName(day), style = MaterialTheme.typography.titleMedium, color = GIDMustard)
                entries.forEach { entry ->
                    ClassRow(
                        entry     = entry,
                        editMode  = editMode,
                        onToggle  = { viewModel.setEnabled(entry, it) },
                        onEdit    = { editingId = entry.id; editorOpen = true },
                        onDelete  = { deletingId = entry.id },
                    )
                }
            }
            if (editMode) {
                TextButton(onClick = { editingId = null; editorOpen = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = GIDCyan)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.timetable_add_class), color = GIDCyan)
                }
            }
        }
    }

    if (editorOpen) {
        ClassEditorDialog(
            initial         = all.firstOrNull { it.id == editingId },
            defaultReminder = defaultReminder,
            onSave          = { existing, draft -> viewModel.save(existing, draft); editorOpen = false },
            onDismiss       = { editorOpen = false },
        )
    }
    deletingId?.let { id ->
        val entry = all.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = { deletingId = null },
            containerColor   = GIDSurface,
            shape            = MaterialTheme.shapes.medium,
            title            = { Text(stringResource(R.string.timetable_delete_title), color = GIDText) },
            text             = { Text(entry?.subject.orEmpty(), color = GIDMutedText) },
            confirmButton    = {
                TextButton(onClick = { entry?.let(viewModel::delete); deletingId = null }) {
                    Text(stringResource(R.string.action_delete), color = Error)
                }
            },
            dismissButton    = {
                TextButton(onClick = { deletingId = null }) { Text(stringResource(R.string.action_cancel), color = GIDMutedText) }
            },
        )
    }
}

@Composable
private fun ClassRow(
    entry    : TimetableEntryEntity,
    editMode : Boolean,
    onToggle : (Boolean) -> Unit,
    onEdit   : () -> Unit,
    onDelete : () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(entry.subject, style = MaterialTheme.typography.bodyMedium, color = if (entry.enabled) GIDText else GIDMutedText)
            val time = "${formatMinutesOfDay(entry.startMinutes)} – ${formatMinutesOfDay(entry.endMinutes)}"
            Text(
                if (entry.room.isNullOrBlank()) time else stringResource(R.string.timetable_time_room, time, entry.room),
                style = MaterialTheme.typography.bodySmall,
                color = GIDMutedText,
            )
            if (editMode) {
                Text(stringResource(R.string.timetable_reminder_before, entry.reminderMinutes), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
            }
        }
        if (editMode) {
            Switch(
                checked = entry.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = GIDMustard, checkedThumbColor = OnCard),
            )
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, stringResource(R.string.cd_edit_class), tint = GIDText) }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, stringResource(R.string.cd_delete_class), tint = Error) }
        }
    }
}

@Composable
private fun ClassEditorDialog(
    initial         : TimetableEntryEntity?,
    defaultReminder : Int,
    onSave          : (TimetableEntryEntity?, ClassDraft) -> Unit,
    onDismiss       : () -> Unit,
) {
    var subject  by rememberSaveable { mutableStateOf(initial?.subject.orEmpty()) }
    var day      by rememberSaveable { mutableStateOf(initial?.dayOfWeek ?: 1) }
    var start    by rememberSaveable { mutableStateOf(initial?.startMinutes ?: (9 * 60)) }
    var end      by rememberSaveable { mutableStateOf(initial?.endMinutes ?: (10 * 60)) }
    var room     by rememberSaveable { mutableStateOf(initial?.room.orEmpty()) }
    var reminder by rememberSaveable { mutableStateOf((initial?.reminderMinutes ?: defaultReminder).toString()) }
    var enabled  by rememberSaveable { mutableStateOf(initial?.enabled ?: true) }
    var error    by rememberSaveable { mutableStateOf<Int?>(null) }
    var picking  by rememberSaveable { mutableStateOf<String?>(null) }   // "start" | "end"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = GIDSurface,
        shape            = MaterialTheme.shapes.medium,
        title = { Text(stringResource(if (initial == null) R.string.timetable_add_class else R.string.timetable_edit_class), color = GIDText) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GIDTextField(
                    value           = subject,
                    onValueChange   = { subject = it; error = null },
                    label           = stringResource(R.string.field_subject),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..7).forEach { d ->
                        FilterChip(
                            selected = d == day,
                            onClick  = { day = d },
                            label    = { Text(shortDayName(d), style = MaterialTheme.typography.labelMedium) },
                            colors   = FilterChipDefaults.filterChipColors(selectedContainerColor = GIDMustard, selectedLabelColor = OnCard, labelColor = GIDText),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickerField(stringResource(R.string.field_start), formatMinutesOfDay(start), Modifier.weight(1f)) { picking = "start" }
                    PickerField(stringResource(R.string.field_end), formatMinutesOfDay(end), Modifier.weight(1f)) { picking = "end" }
                }
                GIDTextField(value = room, onValueChange = { room = it }, label = stringResource(R.string.field_room))
                GIDTextField(
                    value           = reminder,
                    onValueChange   = { v -> reminder = v.filter(Char::isDigit).take(3) },
                    label           = stringResource(R.string.field_reminder_minutes),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.field_enabled), color = GIDText, modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { enabled = it }, colors = SwitchDefaults.colors(checkedTrackColor = GIDMustard, checkedThumbColor = OnCard))
                }
                error?.let { Text(stringResource(it), color = Error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    subject.isBlank() -> R.string.timetable_error_subject
                    end <= start      -> R.string.timetable_error_time
                    else              -> null
                }
                if (error == null) {
                    onSave(initial, ClassDraft(subject, day, start, end, room, reminder.toIntOrNull() ?: defaultReminder, enabled))
                }
            }) { Text(stringResource(R.string.action_save), color = GIDCyan) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = GIDMutedText) } },
    )

    picking?.let { which ->
        GIDTimePickerDialog(
            initialMinutes = if (which == "start") start else end,
            onDismiss      = { picking = null },
            onPicked       = { if (which == "start") start = it else end = it },
        )
    }
}

private fun dayName(isoDay: Int): String = DayOfWeek.of(isoDay).getDisplayName(TextStyle.FULL, Locale.getDefault())
private fun shortDayName(isoDay: Int): String = DayOfWeek.of(isoDay).getDisplayName(TextStyle.SHORT, Locale.getDefault())
