package com.adheesh.gid.feature.exams.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.flowOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.database.ExamEntity
import com.adheesh.gid.core.database.ExamKind
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDDatePickerDialog
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.GIDTextField
import com.adheesh.gid.core.designsystem.GIDTimePickerDialog
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.util.formatMedium
import com.adheesh.gid.core.util.formatMinutesOfDay
import com.adheesh.gid.core.util.today
import com.adheesh.gid.feature.exams.data.ExamDraft
import com.adheesh.gid.feature.exams.viewmodel.ExamEditingViewModel
import com.adheesh.gid.feature.library.ui.AddChip
import com.adheesh.gid.feature.library.ui.Choice
import com.adheesh.gid.feature.library.ui.Label
import com.adheesh.gid.feature.library.ui.NameDialog
import com.adheesh.gid.feature.library.ui.color
import kotlinx.coroutines.launch
import java.time.LocalDate

// ── Exam editor (SPEC §2.10) ─────────────────────────────────────────────────
// Add or edit an exam or assignment. A subject and a date are required; the
// title falls back to the kind ("Exam") when left empty.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExamEditorSheet(
    viewModel        : ExamEditingViewModel,
    initial          : ExamEntity?,
    onDismiss        : () -> Unit,
    presetSubjectId  : String? = null,
) {
    val subjects by viewModel.activeSubjects.collectAsState()
    val scope = rememberCoroutineScope()

    var kind      by rememberSaveable { mutableStateOf(initial?.kind?.let { ExamKind.of(it) } ?: ExamKind.EXAM) }
    var subjectId by rememberSaveable { mutableStateOf(initial?.subjectId ?: presetSubjectId) }
    var title     by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var day       by rememberSaveable { mutableStateOf(initial?.dateEpochDay) }
    var time      by rememberSaveable { mutableStateOf(initial?.timeMinutes) }
    var room      by rememberSaveable { mutableStateOf(initial?.room.orEmpty()) }
    var notes     by rememberSaveable { mutableStateOf(initial?.notes.orEmpty()) }

    // Units this exam covers; loaded once for an existing exam.
    var pickedUnits  by rememberSaveable { mutableStateOf(listOf<String>()) }
    var unitsLoaded  by rememberSaveable { mutableStateOf(initial == null) }
    LaunchedEffect(initial?.id) {
        if (!unitsLoaded && initial != null) {
            pickedUnits = viewModel.examUnitIds(initial.id).toList()
            unitsLoaded = true
        }
    }
    val units by remember(subjectId) {
        subjectId?.let { viewModel.unitsFor(it) } ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    var newSubjectOpen by rememberSaveable { mutableStateOf(false) }
    var dateOpen       by rememberSaveable { mutableStateOf(false) }
    var timeOpen       by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = GIDSurface,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                stringResource(if (initial == null) R.string.exam_add else R.string.exam_edit),
                style = MaterialTheme.typography.titleLarge,
                color = GIDText,
            )

            Label(stringResource(R.string.exam_kind))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExamKind.entries.forEach { k -> Choice(k.label(), selected = kind == k) { kind = k } }
            }

            Label(stringResource(R.string.sort_subject))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                subjects.forEach { s -> Choice(s.name, selected = subjectId == s.id, selectedColor = s.color()) { subjectId = s.id } }
                AddChip(stringResource(R.string.sort_new_subject)) { newSubjectOpen = true }
            }

            if (units.isNotEmpty()) {
                Label(stringResource(R.string.syllabus_exam_units))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    units.forEach { u ->
                        val id = u.unit.id
                        Choice(u.unit.name, selected = id in pickedUnits) {
                            pickedUnits = if (id in pickedUnits) pickedUnits - id else pickedUnits + id
                        }
                    }
                }
                Text(stringResource(R.string.syllabus_exam_units_hint), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
            }

            GIDTextField(value = title, onValueChange = { title = it }, label = stringResource(R.string.exam_title_field))

            Label(stringResource(R.string.exam_when))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { dateOpen = true }) {
                    Text(day?.let { LocalDate.ofEpochDay(it).formatMedium() } ?: stringResource(R.string.exam_pick_date), color = GIDCyan)
                }
                TextButton(onClick = { timeOpen = true }) {
                    Text(time?.let { formatMinutesOfDay(it) } ?: stringResource(R.string.exam_pick_time), color = if (time != null) GIDCyan else GIDMutedText)
                }
                if (time != null) {
                    TextButton(onClick = { time = null }) { Text(stringResource(R.string.action_clear), color = GIDMutedText) }
                }
            }

            GIDTextField(value = room, onValueChange = { room = it }, label = stringResource(R.string.exam_room_field))
            GIDTextField(value = notes, onValueChange = { notes = it }, label = stringResource(R.string.exam_notes_field), singleLine = false)

            val chosenSubject = subjectId
            val chosenDay = day
            val ready = chosenSubject != null && chosenDay != null
            Button(
                enabled  = ready,
                onClick  = {
                    if (chosenSubject == null || chosenDay == null) return@Button
                    viewModel.save(
                        ExamDraft(
                            id           = initial?.id,
                            subjectId    = chosenSubject,
                            title        = title,
                            kind         = kind,
                            dateEpochDay = chosenDay,
                            timeMinutes  = time,
                            room         = room,
                            notes        = notes,
                        ),
                        // Only units of the chosen subject; a subject change drops the others.
                        unitIds = pickedUnits.filter { id -> units.any { it.unit.id == id } }.toSet(),
                    )
                    onDismiss()
                },
                shape    = CircleShape,
                colors   = ButtonDefaults.buttonColors(containerColor = GIDCyan, contentColor = OnCard),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(stringResource(if (ready) R.string.action_save else R.string.exam_needs_subject_date))
            }
        }
    }

    if (newSubjectOpen) {
        NameDialog(stringResource(R.string.sort_new_subject), onDismiss = { newSubjectOpen = false }) { name ->
            scope.launch { subjectId = viewModel.createSubject(name).id }
        }
    }
    if (dateOpen) {
        GIDDatePickerDialog(
            initial   = day?.let { LocalDate.ofEpochDay(it) } ?: today(),
            onDismiss = { dateOpen = false },
            onPicked  = { day = it.toEpochDay(); dateOpen = false },
        )
    }
    if (timeOpen) {
        GIDTimePickerDialog(
            initialMinutes = time ?: (10 * 60),
            onDismiss      = { timeOpen = false },
            onPicked       = { time = it; timeOpen = false },
        )
    }
}
