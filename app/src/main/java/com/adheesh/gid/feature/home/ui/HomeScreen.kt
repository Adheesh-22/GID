package com.adheesh.gid.feature.home.ui

import androidx.compose.ui.text.font.FontWeight // fix : orientation of the home screen
import androidx.compose.ui.unit.sp // fix : orientation of the home screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.database.Priority
import com.adheesh.gid.core.designsystem.Dot
import com.adheesh.gid.core.designsystem.GIDCard
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDHolidayGreen
import com.adheesh.gid.core.designsystem.GIDMustard
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDPeriwinkle
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.designsystem.ProfileAvatar
import com.adheesh.gid.core.util.formatMinutesOfDay
import com.adheesh.gid.core.util.formatShort
import com.adheesh.gid.core.util.today
import com.adheesh.gid.feature.home.viewmodel.HomeViewModel
import com.adheesh.gid.feature.tasks.ui.DeleteTaskDialog
import com.adheesh.gid.feature.tasks.ui.PriorityEditor
import com.adheesh.gid.feature.tasks.ui.TaskDetailPanel
import com.adheesh.gid.feature.tasks.ui.TaskEditorDialog
import com.adheesh.gid.feature.tasks.ui.TaskRow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// ── Home (SPEC §2.2) ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel        : HomeViewModel,
    onOpenSettings   : () -> Unit,
    onOpenCalendar   : (LocalDate) -> Unit,
    onOpenAlarms     : () -> Unit,
    onOpenTimetable  : () -> Unit,
    modifier         : Modifier = Modifier,
) {
    val name       by viewModel.name.collectAsState()
    val photo      by viewModel.photoPath.collectAsState()
    val stripDays  by viewModel.stripDays.collectAsState()
    val taskDays   by viewModel.stripTaskDays.collectAsState()
    val holidays   by viewModel.stripHolidays.collectAsState()
    val nextAlarm  by viewModel.nextAlarm.collectAsState()
    val upcoming   by viewModel.upcoming.collectAsState()
    val tasks      by viewModel.todayTasks.collectAsState()

    var sheetTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorOpen  by rememberSaveable { mutableStateOf(false) }
    var editingId   by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId  by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── Greeting ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top // Keeps the avatar at the top right
        ) {
            Text(
                // Formatted with a comma and a newline to match the reference
                text     = name?.let { "Hello,\n${it.substringBefore(' ')}" } ?: "Hello,\nThere",
                style    = MaterialTheme.typography.displayLarge,
                fontSize = 56.sp, // Made the text larger
                lineHeight = 60.sp,
                fontWeight = FontWeight.ExtraBold, // Made the text thicker
                color    = GIDText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 24.dp) // Pushes the text a bit lower
            )
            ProfileAvatar(
                photoPath = photo, 
                name = name, 
                size = 56.dp, // Slightly larger avatar to balance the large text
                onClick = onOpenSettings
            )
        }

        // Pushes the Calendar and everything below it further down
        Spacer(modifier = Modifier.height(32.dp))

        // ── Two-week calendar strip ──────────────────────────────────────────
        GIDCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth()) {
                DayOfWeek.entries.forEach { d ->
                    Text(
                        d.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium,
                        color = GIDMutedText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            stripDays.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    week.forEach { date ->
                        StripDay(
                            date      = date,
                            isToday   = date == today(),
                            hasTasks  = date.toEpochDay() in taskDays,
                            isHoliday = date.toEpochDay() in holidays,
                            onClick   = { onOpenCalendar(date) },
                            modifier  = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // ── Left column + tall Tasks card ────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GIDCard(Modifier.fillMaxWidth(), color = GIDPeriwinkle, onClick = onOpenAlarms) {
                    CardHeading(stringResource(R.string.home_alarm))
                    val alarm = nextAlarm
                    if (alarm == null) {
                        Text(stringResource(R.string.home_no_alarms), style = MaterialTheme.typography.bodyMedium, color = OnCard)
                    } else {
                        Text(
                            formatMinutesOfDay(alarm.alarm.hour * 60 + alarm.alarm.minute),
                            style = MaterialTheme.typography.displayMedium,
                            color = OnCard,
                        )
                        Text(
                            listOfNotNull(
                                alarm.type?.name,
                                if (alarm.triggerAt.toLocalDate() != today()) alarm.triggerAt.toLocalDate().formatShort() else null,
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnCard,
                        )
                    }
                }

                GIDCard(Modifier.fillMaxWidth(), color = GIDMustard, onClick = onOpenTimetable) {
                    CardHeading(stringResource(R.string.home_timetable))
                    when {
                        upcoming.isHoliday || upcoming.classes.isEmpty() ->
                            Text(stringResource(R.string.home_no_classes), style = MaterialTheme.typography.bodyMedium, color = OnCard)
                        else -> {
                            if (upcoming.day != null && upcoming.day != today()) {
                                Text(upcoming.day!!.formatShort(), style = MaterialTheme.typography.labelMedium, color = OnCard)
                            }
                            upcoming.classes.forEach { c ->
                                Column(Modifier.padding(top = 4.dp)) {
                                    Text(c.subject, style = MaterialTheme.typography.bodyMedium, color = OnCard, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        listOfNotNull(formatMinutesOfDay(c.startMinutes), c.room).joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnCard,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            GIDCard(Modifier.weight(1f).heightIn(min = 300.dp, max = 480.dp), color = GIDCoral) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.home_tasks), style = MaterialTheme.typography.titleLarge, color = OnCard, modifier = Modifier.weight(1f))
                    FilledIconButton(
                        onClick  = { editingId = null; editorOpen = true },
                        colors   = IconButtonDefaults.filledIconButtonColors(containerColor = OnCard, contentColor = GIDCoral),
                        modifier = Modifier.size(32.dp),
                    ) { Icon(Icons.Rounded.Add, stringResource(R.string.cd_add_task)) }
                }
                // Scrolls inside the card when there are many tasks.
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (tasks.isEmpty()) {
                        Text(stringResource(R.string.home_no_tasks), style = MaterialTheme.typography.bodyMedium, color = OnCard, modifier = Modifier.padding(top = 8.dp))
                    }
                    tasks.forEach { task ->
                        TaskRow(
                            task         = task,
                            onToggle     = { viewModel.toggleDone(task) },
                            onClick      = { sheetTaskId = task.id },
                            contentColor = OnCard,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }

    // ── Task detail sheet (same component as Calendar) ───────────────────────
    val sheetTask = tasks.firstOrNull { it.id == sheetTaskId }
    if (sheetTask != null) {
        ModalBottomSheet(
            onDismissRequest = { sheetTaskId = null },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = GIDSurface,
        ) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TaskDetailPanel(
                    task           = sheetTask,
                    onProgress     = { viewModel.setProgress(sheetTask.id, it) },
                    onEdit         = { editingId = sheetTask.id; editorOpen = true },
                    onDelete       = { deletingId = sheetTask.id },
                    onMoveTomorrow = { viewModel.moveToTomorrow(sheetTask.id); sheetTaskId = null },
                )
                Text(stringResource(R.string.field_priority), style = MaterialTheme.typography.labelMedium, color = GIDMutedText)
                PriorityEditor(selected = Priority.of(sheetTask.priority), onChange = { viewModel.setPriority(sheetTask.id, it) })
            }
        }
    }
    if (editorOpen) {
        val editing = tasks.firstOrNull { it.id == editingId }
        TaskEditorDialog(
            initial     = editing,
            defaultDate = today(),
            onSave      = { draft ->
                if (editing == null) viewModel.addTask(draft) else viewModel.updateTask(editing, draft)
                editorOpen = false
            },
            onDismiss   = { editorOpen = false },
        )
    }
    deletingId?.let { id ->
        DeleteTaskDialog(
            title     = tasks.firstOrNull { it.id == id }?.title.orEmpty(),
            onConfirm = { viewModel.delete(id); deletingId = null; sheetTaskId = null },
            onDismiss = { deletingId = null },
        )
    }
}

/** Card heading with a chevron, telling the user it opens a popup. */
@Composable
private fun CardHeading(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = OnCard, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = OnCard)
    }
}

@Composable
private fun StripDay(
    date      : LocalDate,
    isToday   : Boolean,
    hasTasks  : Boolean,
    isHoliday : Boolean,
    onClick   : () -> Unit,
    modifier  : Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(vertical = 2.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isToday   -> GIDCyan
                        isHoliday -> GIDHolidayGreen.copy(alpha = 0.2f)
                        else      -> Color.Transparent
                    }
                )
                .then(if (isHoliday && !isToday) Modifier.border(1.dp, GIDHolidayGreen, CircleShape) else Modifier),
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isToday   -> OnCard
                    isHoliday -> GIDHolidayGreen
                    else      -> GIDText
                },
            )
        }
        Box(Modifier.height(8.dp), contentAlignment = Alignment.Center) {
            if (hasTasks) Dot(GIDCoral, size = 4.dp)
        }
    }
}
