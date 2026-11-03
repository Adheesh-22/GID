package com.adheesh.gid.feature.syllabus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.database.SyllabusTopicEntity
import com.adheesh.gid.core.designsystem.GIDCard
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDHolidayGreen
import com.adheesh.gid.core.designsystem.GIDMustard
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurfaceHigh
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.feature.syllabus.domain.SyllabusLevel
import com.adheesh.gid.feature.syllabus.domain.SyllabusProgress

// ── Shared syllabus pieces (SPEC §2.13) ──────────────────────────────────────

fun SyllabusLevel.color(): Color = when (this) {
    SyllabusLevel.NONE     -> GIDMutedText
    SyllabusLevel.ON_TRACK -> GIDHolidayGreen
    SyllabusLevel.AT_RISK  -> GIDMustard
    SyllabusLevel.BEHIND   -> GIDCoral
}

@Composable
fun SyllabusBar(progress: SyllabusProgress, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(GIDSurfaceHigh),
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth((progress.percent ?: 0) / 100f).clip(CircleShape).background(color))
    }
}

/** '12 of 30 topics · 40%'. */
@Composable
fun SyllabusProgress.text(): String =
    if (total == 0) stringResource(R.string.syllabus_none)
    else pluralStringResource(R.plurals.syllabus_progress, total, done, total, percent ?: 0)

/** The 'Syllabus' card on subject and exam pages. */
@Composable
fun SyllabusCard(
    progress : SyllabusProgress,
    level    : SyllabusLevel,
    onClick  : () -> Unit,
    modifier : Modifier = Modifier,
    content  : @Composable () -> Unit = {},
) {
    GIDCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.syllabus_title), style = MaterialTheme.typography.titleMedium, color = GIDText, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.syllabus_open), style = MaterialTheme.typography.labelLarge, color = GIDCyan)
        }
        Text(progress.text(), style = MaterialTheme.typography.bodyMedium, color = GIDMutedText, modifier = Modifier.padding(vertical = 6.dp))
        if (progress.total > 0) SyllabusBar(progress, level.color())
        content()
    }
}

/** A topic with its checkbox; done topics are struck through. */
@Composable
fun TopicRow(
    topic    : SyllabusTopicEntity,
    onDone   : (Boolean) -> Unit,
    modifier : Modifier = Modifier,
    trailing : @Composable () -> Unit = {},
) {
    val done = topic.doneAt != null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().clickable { onDone(!done) },
    ) {
        Checkbox(
            checked         = done,
            onCheckedChange = onDone,
            colors          = CheckboxDefaults.colors(checkedColor = GIDHolidayGreen, uncheckedColor = GIDMutedText, checkmarkColor = OnCard),
        )
        Text(
            topic.name,
            style          = MaterialTheme.typography.bodyMedium,
            color          = if (done) GIDMutedText else GIDText,
            textDecoration = if (done) TextDecoration.LineThrough else null,
            modifier       = Modifier.weight(1f),
        )
        trailing()
    }
}
