package com.adheesh.gid.feature.exams.ui

import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.pluralStringResource
import com.adheesh.gid.core.database.ExamKind
import com.adheesh.gid.feature.marks.domain.VtopAssessmentCourse
import com.adheesh.gid.feature.marks.domain.VtopAssessmentParser
import com.adheesh.gid.feature.semesters.ui.SemesterChip
import com.adheesh.gid.feature.semesters.ui.VtopPasteSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.designsystem.EmptyText
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.designsystem.ScreenTitle
import com.adheesh.gid.core.designsystem.SectionTitle
import com.adheesh.gid.feature.exams.viewmodel.ExamsViewModel

// ── Exams page (SPEC §2.10) ──────────────────────────────────────────────────
// Upcoming exams and assignments, soonest first; done and past ones folded
// away at the bottom; '+' adds one.
@Composable
fun ExamsScreen(
    viewModel  : ExamsViewModel,
    onBack     : () -> Unit,
    onOpenExam : (String) -> Unit,
    modifier   : Modifier = Modifier,
) {
    val upcoming    by viewModel.upcoming.collectAsState()
    val past        by viewModel.past.collectAsState()
    val subjectById by viewModel.subjectById.collectAsState()

    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var showPast   by rememberSaveable { mutableStateOf(false) }
    var pasteOpen  by rememberSaveable { mutableStateOf(false) }
    val assessments = viewModel.assessments

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier            = Modifier.fillMaxSize(),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back), tint = GIDText)
                    }
                    Spacer(Modifier.width(4.dp))
                    ScreenTitle(
                        stringResource(if (assessments) R.string.assessments_title else R.string.exams_title),
                        Modifier.weight(1f),
                    )
                    SemesterChip()
                }
            }

            if (assessments) {
                item {
                    TextButton(onClick = { pasteOpen = true }) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = GIDCyan)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.assessments_paste), color = GIDCyan)
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.exams_upcoming), Modifier.padding(top = 8.dp)) }
            if (upcoming.isEmpty()) item { EmptyText(stringResource(if (assessments) R.string.assessments_empty else R.string.exams_empty)) }
            items(upcoming, key = { it.id }) { exam ->
                ExamRow(exam, subjectById[exam.subjectId], onClick = { onOpenExam(exam.id) })
            }

            if (past.isNotEmpty()) {
                item {
                    TextButton(onClick = { showPast = !showPast }, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.exams_past, past.size), color = GIDMutedText)
                    }
                }
                if (showPast) {
                    items(past, key = { "p-${it.id}" }) { exam ->
                        ExamRow(exam, subjectById[exam.subjectId], onClick = { onOpenExam(exam.id) })
                    }
                }
            }
        }

        FloatingActionButton(
            onClick        = { editorOpen = true },
            containerColor = GIDCyan,
            contentColor   = OnCard,
            modifier       = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Rounded.Add, stringResource(R.string.exam_add)) }
    }

    if (editorOpen) {
        ExamEditorSheet(
            viewModel = viewModel,
            initial   = null,
            onDismiss = { editorOpen = false },
            kinds     = ExamKind.entries.filter { it.isSubmitted == assessments },
        )
    }
    if (pasteOpen) {
        VtopPasteSheet<List<VtopAssessmentCourse>>(
            title     = stringResource(R.string.assessments_paste),
            hint      = stringResource(R.string.assessments_paste_hint),
            field     = stringResource(R.string.assessments_paste_field),
            notFound  = stringResource(R.string.assessments_paste_not_found),
            action    = stringResource(R.string.assessments_paste_action),
            parse     = VtopAssessmentParser::parse,
            preview   = { courses ->
                courses.forEach { course ->
                    Text(
                        pluralStringResource(R.plurals.assessments_preview, course.items.size, course.title, course.items.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = GIDText,
                    )
                }
            },
            onImport  = viewModel::importAssessments,
            onDismiss = { pasteOpen = false },
        )
    }
}
