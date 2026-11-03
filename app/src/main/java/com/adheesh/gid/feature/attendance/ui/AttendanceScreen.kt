package com.adheesh.gid.feature.attendance.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.database.AttendanceEntity
import com.adheesh.gid.core.database.AttendanceStatus
import com.adheesh.gid.core.designsystem.Dot
import com.adheesh.gid.core.designsystem.EmptyText
import com.adheesh.gid.core.designsystem.GIDCard
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.GIDTextField
import com.adheesh.gid.core.designsystem.ScreenTitle
import com.adheesh.gid.core.designsystem.SectionTitle
import com.adheesh.gid.core.util.formatMinutesOfDay
import com.adheesh.gid.core.util.formatShort
import com.adheesh.gid.feature.attendance.data.SubjectAttendance
import com.adheesh.gid.feature.attendance.viewmodel.AttendanceViewModel
import com.adheesh.gid.feature.library.ui.Choice
import com.adheesh.gid.feature.library.ui.Label
import com.adheesh.gid.feature.library.ui.color
import java.time.LocalDate

// ── Attendance page (SPEC §2.12) ─────────────────────────────────────────────
// One card per subject with its percent against the target; tap a card for
// its marks, the 'Before GID' counts and its own target.
@Composable
fun AttendanceScreen(
    viewModel : AttendanceViewModel,
    onBack    : () -> Unit,
    modifier  : Modifier = Modifier,
) {
    val summaries     by viewModel.summaries.collectAsState()
    val openSubjectId by viewModel.openSubjectId.collectAsState()

    LazyColumn(
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier            = modifier.fillMaxSize(),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back), tint = GIDText)
                }
                Spacer(Modifier.width(4.dp))
                ScreenTitle(stringResource(R.string.attendance_title))
            }
        }
        if (summaries.isEmpty()) item { EmptyText(stringResource(R.string.attendance_empty)) }
        items(summaries, key = { it.subject.id }) { item ->
            SubjectAttendanceCard(item, onClick = { viewModel.open(item.subject.id) })
        }
    }

    val open = summaries.firstOrNull { it.subject.id == openSubjectId }
    if (open != null) SubjectAttendanceSheet(open, viewModel, onDismiss = { viewModel.open(null) })
}

@Composable
fun SubjectAttendanceCard(item: SubjectAttendance, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = item.summary
    GIDCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(item.subject.color(), size = 10.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                item.subject.name,
                style    = MaterialTheme.typography.titleMedium,
                color    = GIDText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                s.percent?.let { stringResource(R.string.attendance_percent, it) } ?: "–",
                style = MaterialTheme.typography.titleLarge,
                color = s.level.color(),
            )
        }
        AttendanceBar(s, Modifier.padding(vertical = 8.dp))
        Text(s.advice(), style = MaterialTheme.typography.bodyMedium, color = GIDText)
        if (s.total > 0) {
            Text(
                stringResource(R.string.attendance_counts, s.present, s.total, s.target),
                style = MaterialTheme.typography.bodySmall,
                color = GIDMutedText,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SubjectAttendanceSheet(item: SubjectAttendance, viewModel: AttendanceViewModel, onDismiss: () -> Unit) {
    val marks         by viewModel.openMarks.collectAsState()
    val defaultTarget by viewModel.defaultTarget.collectAsState()
    val subject = item.subject
    var present by rememberSaveable(subject.id) { mutableStateOf(subject.priorPresent.toString()) }
    var total   by rememberSaveable(subject.id) { mutableStateOf(subject.priorTotal.toString()) }
    val savePrior = {
        val t = total.toIntOrNull() ?: 0
        val p = present.toIntOrNull() ?: 0
        if (t != subject.priorTotal || p != subject.priorPresent) viewModel.setPrior(subject.id, p, t)
    }

    ModalBottomSheet(
        onDismissRequest = { savePrior(); onDismiss() },
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = GIDSurface,
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding      = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            modifier            = Modifier.fillMaxWidth().navigationBarsPadding(),
        ) {
            item { SubjectAttendanceCard(item, onClick = {}) }

            item { Label(stringResource(R.string.attendance_target)) }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choice(stringResource(R.string.attendance_target_default, defaultTarget), selected = subject.attendanceTarget == null) {
                        viewModel.setTarget(subject.id, null)
                    }
                    TARGETS.forEach { t ->
                        Choice(stringResource(R.string.attendance_percent, t), selected = subject.attendanceTarget == t) {
                            viewModel.setTarget(subject.id, t)
                        }
                    }
                }
            }

            item { Label(stringResource(R.string.attendance_before_gid)) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GIDTextField(
                            value           = present,
                            onValueChange   = { present = it.filter(Char::isDigit).take(4) },
                            label           = stringResource(R.string.attendance_prior_present),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier        = Modifier.weight(1f),
                        )
                        GIDTextField(
                            value           = total,
                            onValueChange   = { total = it.filter(Char::isDigit).take(4) },
                            label           = stringResource(R.string.attendance_prior_total),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier        = Modifier.weight(1f),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.attendance_before_hint),
                            style    = MaterialTheme.typography.bodySmall,
                            color    = GIDMutedText,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = savePrior) { Text(stringResource(R.string.action_save), color = GIDCyan) }
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.attendance_marks), Modifier.padding(top = 8.dp)) }
            if (marks.isEmpty()) item { EmptyText(stringResource(R.string.attendance_no_marks)) }
            items(marks, key = { it.id }) { mark ->
                MarkRow(mark, onStatus = { viewModel.setStatus(mark, it) }, onRemove = { viewModel.delete(mark) })
            }
        }
    }
}

@Composable
private fun MarkRow(mark: AttendanceEntity, onStatus: (AttendanceStatus) -> Unit, onRemove: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val status = AttendanceStatus.of(mark.status)
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { menuOpen = true }.padding(vertical = 8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(LocalDate.ofEpochDay(mark.dateEpochDay).formatShort(), style = MaterialTheme.typography.bodyMedium, color = GIDText)
                Text(formatMinutesOfDay(mark.startMinutes), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
            }
            if (status != null) Text(status.label(), style = MaterialTheme.typography.labelLarge, color = status.color())
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = GIDSurface) {
            AttendanceStatus.entries.forEach { option ->
                DropdownMenuItem(
                    text    = { Text(option.label(), color = option.color()) },
                    onClick = { menuOpen = false; onStatus(option) },
                )
            }
            DropdownMenuItem(
                text    = { Text(stringResource(R.string.attendance_remove), color = GIDText) },
                onClick = { menuOpen = false; onRemove() },
            )
        }
    }
}

private val TARGETS = listOf(60, 65, 70, 75, 80, 85, 90)
