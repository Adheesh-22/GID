package com.adheesh.gid.feature.calendar.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.database.Priority
import com.adheesh.gid.core.database.TaskEntity
import com.adheesh.gid.core.designsystem.Dot
import com.adheesh.gid.core.designsystem.EmptyText
import com.adheesh.gid.core.designsystem.GIDCard
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDHolidayGreen
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.GIDTextField
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.designsystem.ScreenTitle
import com.adheesh.gid.core.util.formatMedium
import com.adheesh.gid.core.util.formatShort
import com.adheesh.gid.core.util.today
import com.adheesh.gid.feature.calendar.viewmodel.CalendarViewModel
import com.adheesh.gid.feature.tasks.ui.DeleteTaskDialog
import com.adheesh.gid.feature.tasks.ui.PriorityEditor
import com.adheesh.gid.feature.tasks.ui.TaskDetailPanel
import com.adheesh.gid.feature.tasks.ui.TaskEditorDialog
import com.adheesh.gid.feature.tasks.ui.TaskRow
import kotlinx.coroutines.flow.StateFlow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ── Calendar (SPEC §2.3) ─────────────────────────────────────────────────────
@Composable
fun CalendarScreen(
    viewModel        : CalendarViewModel,
    externalSelection: StateFlow<LocalDate?>,
    onSelectionUsed  : () -> Unit,
    modifier         : Modifier = Modifier,
) {
    val grid         by viewModel.grid.collectAsState()
    val selected     by viewModel.selected.collectAsState()
    val taskDays     by viewModel.taskDays.collectAsState()
    val holidays     by viewModel.holidayByDay.collectAsState()
    val dayTasks     by viewModel.dayTasks.collectAsState()
    val selectedTask by viewModel.selectedTask.collectAsState()
    val pending      by externalSelection.collectAsState()

    // A date tapped on Home's calendar strip.
    LaunchedEffect(pending) {
        pending?.let { viewModel.select(it); onSelectionUsed() }
    }

    var holidayDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var editorOpen  by rememberSaveable { mutableStateOf(false) }
    var editingId   by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId  by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle(stringResource(R.string.calendar_title))

        // ── Month header ─────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = viewModel::previousMonth) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.cd_previous_month), tint = GIDText)
            }
            Text(
                grid.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                style     = MaterialTheme.typography.titleLarge,
                color     = GIDText,
                textAlign = TextAlign.Center,
                modifier  = Modifier.weight(1f),
            )
            IconButton(onClick = viewModel::nextMonth) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.cd_next_month), tint = GIDText)
            }
        }

        // ── Month grid ───────────────────────────────────────────────────────
        GIDCard(Modifier.fillMaxWidth()) {
            WeekdayHeader()
            grid.days.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        DayCell(
                            date        = date,
                            inMonth     = date.month == grid.month.month,
                            isToday     = date == today(),
                            isSelected  = date == selected,
                            hasTasks    = date.toEpochDay() in taskDays,
                            isHoliday   = date.toEpochDay() in holidays,
                            onClick     = { viewModel.select(date) },
                            onLongPress = { holidayDate = date.toEpochDay() },
                            modifier    = Modifier.weight(1f),
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.calendar_long_press_hint),
                style    = MaterialTheme.typography.bodySmall,
                color    = GIDMutedText,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        holidays[selected.toEpochDay()]?.let { name ->
            Text(
                if (name.isBlank()) stringResource(R.string.calendar_holiday) else stringResource(R.string.calendar_holiday_named, name),
                style = MaterialTheme.typography.bodyMedium,
                color = GIDHolidayGreen,
            )
        }

        // ── Below the grid: list on the left, detail + priority on the right ─
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            GIDCard(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.calendar_tasks_on, selected.formatShort()),
                        style    = MaterialTheme.typography.titleMedium,
                        color    = GIDText,
                        modifier = Modifier.weight(1f),
                    )
                    FilledIconButton(
                        onClick  = { editingId = null; editorOpen = true },
                        colors   = IconButtonDefaults.filledIconButtonColors(containerColor = GIDCoral, contentColor = OnCard),
                        modifier = Modifier.size(36.dp),
                    ) { Icon(Icons.Rounded.Add, stringResource(R.string.cd_add_task)) }
                }
                Spacer(Modifier.height(4.dp))
                if (dayTasks.isEmpty()) {
                    EmptyText(stringResource(R.string.calendar_no_tasks))
                } else {
                    dayTasks.forEach { task ->
                        TaskRow(
                            task     = task,
                            selected = task.id == selectedTask?.id,
                            onToggle = { viewModel.toggleDone(task) },
                            onClick  = { viewModel.selectTask(task.id) },
                        )
                    }
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GIDCard(Modifier.fillMaxWidth()) {
                    TaskDetailPanel(
                        task           = selectedTask,
                        onProgress     = { p -> selectedTask?.let { viewModel.setProgress(it.id, p) } },
                        onEdit         = { selectedTask?.let { editingId = it.id; editorOpen = true } },
                        onDelete       = { deletingId = selectedTask?.id },
                        onMoveTomorrow = { selectedTask?.let { viewModel.moveToTomorrow(it.id) } },
                    )
                }
                GIDCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.field_priority), style = MaterialTheme.typography.titleMedium, color = GIDText)
                    Spacer(Modifier.height(4.dp))
                    PriorityColumn(selectedTask) { p -> selectedTask?.let { viewModel.setPriority(it.id, p) } }
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }

    // ── Dialogs ─────────────────────────────────────────────────────────────
    holidayDate?.let { day ->
        HolidayDialog(
            date        = LocalDate.ofEpochDay(day),
            currentName = holidays[day],
            onMark      = { name -> viewModel.setHoliday(LocalDate.ofEpochDay(day), name); holidayDate = null },
            onUnmark    = { viewModel.removeHoliday(LocalDate.ofEpochDay(day)); holidayDate = null },
            onDismiss   = { holidayDate = null },
        )
    }
    if (editorOpen) {
        val editing = dayTasks.firstOrNull { it.id == editingId }
        TaskEditorDialog(
            initial     = editing,
            defaultDate = selected,
            onSave      = { draft ->
                if (editing == null) viewModel.addTask(draft) else viewModel.updateTask(editing, draft)
                editorOpen = false
            },
            onDismiss   = { editorOpen = false },
        )
    }
    deletingId?.let { id ->
        val task: TaskEntity? = dayTasks.firstOrNull { it.id == id }
        DeleteTaskDialog(
            title     = task?.title.orEmpty(),
            onConfirm = { viewModel.delete(id); deletingId = null },
            onDismiss = { deletingId = null },
        )
    }
}

/** Narrow column: the three chips stack so they fit beside the list on a phone. */
@Composable
private fun PriorityColumn(task: TaskEntity?, onChange: (Priority) -> Unit) {
    if (task == null) {
        EmptyText(stringResource(R.string.task_select_hint))
        return
    }
    PriorityEditor(selected = Priority.of(task.priority), onChange = onChange, vertical = true)
}

@Composable
private fun WeekdayHeader() {
    Row(Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { day ->
            Text(
                day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                style     = MaterialTheme.typography.labelMedium,
                color     = GIDMutedText,
                textAlign = TextAlign.Center,
                modifier  = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    date        : LocalDate,
    inMonth     : Boolean,
    isToday     : Boolean,
    isSelected  : Boolean,
    hasTasks    : Boolean,
    isHoliday   : Boolean,
    onClick     : () -> Unit,
    onLongPress : () -> Unit,
    modifier    : Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val textColor = when {
        isSelected -> OnCard
        isHoliday  -> GIDHolidayGreen
        !inMonth   -> GIDMutedText.copy(alpha = 0.5f)
        else       -> GIDText
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(2.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick     = onClick,
                onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onLongPress() },
            ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(2.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSelected -> GIDCoral
                        isHoliday  -> GIDHolidayGreen.copy(alpha = 0.15f)
                        else       -> Color.Transparent
                    }
                )
                .then(if (isToday && !isSelected) Modifier.border(2.dp, GIDCyan, CircleShape) else Modifier),
        ) {
            Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.bodySmall, color = textColor)
        }
        Box(Modifier.height(6.dp), contentAlignment = Alignment.Center) {
            if (hasTasks) Dot(if (isHoliday) GIDHolidayGreen else GIDCoral, size = 4.dp)
        }
    }
}

@Composable
private fun HolidayDialog(
    date        : LocalDate,
    currentName : String?,
    onMark      : (String?) -> Unit,
    onUnmark    : () -> Unit,
    onDismiss   : () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(currentName.orEmpty()) }
    val isHoliday = currentName != null
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = GIDSurface,
        shape            = MaterialTheme.shapes.medium,
        title = {
            Text(
                stringResource(if (isHoliday) R.string.holiday_edit_title else R.string.holiday_mark_title, date.formatMedium()),
                color = GIDText,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.holiday_text), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
                GIDTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.field_holiday_name))
            }
        },
        confirmButton = {
            TextButton(onClick = { onMark(name) }) {
                Text(stringResource(if (isHoliday) R.string.action_save else R.string.holiday_mark), color = GIDHolidayGreen)
            }
        },
        dismissButton = {
            Row {
                if (isHoliday) TextButton(onClick = onUnmark) { Text(stringResource(R.string.holiday_unmark), color = GIDCoral) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = GIDMutedText) }
            }
        },
    )
}
