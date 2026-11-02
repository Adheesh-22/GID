package com.adheesh.gid.feature.exams.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.database.ExamEntity
import com.adheesh.gid.core.database.ExamKind
import com.adheesh.gid.core.database.SubjectEntity
import com.adheesh.gid.core.designsystem.Dot
import com.adheesh.gid.core.designsystem.GIDCard
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurfaceHigh
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.util.formatMinutesOfDay
import com.adheesh.gid.core.util.formatShort
import com.adheesh.gid.core.util.today
import com.adheesh.gid.feature.exams.domain.Countdown
import com.adheesh.gid.feature.exams.domain.Urgency
import com.adheesh.gid.feature.library.ui.SmallChip
import com.adheesh.gid.feature.library.ui.color
import java.time.LocalDate

@Composable
fun ExamKind.label(): String = stringResource(
    when (this) {
        ExamKind.EXAM       -> R.string.exam_kind_exam
        ExamKind.TEST       -> R.string.exam_kind_test
        ExamKind.ASSIGNMENT -> R.string.exam_kind_assignment
        ExamKind.PROJECT    -> R.string.exam_kind_project
    }
)

/** The title, or the kind ("Exam") when the user left it blank. */
@Composable
fun ExamEntity.displayTitle(): String = title.ifBlank { ExamKind.of(kind).label() }

@Composable
fun Countdown.text(): String = when (unit) {
    Countdown.Unit.PAST     -> pluralStringResource(R.plurals.countdown_past, value.toInt(), value.toInt())
    Countdown.Unit.TODAY    -> stringResource(R.string.countdown_today)
    Countdown.Unit.TOMORROW -> stringResource(R.string.countdown_tomorrow)
    Countdown.Unit.DAYS     -> pluralStringResource(R.plurals.countdown_days, value.toInt(), value.toInt())
    Countdown.Unit.WEEKS    -> pluralStringResource(R.plurals.countdown_weeks, value.toInt(), value.toInt())
}

/** Coral for today/tomorrow, cyan within a week, quiet otherwise. */
fun Urgency.container(): Color = when (this) {
    Urgency.NOW   -> GIDCoral
    Urgency.SOON  -> GIDCyan
    else          -> GIDSurfaceHigh
}

fun Urgency.content(): Color = if (this == Urgency.NOW || this == Urgency.SOON) OnCard else GIDText

fun ExamEntity.countdown(): Countdown = Countdown.of(dateEpochDay, today().toEpochDay())

/** "Mon 10 Nov · 10:00 · Room 204" */
fun ExamEntity.whenText(): String =
    listOfNotNull(LocalDate.ofEpochDay(dateEpochDay).formatShort(), timeMinutes?.let { formatMinutesOfDay(it) }, room)
        .joinToString(" · ")

@Composable
fun CountdownChip(countdown: Countdown) {
    Text(
        countdown.text(),
        style    = MaterialTheme.typography.labelLarge,
        color    = countdown.urgency.content(),
        modifier = Modifier.clip(CircleShape).background(countdown.urgency.container()).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** One exam or assignment in a list: subject, title, kind, when, and the countdown. */
@Composable
fun ExamRow(exam: ExamEntity, subject: SubjectEntity?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val done = exam.completedAt != null
    GIDCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (subject != null) {
                        Dot(subject.color(), size = 10.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(subject.name, style = MaterialTheme.typography.bodyMedium, color = GIDMutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text(
                    exam.displayTitle(),
                    style          = MaterialTheme.typography.titleMedium,
                    color          = GIDText,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    maxLines       = 2,
                    overflow       = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SmallChip(ExamKind.of(exam.kind).label(), GIDSurfaceHigh, GIDText)
                    Text(exam.whenText(), style = MaterialTheme.typography.bodySmall, color = GIDMutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (!done) {
                Spacer(Modifier.width(8.dp))
                CountdownChip(exam.countdown())
            }
        }
    }
}
