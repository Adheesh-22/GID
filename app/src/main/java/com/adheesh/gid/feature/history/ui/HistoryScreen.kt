package com.adheesh.gid.feature.history.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.designsystem.EmptyText
import com.adheesh.gid.core.designsystem.GIDBigNumber
import com.adheesh.gid.core.designsystem.GIDCard
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDDialog
import com.adheesh.gid.core.designsystem.GIDMustard
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDPeriwinkle
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.designsystem.ScreenTitle
import com.adheesh.gid.core.designsystem.SectionTitle
import com.adheesh.gid.core.util.formatDuration
import com.adheesh.gid.core.util.formatMedium
import com.adheesh.gid.core.util.formatShort
import com.adheesh.gid.core.util.formatTimeOfMillis
import com.adheesh.gid.feature.history.domain.DaySummary
import com.adheesh.gid.feature.history.domain.WeekSummary
import com.adheesh.gid.feature.history.viewmodel.HistoryViewModel
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// ── History (SPEC §2.4) ──────────────────────────────────────────────────────
@Composable
fun HistoryScreen(viewModel: HistoryViewModel, modifier: Modifier = Modifier) {
    val weekStart by viewModel.weekStart.collectAsState()
    val summary   by viewModel.summary.collectAsState()
    val typeNames by viewModel.typeNames.collectAsState()
    val isCurrent by viewModel.isCurrentWeek.collectAsState()

    var productivityDay by rememberSaveable { mutableStateOf<Int?>(null) }
    var sleepDay        by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle(stringResource(R.string.history_title))

        // ── Week selector ────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = viewModel::previousWeek) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.cd_previous_week), tint = GIDText)
            }
            Text(
                stringResource(R.string.history_week_range, weekStart.formatShort(), weekStart.plusDays(6).formatShort()),
                style     = MaterialTheme.typography.titleMedium,
                color     = GIDText,
                textAlign = TextAlign.Center,
                modifier  = Modifier.weight(1f),
            )
            IconButton(onClick = viewModel::nextWeek, enabled = !isCurrent) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    stringResource(R.string.cd_next_week),
                    tint = if (isCurrent) GIDMutedText.copy(alpha = 0.4f) else GIDText,
                )
            }
        }

        val week = summary
        if (week == null) {
            CircularProgressIndicator(color = GIDCyan, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            ProductivityCard(week, onBar = { productivityDay = it })
            SleepCard(week, onBar = { sleepDay = it })
            WeeklyScoreCard(week)
        }
        Spacer(Modifier.height(96.dp))
    }

    val week = summary
    if (week != null) {
        productivityDay?.let { i -> DayDetailDialog(week.days[i], typeNames) { productivityDay = null } }
        sleepDay?.let { i -> SleepDetailDialog(week.days[i]) { sleepDay = null } }
    }
}

@Composable
private fun ProductivityCard(week: WeekSummary, onBar: (Int) -> Unit) {
    GIDCard(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.history_productivity))
        Spacer(Modifier.height(8.dp))
        BarChart(
            values    = week.days.map { d -> d.score?.let { (it * 100).toFloat() } },
            maxValue  = 100f,
            color     = GIDCoral,
            labels    = week.days.map { it.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()) },
            valueText = { it.roundToInt().toString() },
            onBar     = onBar,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.history_focus_week, formatDuration(week.focusMinutes)),
            style = MaterialTheme.typography.bodyMedium,
            color = GIDText,
        )
    }
}

@Composable
private fun SleepCard(week: WeekSummary, onBar: (Int) -> Unit) {
    val hours = week.days.map { d -> if (d.hasSleepData) d.sleep.totalMinutes / 60f else null }
    GIDCard(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.history_sleep))
        Spacer(Modifier.height(8.dp))
        BarChart(
            values    = hours,
            // Keep a 10-hour scale so a short night looks short.
            maxValue  = maxOf(10f, hours.filterNotNull().maxOrNull() ?: 0f),
            color     = GIDPeriwinkle,
            labels    = week.days.map { it.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()) },
            valueText = { "%.1f".format(it) },
            onBar     = onBar,
        )
    }
}

@Composable
private fun WeeklyScoreCard(week: WeekSummary) {
    GIDCard(Modifier.fillMaxWidth(), color = GIDMustard) {
        Text(stringResource(R.string.history_weekly_score), style = MaterialTheme.typography.titleLarge, color = OnCard)
        Text(
            week.weekScore?.let { (it * 100).roundToInt().toString() } ?: stringResource(R.string.history_no_data),
            style = GIDBigNumber,
            color = OnCard,
        )
        val change = week.change
        Text(
            when {
                week.previousWeekScore == null -> stringResource(R.string.history_no_data_last_week)
                change == null                 -> stringResource(R.string.history_no_data)
                else -> {
                    val pts = change.points.roundToInt()
                    val sign = if (pts >= 0) "+" else "−"
                    val pct = change.percentChange?.let { p -> " (${if (p >= 0) "+" else "−"}${abs(p).roundToInt()}%)" }.orEmpty()
                    stringResource(R.string.history_change, "$sign${abs(pts)}", pct)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = OnCard,
        )
        Spacer(Modifier.height(8.dp))
        StatLine(stringResource(R.string.history_tasks_completed), stringResource(R.string.history_tasks_ratio, week.tasksCompleted, week.tasksRegistered))
        StatLine(
            stringResource(R.string.history_avg_percent),
            week.averagePercent?.let { stringResource(R.string.percent_value, it.roundToInt()) } ?: stringResource(R.string.history_no_data),
        )
        StatLine(stringResource(R.string.history_focus_hours), "%.1f".format(week.focusMinutes / 60f))
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = OnCard, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = OnCard)
    }
}

/**
 * Seven tappable bars drawn with Canvas. A null value means no data: we show
 * 'no data' under an empty column instead of a zero-height bar.
 */
@Composable
private fun BarChart(
    values    : List<Float?>,
    maxValue  : Float,
    color     : Color,
    labels    : List<String>,
    valueText : (Float) -> String,
    onBar     : (Int) -> Unit,
) {
    val noData = stringResource(R.string.history_no_data_short)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        values.forEachIndexed { index, value ->
            val description = "${labels[index]}: ${value?.let(valueText) ?: noData}"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onBar(index) }
                    .semantics { contentDescription = description },
            ) {
                Text(
                    value?.let(valueText) ?: noData,
                    style    = MaterialTheme.typography.labelMedium,
                    color    = if (value == null) GIDMutedText else GIDText,
                    maxLines = 1,
                )
                Canvas(Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 4.dp, vertical = 4.dp)) {
                    val track = CornerRadius(12f, 12f)
                    drawRoundRect(color = GIDMutedText.copy(alpha = 0.12f), size = size, cornerRadius = track)
                    if (value != null && maxValue > 0f) {
                        val h = (value / maxValue).coerceIn(0f, 1f) * size.height
                        // Draw a sliver even for 0 so a real zero differs from 'no data'.
                        val barHeight = maxOf(h, 4f)
                        drawRoundRect(
                            color        = color,
                            topLeft      = Offset(0f, size.height - barHeight),
                            size         = Size(size.width, barHeight),
                            cornerRadius = track,
                        )
                    }
                }
                Text(labels[index], style = MaterialTheme.typography.labelMedium, color = GIDMutedText)
            }
        }
    }
}

// ── Dialogs (small, centered, with a back arrow) ─────────────────────────────
@Composable
private fun DayDetailDialog(day: DaySummary, typeNames: Map<String, String>, onDismiss: () -> Unit) {
    GIDDialog(title = day.date.formatMedium(), onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.history_day_score, day.score?.let { (it * 100).roundToInt().toString() } ?: stringResource(R.string.history_no_data)),
                style = MaterialTheme.typography.titleMedium,
                color = GIDText,
            )
            Text(stringResource(R.string.history_tasks), style = MaterialTheme.typography.labelMedium, color = GIDMutedText)
            if (day.tasks.isEmpty()) EmptyText(stringResource(R.string.history_no_tasks))
            day.tasks.forEach { t ->
                Row(Modifier.fillMaxWidth()) {
                    Text(t.title, style = MaterialTheme.typography.bodyMedium, color = GIDText, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.percent_value, t.progressPercent), style = MaterialTheme.typography.bodyMedium, color = GIDText)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.history_focus_by_type), style = MaterialTheme.typography.labelMedium, color = GIDMutedText)
            if (day.focusByType.isEmpty()) EmptyText(stringResource(R.string.history_no_focus))
            day.focusByType.forEach { (typeId, minutes) ->
                Row(Modifier.fillMaxWidth()) {
                    Text(typeNames[typeId] ?: stringResource(R.string.history_unknown_type), style = MaterialTheme.typography.bodyMedium, color = GIDText, modifier = Modifier.weight(1f))
                    Text(formatDuration(minutes), style = MaterialTheme.typography.bodyMedium, color = GIDText)
                }
            }
        }
    }
}

@Composable
private fun SleepDetailDialog(day: DaySummary, onDismiss: () -> Unit) {
    GIDDialog(title = day.date.formatMedium(), onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StatRow(stringResource(R.string.history_nap_time), formatDuration(day.sleep.napMinutes))
            StatRow(stringResource(R.string.history_long_sleep), formatDuration(day.sleep.longSleepMinutes))
            StatRow(stringResource(R.string.history_total_sleep), formatDuration(day.sleep.totalMinutes))
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.history_sessions), style = MaterialTheme.typography.labelMedium, color = GIDMutedText)
            if (day.sleepSessions.isEmpty()) EmptyText(stringResource(R.string.history_no_sleep))
            day.sleepSessions.forEach { s ->
                val label = stringResource(if (s.category == "NAP") R.string.category_nap else R.string.category_sleep)
                val range = "${formatTimeOfMillis(s.startAt)} – ${s.endAt?.let(::formatTimeOfMillis).orEmpty()}"
                Row(Modifier.fillMaxWidth()) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = GIDText, modifier = Modifier.weight(1f))
                    Text(range, style = MaterialTheme.typography.bodyMedium, color = GIDText)
                }
                if (s.flagged) Text(stringResource(R.string.history_auto_closed), style = MaterialTheme.typography.bodySmall, color = GIDMustard)
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = GIDText, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = GIDText)
    }
}
