package com.adheesh.gid.feature.alarm.ui

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.adheesh.gid.R
import com.adheesh.gid.core.database.ActivityCategory
import com.adheesh.gid.core.database.ActivitySessionEntity
import com.adheesh.gid.core.database.ActivityTypeEntity
import com.adheesh.gid.core.database.AlarmEntity
import com.adheesh.gid.core.designsystem.ActivityTypePalette
import com.adheesh.gid.core.designsystem.Dot
import com.adheesh.gid.core.designsystem.EmptyText
import com.adheesh.gid.core.designsystem.Error
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDDialog
import com.adheesh.gid.core.designsystem.GIDMustard
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDPeriwinkle
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDSurfaceHigh
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.GIDTextField
import com.adheesh.gid.core.designsystem.GIDTimePickerDialog
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.designsystem.colorFromHex
import com.adheesh.gid.core.scheduling.ExactAlarms
import com.adheesh.gid.core.util.formatMinutesOfDay
import com.adheesh.gid.feature.alarm.domain.AlarmTimeCalculator
import com.adheesh.gid.feature.alarm.viewmodel.AlarmDraft
import com.adheesh.gid.feature.alarm.viewmodel.AlarmViewModel
import com.adheesh.gid.feature.sessions.domain.SessionRules
import com.adheesh.gid.feature.tasks.ui.PickerField
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

// ── AlarmPopup (SPEC §2.6) ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmPopup(viewModel: AlarmViewModel, onDismiss: () -> Unit) {
    val alarms  by viewModel.alarmList.collectAsState()
    val types   by viewModel.typeList.collectAsState()
    val timers  by viewModel.runningTimers.collectAsState()

    var tab          by rememberSaveable { mutableStateOf(0) }       // 0 = Alarms, 1 = Timers
    var editorOpen   by rememberSaveable { mutableStateOf(false) }
    var editingId    by rememberSaveable { mutableStateOf<String?>(null) }
    var typesOpen    by rememberSaveable { mutableStateOf(false) }

    GIDDialog(title = stringResource(R.string.alarm_title), onDismiss = onDismiss) {
        Column(
            Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(R.string.alarm_tab_alarms, R.string.alarm_tab_timers).forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = tab == index,
                        onClick  = { tab = index },
                        shape    = SegmentedButtonDefaults.itemShape(index, 2),
                        colors   = SegmentedButtonDefaults.colors(
                            activeContainerColor   = GIDPeriwinkle,
                            activeContentColor     = OnCard,
                            inactiveContainerColor = GIDSurface,
                            inactiveContentColor   = GIDText,
                        ),
                    ) { Text(stringResource(label)) }
                }
            }

            PermissionBanner()

            if (tab == 0) {
                if (alarms.isEmpty()) EmptyText(stringResource(R.string.alarm_none))
                alarms.forEach { alarm ->
                    AlarmRow(
                        alarm    = alarm,
                        type     = types.firstOrNull { it.id == alarm.typeId },
                        onToggle = { viewModel.setEnabled(alarm, it) },
                        onClick  = { editingId = alarm.id; editorOpen = true },
                    )
                }
                TextButton(onClick = { editingId = null; editorOpen = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = GIDCyan)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.alarm_add), color = GIDCyan)
                }
            } else {
                TimersTab(types = types, timers = timers, onStart = viewModel::startTimer, onStop = viewModel::stopTimer)
            }

            // ── Activity types manager ──────────────────────────────────────
            TextButton(onClick = { typesOpen = true }) {
                Icon(Icons.Rounded.Category, contentDescription = null, tint = GIDMutedText)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.types_title), color = GIDMutedText)
            }
        }
    }

    if (editorOpen) {
        AlarmEditorDialog(
            initial   = alarms.firstOrNull { it.id == editingId },
            types     = types,
            onSave    = { existing, draft -> viewModel.saveAlarm(existing, draft); editorOpen = false },
            onDelete  = { alarm -> viewModel.deleteAlarm(alarm); editorOpen = false },
            onDismiss = { editorOpen = false },
        )
    }
    if (typesOpen) {
        ActivityTypesDialog(
            types     = types,
            onSave    = viewModel::saveType,
            onDelete  = viewModel::deleteType,
            onDismiss = { typesOpen = false },
        )
    }
}

/** Shown when Android would stop alarms from ringing on time or full-screen. */
@Composable
private fun PermissionBanner() {
    val context = LocalContext.current
    var exactOk by remember { mutableStateOf(true) }
    var fullScreenOk by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        exactOk = ExactAlarms.canScheduleExact(context)
        fullScreenOk = Build.VERSION.SDK_INT < 34 ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        onPauseOrDispose { }
    }
    if (exactOk && fullScreenOk) return

    val packageUri = Uri.fromParts("package", context.packageName, null)
    Surface(color = GIDMustard.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Warning, contentDescription = null, tint = GIDMustard)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(if (!exactOk) R.string.alarm_warn_exact else R.string.alarm_warn_full_screen),
                style = MaterialTheme.typography.bodySmall,
                color = GIDText,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                val intent = when {
                    !exactOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri)
                    Build.VERSION.SDK_INT >= 34 -> Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri)
                    else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
                }
                runCatching { context.startActivity(intent) }
            }) { Text(stringResource(R.string.action_fix), color = GIDCyan) }
        }
    }
}

@Composable
private fun AlarmRow(alarm: AlarmEntity, type: ActivityTypeEntity?, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                formatMinutesOfDay(alarm.hour * 60 + alarm.minute),
                style = MaterialTheme.typography.displayMedium,
                color = if (alarm.enabled) GIDText else GIDMutedText,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (type != null) TypeChip(type)
                Text(
                    listOfNotNull(alarm.label.ifBlank { null }, repeatText(alarm.repeatDaysMask)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = GIDMutedText,
                )
            }
        }
        Switch(
            checked = alarm.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedTrackColor = GIDPeriwinkle, checkedThumbColor = OnCard),
        )
    }
}

@Composable
private fun TypeChip(type: ActivityTypeEntity) {
    Surface(color = GIDSurfaceHigh, shape = MaterialTheme.shapes.large) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Dot(colorFromHex(type.colorHex), size = 8.dp)
            Spacer(Modifier.width(4.dp))
            Text(type.name, style = MaterialTheme.typography.labelMedium, color = GIDText)
        }
    }
}

@Composable
private fun repeatText(mask: Int): String = when (mask) {
    0           -> stringResource(R.string.alarm_once)
    0b1111111   -> stringResource(R.string.alarm_every_day)
    0b0011111   -> stringResource(R.string.alarm_weekdays)
    else        -> (1..7).filter { AlarmTimeCalculator.isDayEnabled(mask, it) }
        .joinToString(" ") { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

// ── Timers ───────────────────────────────────────────────────────────────────
@Composable
private fun TimersTab(
    types   : List<ActivityTypeEntity>,
    timers  : List<ActivitySessionEntity>,
    onStart : (typeId: String, minutes: Int) -> Unit,
    onStop  : (sessionId: String) -> Unit,
) {
    // Ticks once a second while timers are visible; the end time itself is in the database.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timers.isNotEmpty()) {
        while (timers.isNotEmpty()) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    timers.forEach { session ->
        val endsAt = SessionRules.endsAt(session) ?: return@forEach
        val left = ((endsAt - now) / 1000).coerceAtLeast(0)
        val type = types.firstOrNull { it.id == session.typeId }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("%02d:%02d".format(left / 60, left % 60), style = MaterialTheme.typography.displayMedium, color = GIDText)
                Text(type?.name.orEmpty(), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
            }
            TextButton(onClick = { onStop(session.id) }) { Text(stringResource(R.string.action_stop), color = Error) }
        }
    }

    var minutes by rememberSaveable { mutableStateOf("25") }
    var typeId  by rememberSaveable { mutableStateOf<String?>(null) }
    val focusTypes = types.filter { ActivityCategory.of(it.category) != ActivityCategory.CLASS }
    val chosen = typeId ?: focusTypes.firstOrNull { ActivityCategory.of(it.category) == ActivityCategory.FOCUS }?.id

    Text(stringResource(R.string.timer_new), style = MaterialTheme.typography.titleMedium, color = GIDText)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(5, 15, 25, 45, 60).forEach { m ->
            FilterChip(
                selected = minutes == m.toString(),
                onClick  = { minutes = m.toString() },
                label    = { Text(stringResource(R.string.minutes_short, m)) },
                colors   = FilterChipDefaults.filterChipColors(selectedContainerColor = GIDPeriwinkle, selectedLabelColor = OnCard, labelColor = GIDText),
            )
        }
    }
    GIDTextField(
        value           = minutes,
        onValueChange   = { v -> minutes = v.filter(Char::isDigit).take(3) },
        label           = stringResource(R.string.field_duration_minutes),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    TypePicker(focusTypes, chosen) { typeId = it }
    Button(
        onClick  = { val m = minutes.toIntOrNull(); if (m != null && m > 0 && chosen != null) onStart(chosen, m) },
        enabled  = (minutes.toIntOrNull() ?: 0) > 0 && chosen != null,
        modifier = Modifier.fillMaxWidth(),
        shape    = MaterialTheme.shapes.large,
        colors   = ButtonDefaults.buttonColors(containerColor = GIDCyan, contentColor = OnCard),
    ) { Text(stringResource(R.string.action_start)) }
}

@Composable
private fun TypePicker(types: List<ActivityTypeEntity>, selectedId: String?, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        types.forEach { type ->
            FilterChip(
                selected    = type.id == selectedId,
                onClick     = { onSelect(type.id) },
                label       = { Text(type.name, style = MaterialTheme.typography.labelMedium) },
                leadingIcon = { Dot(colorFromHex(type.colorHex), size = 8.dp) },
                colors      = FilterChipDefaults.filterChipColors(selectedContainerColor = GIDSurfaceHigh, selectedLabelColor = GIDText, labelColor = GIDMutedText),
            )
        }
    }
}

// ── Alarm add / edit ─────────────────────────────────────────────────────────
@Composable
private fun AlarmEditorDialog(
    initial   : AlarmEntity?,
    types     : List<ActivityTypeEntity>,
    onSave    : (AlarmEntity?, AlarmDraft) -> Unit,
    onDelete  : (AlarmEntity) -> Unit,
    onDismiss : () -> Unit,
) {
    var minutes    by rememberSaveable { mutableStateOf(initial?.let { it.hour * 60 + it.minute } ?: (7 * 60)) }
    var label      by rememberSaveable { mutableStateOf(initial?.label.orEmpty()) }
    var typeId     by rememberSaveable { mutableStateOf(initial?.typeId ?: types.firstOrNull { it.category == ActivityCategory.SLEEP.name }?.id) }
    var mask       by rememberSaveable { mutableStateOf(initial?.repeatDaysMask ?: 0) }
    var sleepNow   by rememberSaveable { mutableStateOf(false) }
    var pickTime   by rememberSaveable { mutableStateOf(false) }

    val category = types.firstOrNull { it.id == typeId }?.let { ActivityCategory.of(it.category) }
    val isSleepType = category == ActivityCategory.SLEEP || category == ActivityCategory.NAP

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = GIDSurface,
        shape            = MaterialTheme.shapes.medium,
        title = { Text(stringResource(if (initial == null) R.string.alarm_add else R.string.alarm_edit), color = GIDText) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerField(stringResource(R.string.field_time), formatMinutesOfDay(minutes)) { pickTime = true }
                GIDTextField(
                    value           = label,
                    onValueChange   = { label = it },
                    label           = stringResource(R.string.field_label),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Text(stringResource(R.string.field_type), style = MaterialTheme.typography.labelMedium, color = GIDMutedText)
                TypePicker(types, typeId) { typeId = it }
                Text(stringResource(R.string.field_repeat), style = MaterialTheme.typography.labelMedium, color = GIDMutedText)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..7).forEach { d ->
                        FilterChip(
                            selected = AlarmTimeCalculator.isDayEnabled(mask, d),
                            onClick  = { mask = AlarmTimeCalculator.toggleDay(mask, d) },
                            label    = { Text(DayOfWeek.of(d).getDisplayName(TextStyle.NARROW, Locale.getDefault())) },
                            colors   = FilterChipDefaults.filterChipColors(selectedContainerColor = GIDPeriwinkle, selectedLabelColor = OnCard, labelColor = GIDText),
                        )
                    }
                }
                if (isSleepType) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.alarm_start_sleep_now), color = GIDText)
                            Text(stringResource(R.string.alarm_start_sleep_hint), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
                        }
                        Switch(checked = sleepNow, onCheckedChange = { sleepNow = it }, colors = SwitchDefaults.colors(checkedTrackColor = GIDPeriwinkle, checkedThumbColor = OnCard))
                    }
                }
                if (initial != null) {
                    TextButton(onClick = { onDelete(initial) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = Error)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.alarm_delete), color = Error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = typeId != null,
                onClick = {
                    val type = typeId ?: return@TextButton
                    onSave(initial, AlarmDraft(label, type, minutes / 60, minutes % 60, mask, enabled = true, startSleepNow = sleepNow && isSleepType))
                },
            ) { Text(stringResource(R.string.action_save), color = GIDCyan) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = GIDMutedText) } },
    )

    if (pickTime) {
        GIDTimePickerDialog(initialMinutes = minutes, onDismiss = { pickTime = false }, onPicked = { minutes = it })
    }
}

// ── Activity types manager ───────────────────────────────────────────────────
@Composable
private fun ActivityTypesDialog(
    types     : List<ActivityTypeEntity>,
    onSave    : (ActivityTypeEntity?, String, String, ActivityCategory) -> Unit,
    onDelete  : (ActivityTypeEntity) -> Unit,
    onDismiss : () -> Unit,
) {
    var editing by remember { mutableStateOf<ActivityTypeEntity?>(null) }
    var adding  by rememberSaveable { mutableStateOf(false) }

    GIDDialog(title = stringResource(R.string.types_title), onDismiss = onDismiss) {
        Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
            types.forEach { type ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { editing = type }.padding(vertical = 6.dp),
                ) {
                    Dot(colorFromHex(type.colorHex), size = 14.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(type.name, color = GIDText, style = MaterialTheme.typography.bodyMedium)
                        Text(categoryLabel(ActivityCategory.of(type.category)), color = GIDMutedText, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!type.isDefault) {
                        IconButton(onClick = { onDelete(type) }) {
                            Icon(Icons.Rounded.Delete, stringResource(R.string.cd_delete_type), tint = Error)
                        }
                    }
                }
            }
            TextButton(onClick = { adding = true }) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = GIDCyan)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.types_add), color = GIDCyan)
            }
            Text(stringResource(R.string.types_default_note), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
        }
    }

    if (adding || editing != null) {
        TypeEditorDialog(
            initial   = editing,
            onSave    = { name, color, category -> onSave(editing, name, color, category); editing = null; adding = false },
            onDismiss = { editing = null; adding = false },
        )
    }
}

@Composable
private fun TypeEditorDialog(
    initial   : ActivityTypeEntity?,
    onSave    : (String, String, ActivityCategory) -> Unit,
    onDismiss : () -> Unit,
) {
    var name     by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var color    by rememberSaveable { mutableStateOf(initial?.colorHex ?: ActivityTypePalette.first().first) }
    var category by rememberSaveable { mutableStateOf(initial?.let { ActivityCategory.of(it.category) } ?: ActivityCategory.FOCUS) }
    val lockedCategory = initial?.isDefault == true

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = GIDSurface,
        shape            = MaterialTheme.shapes.medium,
        title = { Text(stringResource(if (initial == null) R.string.types_add else R.string.types_edit), color = GIDText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GIDTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.field_name))
                Text(stringResource(R.string.field_color), style = MaterialTheme.typography.labelMedium, color = GIDMutedText)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActivityTypePalette.forEach { (hex, c) ->
                        FilterChip(
                            selected = hex.equals(color, ignoreCase = true),
                            onClick  = { color = hex },
                            label    = { Dot(c, size = 16.dp) },
                            colors   = FilterChipDefaults.filterChipColors(selectedContainerColor = GIDSurfaceHigh),
                        )
                    }
                }
                Text(stringResource(R.string.field_category), style = MaterialTheme.typography.labelMedium, color = GIDMutedText)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActivityCategory.entries.forEach { c ->
                        FilterChip(
                            selected = c == category,
                            enabled  = !lockedCategory,
                            onClick  = { category = c },
                            label    = { Text(categoryLabel(c), style = MaterialTheme.typography.labelMedium) },
                            colors   = FilterChipDefaults.filterChipColors(selectedContainerColor = GIDPeriwinkle, selectedLabelColor = OnCard, labelColor = GIDText),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name, color, category) }) {
                Text(stringResource(R.string.action_save), color = GIDCyan)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = GIDMutedText) } },
    )
}

@Composable
fun categoryLabel(category: ActivityCategory): String = stringResource(
    when (category) {
        ActivityCategory.SLEEP -> R.string.category_sleep
        ActivityCategory.NAP   -> R.string.category_nap
        ActivityCategory.FOCUS -> R.string.category_focus
        ActivityCategory.CLASS -> R.string.category_class
        ActivityCategory.FREE  -> R.string.category_free
        ActivityCategory.OTHER -> R.string.category_other
    }
)
