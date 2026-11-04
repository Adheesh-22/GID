package com.adheesh.gid.feature.library.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.database.CaptureEntity
import com.adheesh.gid.core.database.CaptureType
import com.adheesh.gid.core.database.ExamKind
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDDatePickerDialog
import com.adheesh.gid.core.designsystem.GIDDialog
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDSurfaceHigh
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.GIDTextField
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.util.formatMedium
import com.adheesh.gid.core.util.today
import com.adheesh.gid.feature.capture.data.SortChoice
import com.adheesh.gid.feature.exams.ui.label
import com.adheesh.gid.feature.library.viewmodel.SortingViewModel
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

// ── Sort sheet (SPEC §2.8) ───────────────────────────────────────────────────
// One capture: everything is prefilled from it (or its suggested subject).
// Several captures: only type, subject and heading are set; each keeps its own
// title and note.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SortSheet(
    viewModel : SortingViewModel,
    captures  : List<CaptureEntity>,
    onDismiss : () -> Unit,
    onSorted  : () -> Unit = {},
) {
    if (captures.isEmpty()) return
    val single = captures.singleOrNull()
    val subjects by viewModel.activeSubjects.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val scope = rememberCoroutineScope()

    var type        by rememberSaveable { mutableStateOf(single?.type) }
    var subjectId   by rememberSaveable { mutableStateOf(single?.let { it.subjectId ?: it.suggestedSubjectId } ?: captures.firstNotNullOfOrNull { it.suggestedSubjectId }) }
    var headingId   by rememberSaveable { mutableStateOf(single?.headingId) }
    var title       by rememberSaveable { mutableStateOf(single?.title.orEmpty()) }
    var note        by rememberSaveable { mutableStateOf(single?.note.orEmpty()) }
    var dueEpochDay by rememberSaveable { mutableStateOf(single?.dueEpochDay) }
    // Deadline extras: NONE, TASK, or an ExamKind name.
    var alsoCreate  by rememberSaveable { mutableStateOf(if (single?.linkedTaskId == null) ALSO_TASK else ALSO_NONE) }

    var newSubjectOpen by rememberSaveable { mutableStateOf(false) }
    var newCategoryOpen by rememberSaveable { mutableStateOf(false) }
    var newHeadingOpen by rememberSaveable { mutableStateOf(false) }
    var datePickerOpen by rememberSaveable { mutableStateOf(false) }

    val headingsFlow = remember(subjectId) { subjectId?.let { viewModel.headingsFor(it) } ?: flowOf(emptyList()) }
    val headings by headingsFlow.collectAsState(initial = emptyList())

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
                if (single != null) stringResource(R.string.sort_title) else pluralStringResource(R.plurals.sort_title_many, captures.size, captures.size),
                style = MaterialTheme.typography.titleLarge,
                color = GIDText,
            )

            Label(stringResource(R.string.sort_type))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { c ->
                    Choice(c.name, selected = type == c.id) { type = c.id }
                }
                AddChip(stringResource(R.string.category_new)) { newCategoryOpen = true }
            }

            Label(stringResource(R.string.sort_subject))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                subjects.forEach { s ->
                    Choice(s.name, selected = subjectId == s.id, selectedColor = s.color()) {
                        subjectId = if (subjectId == s.id) null else s.id
                        headingId = null   // a heading belongs to one subject
                    }
                }
                AddChip(stringResource(R.string.sort_new_subject)) { newSubjectOpen = true }
            }

            if (subjectId != null) {
                Label(stringResource(R.string.sort_heading))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choice(stringResource(R.string.sort_no_heading), selected = headingId == null) { headingId = null }
                    headings.forEach { h -> Choice(h.name, selected = headingId == h.id) { headingId = h.id } }
                    AddChip(stringResource(R.string.sort_new_heading)) { newHeadingOpen = true }
                }
            }

            if (type == CaptureType.DEADLINE.name) {
                Label(stringResource(R.string.sort_due))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { datePickerOpen = true }) {
                        Text(
                            dueEpochDay?.let { LocalDate.ofEpochDay(it).formatMedium() } ?: stringResource(R.string.sort_pick_due),
                            color = GIDCyan,
                        )
                    }
                }
                Label(stringResource(R.string.sort_also_create))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choice(stringResource(R.string.sort_also_nothing), selected = alsoCreate == ALSO_NONE) { alsoCreate = ALSO_NONE }
                    if (single?.linkedTaskId == null) {
                        Choice(stringResource(R.string.sort_also_task), selected = alsoCreate == ALSO_TASK) { alsoCreate = ALSO_TASK }
                    }
                    // An exam belongs to a subject, so these need one picked above.
                    if (subjectId != null) {
                        listOf(ExamKind.EXAM, ExamKind.ASSIGNMENT).forEach { k ->
                            Choice(k.label(), selected = alsoCreate == k.name) { alsoCreate = k.name }
                        }
                    }
                }
                if (subjectId == null) {
                    Text(stringResource(R.string.sort_exam_needs_subject), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
                }
            }

            if (single != null) {
                GIDTextField(value = title, onValueChange = { title = it }, label = stringResource(R.string.sort_title_field))
                GIDTextField(value = note, onValueChange = { note = it }, label = stringResource(R.string.sort_note_field), singleLine = false)
            }

            val chosenType = type
            Button(
                enabled  = chosenType != null,
                onClick  = {
                    if (chosenType == null) return@Button
                    viewModel.sort(
                        ids    = captures.map { it.id },
                        choice = SortChoice(
                            type        = chosenType,
                            subjectId   = subjectId,
                            headingId   = headingId,
                            title       = if (single != null) title.trim() else null,
                            note        = if (single != null) note.trim() else null,
                            dueEpochDay = if (chosenType == CaptureType.DEADLINE.name) dueEpochDay else null,
                            createTask  = alsoCreate == ALSO_TASK,
                            createExam  = ExamKind.entries.firstOrNull { it.name == alsoCreate }?.takeIf { subjectId != null },
                        ),
                    ) { onSorted() }
                    onDismiss()
                },
                shape    = CircleShape,
                colors   = ButtonDefaults.buttonColors(containerColor = GIDCyan, contentColor = OnCard),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(if (chosenType != null) stringResource(R.string.action_save) else stringResource(R.string.sort_choose_type))
            }
        }
    }

    if (newCategoryOpen) {
        NameDialog(stringResource(R.string.category_new), onDismiss = { newCategoryOpen = false }) { name ->
            scope.launch { type = viewModel.createCategory(name).id }
        }
    }
    if (newSubjectOpen) {
        NameDialog(stringResource(R.string.sort_new_subject), onDismiss = { newSubjectOpen = false }) { name ->
            scope.launch { subjectId = viewModel.createSubject(name).id; headingId = null }
        }
    }
    if (newHeadingOpen) {
        val sid = subjectId
        NameDialog(stringResource(R.string.sort_new_heading), onDismiss = { newHeadingOpen = false }) { name ->
            if (sid != null) scope.launch { headingId = viewModel.createHeading(sid, name).id }
        }
    }
    if (datePickerOpen) {
        GIDDatePickerDialog(
            initial   = dueEpochDay?.let { LocalDate.ofEpochDay(it) } ?: today(),
            onDismiss = { datePickerOpen = false },
            onPicked  = { dueEpochDay = it.toEpochDay(); datePickerOpen = false },
        )
    }
}

private const val ALSO_NONE = "NONE"
private const val ALSO_TASK = "TASK"

@Composable
internal fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = GIDMutedText)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Choice(text: String, selected: Boolean, selectedColor: Color = GIDCyan, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick  = onClick,
        label    = { Text(text) },
        shape    = CircleShape,
        colors   = FilterChipDefaults.filterChipColors(
            containerColor         = GIDSurfaceHigh,
            labelColor             = GIDText,
            selectedContainerColor = selectedColor,
            selectedLabelColor     = OnCard,
        ),
        border   = null,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddChip(text: String, onClick: () -> Unit) {
    FilterChip(
        selected    = false,
        onClick     = onClick,
        label       = { Text(text) },
        leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
        shape       = CircleShape,
        colors      = FilterChipDefaults.filterChipColors(
            containerColor   = GIDSurface,
            labelColor       = GIDCyan,
            iconColor        = GIDCyan,
        ),
        border      = FilterChipDefaults.filterChipBorder(enabled = true, selected = false, borderColor = GIDCyan),
    )
}

/** A one-field dialog for naming a new subject or heading (also used to rename). */
@Composable
fun NameDialog(title: String, initial: String = "", onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    GIDDialog(
        title     = title,
        onDismiss = onDismiss,
        actions   = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim()); onDismiss() }) {
                Text(stringResource(R.string.action_save), color = GIDCyan)
            }
        },
    ) {
        GIDTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.sort_name_field))
    }
}
