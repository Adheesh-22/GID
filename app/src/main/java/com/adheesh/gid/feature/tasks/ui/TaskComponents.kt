package com.adheesh.gid.feature.tasks.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.database.Priority
import com.adheesh.gid.core.database.TaskEntity
import com.adheesh.gid.core.designsystem.Dot
import com.adheesh.gid.core.designsystem.Error
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDDatePickerDialog
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDSurfaceHigh
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.GIDTextField
import com.adheesh.gid.core.designsystem.GIDTimePickerDialog
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.designsystem.color
import com.adheesh.gid.core.designsystem.label
import com.adheesh.gid.core.util.formatMedium
import com.adheesh.gid.core.util.formatMinutesOfDay
import com.adheesh.gid.feature.tasks.data.TaskDraft
import java.time.LocalDate
import kotlin.math.roundToInt

/** One task line: checkbox, title, priority dot, percent done. */
@Composable
fun TaskRow(
    task        : TaskEntity,
    onToggle    : () -> Unit,
    onClick     : () -> Unit,
    modifier    : Modifier = Modifier,
    selected    : Boolean = false,
    contentColor: Color = GIDText,
) {
    val done = task.progressPercent >= 100
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
    ) {
        // A smaller touch box leaves room for the title in narrow cards (still 36dp tall).
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 36.dp) {
            Checkbox(
                checked         = done,
                onCheckedChange = { onToggle() },
                colors          = CheckboxDefaults.colors(
                    checkedColor   = contentColor,
                    uncheckedColor = contentColor,
                    checkmarkColor = if (contentColor == OnCard) GIDCoral else OnCard,
                ),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text           = task.title,
            style          = MaterialTheme.typography.bodyMedium.copy(
                fontWeight     = if (selected) FontWeight.Bold else FontWeight.Normal,
                textDecoration = if (done) TextDecoration.LineThrough else null,
            ),
            color          = contentColor,
            maxLines       = 2,
            overflow       = TextOverflow.Ellipsis,
            modifier       = Modifier.weight(1f),
        )
        Spacer(Modifier.width(6.dp))
        // On the coral Tasks card a coral dot would vanish, so ring it in near-black.
        Dot(
            color    = Priority.of(task.priority).color(),
            size     = 10.dp,
            modifier = if (contentColor == OnCard) Modifier.border(1.5.dp, OnCard, CircleShape) else Modifier,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(R.string.percent_value, task.progressPercent),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
        )
    }
}

/**
 * Selected-task detail: title, description, time, estimate, percent slider,
 * and the edit / delete / 'Move to tomorrow' actions. Reused on Home.
 */
@Composable
fun TaskDetailPanel(
    task             : TaskEntity?,
    onProgress       : (Int) -> Unit,
    onEdit           : () -> Unit,
    onDelete         : () -> Unit,
    onMoveTomorrow   : () -> Unit,
    modifier         : Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (task == null) {
            Text(stringResource(R.string.task_select_hint), style = MaterialTheme.typography.bodyMedium, color = GIDMutedText)
            return@Column
        }
        Text(task.title, style = MaterialTheme.typography.titleMedium, color = GIDText)
        if (task.description.isNotBlank()) {
            Text(task.description, style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
        }
        Text(
            text  = task.timeMinutes?.let { formatMinutesOfDay(it) } ?: stringResource(R.string.task_no_time),
            style = MaterialTheme.typography.bodySmall,
            color = GIDMutedText,
        )
        task.estimatedMinutes?.let {
            Text(stringResource(R.string.task_estimate, it), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
        }

        // The slider moves freely and saves when the finger lifts, so we write
        // once per drag instead of on every pixel.
        var sliderValue by remember(task.id, task.progressPercent) { mutableFloatStateOf(task.progressPercent.toFloat()) }
        Text(
            stringResource(R.string.task_percent_done, sliderValue.roundToInt()),
            style = MaterialTheme.typography.labelMedium,
            color = GIDText,
        )
        Slider(
            value                 = sliderValue,
            onValueChange         = { sliderValue = it },
            onValueChangeFinished = { onProgress(sliderValue.roundToInt()) },
            valueRange            = 0f..100f,
            steps                 = 19,     // 5% steps
            colors                = SliderDefaults.colors(thumbColor = GIDCoral, activeTrackColor = GIDCoral, inactiveTrackColor = GIDSurfaceHigh),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, stringResource(R.string.cd_edit_task), tint = GIDText) }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, stringResource(R.string.cd_delete_task), tint = Error) }
        }
        // On its own line so it never gets squeezed in the narrow Calendar column.
        Row {
            TextButton(onClick = onMoveTomorrow) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = GIDCyan, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.task_move_tomorrow), color = GIDCyan, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** Low / Medium / High chips (editable). */
@Composable
fun PriorityEditor(
    selected : Priority?,
    onChange : (Priority) -> Unit,
    modifier : Modifier = Modifier,
    vertical : Boolean = false,
) {
    // High first when stacked (most important on top), Low first in a row.
    val order = if (vertical) Priority.entries.reversed() else Priority.entries
    val chips: @Composable () -> Unit = {
        order.forEach { p ->
            FilterChip(
                selected = p == selected,
                onClick  = { onChange(p) },
                label    = { Text(p.label(), style = MaterialTheme.typography.labelMedium) },
                leadingIcon = { Dot(p.color(), size = 8.dp) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = p.color(),
                    selectedLabelColor     = OnCard,
                    labelColor             = GIDText,
                ),
            )
        }
    }
    if (vertical) Column(modifier) { chips() }
    else Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) { chips() }
}

/** Confirm before deleting (Phase 4 step 5). */
@Composable
fun DeleteTaskDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = GIDSurface,
        shape            = MaterialTheme.shapes.medium,
        title            = { Text(stringResource(R.string.task_delete_title), color = GIDText) },
        text             = { Text(stringResource(R.string.task_delete_text, title), color = GIDMutedText) },
        confirmButton    = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete), color = Error) } },
        dismissButton    = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = GIDMutedText) } },
    )
}

/** Add or edit a task: title, description, date, optional time, priority, estimate. */
@Composable
fun TaskEditorDialog(
    initial     : TaskEntity?,
    defaultDate : LocalDate,
    onSave      : (TaskDraft) -> Unit,
    onDismiss   : () -> Unit,
) {
    var title       by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(initial?.description.orEmpty()) }
    var dateDay     by rememberSaveable { mutableStateOf(initial?.dateEpochDay ?: defaultDate.toEpochDay()) }
    var time        by rememberSaveable { mutableStateOf(initial?.timeMinutes) }
    var priority    by rememberSaveable { mutableStateOf(initial?.let { Priority.of(it.priority) } ?: Priority.MEDIUM) }
    var estimate    by rememberSaveable { mutableStateOf(initial?.estimatedMinutes?.toString().orEmpty()) }
    var titleError  by rememberSaveable { mutableStateOf(false) }
    var pickDate    by rememberSaveable { mutableStateOf(false) }
    var pickTime    by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = GIDSurface,
        shape            = MaterialTheme.shapes.medium,
        title = {
            Text(stringResource(if (initial == null) R.string.task_add_title else R.string.task_edit_title), color = GIDText)
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GIDTextField(
                    value           = title,
                    onValueChange   = { title = it; titleError = false },
                    label           = stringResource(R.string.field_task_title),
                    error           = if (titleError) stringResource(R.string.task_error_title) else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                )
                GIDTextField(
                    value           = description,
                    onValueChange   = { description = it },
                    label           = stringResource(R.string.field_description),
                    singleLine      = false,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                PickerField(stringResource(R.string.field_date), LocalDate.ofEpochDay(dateDay).formatMedium()) { pickDate = true }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PickerField(
                        label    = stringResource(R.string.field_time_optional),
                        value    = time?.let { formatMinutesOfDay(it) } ?: stringResource(R.string.task_no_time),
                        modifier = Modifier.weight(1f),
                    ) { pickTime = true }
                    if (time != null) {
                        TextButton(onClick = { time = null }) { Text(stringResource(R.string.action_clear), color = GIDMutedText) }
                    }
                }
                Text(stringResource(R.string.field_priority), style = MaterialTheme.typography.labelMedium, color = GIDMutedText)
                PriorityEditor(selected = priority, onChange = { priority = it })
                GIDTextField(
                    value           = estimate,
                    onValueChange   = { v -> estimate = v.filter(Char::isDigit).take(4) },
                    label           = stringResource(R.string.field_estimate),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) { titleError = true; return@TextButton }
                onSave(TaskDraft(title, description, dateDay, time, priority, estimate.toIntOrNull()))
            }) { Text(stringResource(R.string.action_save), color = GIDCyan) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = GIDMutedText) }
        },
    )

    if (pickDate) {
        GIDDatePickerDialog(
            initial   = LocalDate.ofEpochDay(dateDay),
            onDismiss = { pickDate = false },
            onPicked  = { dateDay = it.toEpochDay() },
        )
    }
    if (pickTime) {
        GIDTimePickerDialog(
            initialMinutes = time ?: (9 * 60),
            onDismiss      = { pickTime = false },
            onPicked       = { time = it },
        )
    }
}

/** A read-only field that opens a picker when tapped. */
@Composable
fun PickerField(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.fillMaxWidth()) {
        GIDTextField(value = value, onValueChange = {}, label = label, readOnly = true)
        Box(Modifier.matchParentSize().clickable(onClick = onClick))
    }
}
