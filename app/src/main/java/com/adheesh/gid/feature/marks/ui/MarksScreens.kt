package com.adheesh.gid.feature.marks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.database.MarkEntryEntity
import com.adheesh.gid.core.designsystem.EmptyText
import com.adheesh.gid.core.designsystem.GIDCard
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.GIDTextField
import com.adheesh.gid.core.designsystem.ScreenTitle
import com.adheesh.gid.feature.marks.data.MarkPart
import com.adheesh.gid.feature.marks.data.SubjectMarkCard
import com.adheesh.gid.feature.marks.domain.VtopMarks
import com.adheesh.gid.feature.marks.domain.VtopMarksParser
import com.adheesh.gid.feature.marks.viewmodel.MarksDetailViewModel
import com.adheesh.gid.feature.marks.viewmodel.MarksViewModel
import com.adheesh.gid.feature.semesters.ui.SemesterChip
import com.adheesh.gid.feature.semesters.ui.VtopPasteSheet
import java.util.Locale
import kotlin.math.roundToInt

/** Marks read like VTOP's: 12.3, 14, 11.15. */
private fun Double.marks(): String = String.format(Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')

// ── Marks page (SPEC §2.15) ──────────────────────────────────────────────────
@Composable
fun MarksScreen(
    viewModel     : MarksViewModel,
    onBack        : () -> Unit,
    onOpenSubject : (String) -> Unit,
    modifier      : Modifier = Modifier,
) {
    val cards by viewModel.cards.collectAsState()
    var pasteOpen by rememberSaveable { mutableStateOf(false) }

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
                ScreenTitle(stringResource(R.string.marks_title), Modifier.weight(1f))
                SemesterChip()
            }
        }
        item {
            TextButton(onClick = { pasteOpen = true }) {
                Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = GIDCyan)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.marks_paste), color = GIDCyan)
            }
        }
        if (cards.isEmpty()) item { EmptyText(stringResource(R.string.marks_empty)) }
        items(cards, key = { it.subject.id }) { card ->
            GIDCard(Modifier.fillMaxWidth(), onClick = { onOpenSubject(card.subject.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        card.subject.name,
                        style    = MaterialTheme.typography.titleMedium,
                        color    = GIDText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.marks_out_of_100, card.totals.projected.marks()),
                        style = MaterialTheme.typography.titleLarge,
                        color = GIDCyan,
                    )
                }
                Text(sharesText(card), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
            }
        }
    }

    if (pasteOpen) {
        VtopPasteSheet<VtopMarks>(
            title     = stringResource(R.string.marks_paste),
            hint      = stringResource(R.string.marks_paste_hint),
            field     = stringResource(R.string.marks_paste_field),
            notFound  = stringResource(R.string.marks_paste_not_found),
            action    = stringResource(R.string.marks_paste_action),
            parse     = VtopMarksParser::parse,
            preview   = { parsed ->
                parsed.semester?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = GIDText) }
                Text(
                    pluralStringResource(R.plurals.marks_preview_courses, parsed.courses.size, parsed.courses.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = GIDText,
                )
                parsed.courses.forEach { c ->
                    Text(
                        pluralStringResource(R.plurals.marks_preview_course, c.rows.size, c.title, c.type, c.rows.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = GIDMutedText,
                    )
                }
            },
            onImport  = { viewModel.import(it) },
            onDismiss = { pasteOpen = false },
        )
    }
}

/** 'Embedded Theory 75% · Embedded Lab 25%'. */
@Composable
private fun sharesText(card: SubjectMarkCard): String =
    card.parts.map { part ->
        stringResource(R.string.marks_share, part.component.courseType, (part.share * 100).roundToInt())
    }.joinToString(" · ")

// ── One subject: what's uploaded, what's left, what you expect ───────────────
@Composable
fun MarksDetailScreen(viewModel: MarksDetailViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val card by viewModel.card.collectAsState()
    val current = card ?: return

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
                Text(
                    current.subject.name,
                    style    = MaterialTheme.typography.titleLarge,
                    color    = GIDText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            GIDCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.marks_total), style = MaterialTheme.typography.labelLarge, color = GIDMutedText)
                Text(
                    stringResource(R.string.marks_out_of_100, current.totals.projected.marks()),
                    style = MaterialTheme.typography.displayMedium,
                    color = GIDCyan,
                )
                Text(
                    stringResource(R.string.marks_uploaded_only, current.totals.uploaded.marks()),
                    style = MaterialTheme.typography.bodySmall,
                    color = GIDMutedText,
                )
                Text(sharesText(current), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
            }
        }
        items(current.parts, key = { it.component.id }) { part ->
            PartCard(
                part       = part,
                onExpected = { viewModel.setExpected(part.component.id, it) },
                onCredits  = { viewModel.setCredits(part.component.id, it) },
            )
        }
    }
}

@Composable
private fun PartCard(part: MarkPart, onExpected: (Double?) -> Unit, onCredits: (Double?) -> Unit) {
    val marks = part.marks
    // Typing stays as typed ('3.', '') while the sums use the number.
    var expected by rememberSaveable(part.component.id) { mutableStateOf(part.component.expected?.marks().orEmpty()) }
    var credits  by rememberSaveable(part.component.id) { mutableStateOf(part.component.credits?.marks().orEmpty()) }
    val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    GIDCard(Modifier.fillMaxWidth()) {
        Text(part.component.courseType, style = MaterialTheme.typography.titleMedium, color = GIDText)
        Text(part.component.courseCode, style = MaterialTheme.typography.bodySmall, color = GIDMutedText)

        Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (part.entries.isEmpty()) {
                Text(stringResource(R.string.marks_none_uploaded), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
            }
            part.entries.forEach { MarkRow(it) }
        }

        LabelledValue(stringResource(R.string.marks_uploaded), stringResource(R.string.marks_of, marks.uploaded.marks(), marks.counted.marks()))
        LabelledValue(stringResource(R.string.marks_left), marks.left.marks())

        if (marks.left > 0) {
            GIDTextField(
                value           = expected,
                onValueChange   = { text ->
                    expected = text.filter { it.isDigit() || it == '.' }.take(6)
                    onExpected(expected.toDoubleOrNull())
                },
                label           = stringResource(R.string.marks_expected, marks.left.marks()),
                keyboardOptions = decimal,
                modifier        = Modifier.padding(vertical = 8.dp),
            )
        }
        LabelledValue(stringResource(R.string.marks_part_total), stringResource(R.string.marks_out_of_100, marks.projected.marks()), strong = true)

        GIDTextField(
            value           = credits,
            onValueChange   = { text ->
                credits = text.filter { it.isDigit() || it == '.' }.take(4)
                onCredits(credits.toDoubleOrNull())
            },
            label           = stringResource(R.string.marks_credits),
            keyboardOptions = decimal,
            modifier        = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun MarkRow(entry: MarkEntryEntity) {
    val earned = entry.weightageMark ?: 0.0
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(entry.title, style = MaterialTheme.typography.bodyMedium, color = GIDText, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.marks_of, earned.marks(), entry.weightage.marks()),
                style = MaterialTheme.typography.bodyMedium,
                color = GIDText,
            )
        }
        val raw = entry.scoredMark?.let { stringResource(R.string.marks_raw, it.marks(), entry.maxMark.marks()) } ?: entry.status
        Text(raw, style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
        if (entry.remark.isNotBlank()) {
            Text(entry.remark, style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
        }
    }
}

@Composable
private fun LabelledValue(label: String, value: String, strong: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style    = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color    = if (strong) GIDText else GIDMutedText,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (strong) GIDCyan else GIDText,
        )
    }
}
