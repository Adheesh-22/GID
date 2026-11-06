package com.adheesh.gid.feature.timetable.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.adheesh.gid.R
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.feature.semesters.ui.VtopPasteSheet
import com.adheesh.gid.feature.timetable.domain.VtopParser
import com.adheesh.gid.feature.timetable.domain.VtopTimetable

// ── Paste the timetable from VTOP (SPEC §2.7) ────────────────────────────────
// Everything is read as it is pasted, and the preview shows what was understood
// before anything changes. The page names its semester, so it lands there.
@Composable
fun VtopImportSheet(
    runningByDefault : Boolean,
    onImport         : (VtopTimetable, running: Boolean) -> Unit,
    onDismiss        : () -> Unit,
) {
    var running by rememberSaveable { mutableStateOf(runningByDefault) }

    VtopPasteSheet<VtopTimetable>(
        title     = stringResource(R.string.vtop_title),
        hint      = stringResource(R.string.vtop_hint),
        field     = stringResource(R.string.vtop_field),
        notFound  = stringResource(R.string.vtop_not_found),
        action    = stringResource(R.string.vtop_import),
        parse     = VtopParser::parse,
        preview   = { parsed ->
            parsed.semester?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = GIDText) }
            val courses = parsed.classes.map { it.courseCode }.toSet().size
            Text(
                pluralStringResource(R.plurals.vtop_preview_classes, parsed.classes.size, parsed.classes.size) + " · " +
                    pluralStringResource(R.plurals.vtop_preview_courses, courses, courses),
                style = MaterialTheme.typography.labelLarge,
                color = GIDText,
            )
            parsed.classes.groupBy { it.dayOfWeek }.forEach { (day, classes) ->
                Text(
                    "${dayName(day)}: " + classes.joinToString(", ") { it.courseName },
                    style = MaterialTheme.typography.bodySmall,
                    color = GIDMutedText,
                )
            }
            if (parsed.withoutClasses.isNotEmpty()) {
                Text(
                    stringResource(R.string.vtop_no_slot, parsed.withoutClasses.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = GIDMutedText,
                )
            }
            Text(stringResource(R.string.vtop_replaces), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.vtop_running), color = GIDText)
                    Text(stringResource(R.string.vtop_running_hint), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
                }
                Switch(
                    checked         = running,
                    onCheckedChange = { running = it },
                    colors          = SwitchDefaults.colors(checkedTrackColor = GIDCyan, checkedThumbColor = OnCard),
                )
            }
        },
        onImport  = { onImport(it, running) },
        onDismiss = onDismiss,
    )
}
