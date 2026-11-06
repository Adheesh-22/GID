package com.adheesh.gid.feature.settings.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.designsystem.GIDCard
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.ScreenTitle

private data class HelpSection(@param:StringRes val title: Int, @param:StringRes val body: Int)

private val SECTIONS = listOf(
    HelpSection(R.string.help_home_title, R.string.help_home_body),
    HelpSection(R.string.help_capture_title, R.string.help_capture_body),
    HelpSection(R.string.help_library_title, R.string.help_library_body),
    HelpSection(R.string.help_tasks_title, R.string.help_tasks_body),
    HelpSection(R.string.help_timetable_title, R.string.help_timetable_body),
    HelpSection(R.string.help_attendance_title, R.string.help_attendance_body),
    HelpSection(R.string.help_semesters_title, R.string.help_semesters_body),
    HelpSection(R.string.help_exams_title, R.string.help_exams_body),
    HelpSection(R.string.help_marks_title, R.string.help_marks_body),
    HelpSection(R.string.help_syllabus_title, R.string.help_syllabus_body),
    HelpSection(R.string.help_alarms_title, R.string.help_alarms_body),
    HelpSection(R.string.help_history_title, R.string.help_history_body),
    HelpSection(R.string.help_look_title, R.string.help_look_body),
    HelpSection(R.string.help_backup_title, R.string.help_backup_body),
    HelpSection(R.string.help_reliability_title, R.string.help_reliability_body),
)

// ── How to use GID (SPEC §2.5a) ──────────────────────────────────────────────
// One short section per feature, folded so the list stays easy to scan.
@Composable
fun HelpScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
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
                ScreenTitle(stringResource(R.string.help_title))
            }
        }
        item { Text(stringResource(R.string.help_intro), style = MaterialTheme.typography.bodyMedium, color = GIDMutedText) }
        items(SECTIONS, key = { it.title }) { section -> SectionCard(section) }
    }
}

@Composable
private fun SectionCard(section: HelpSection) {
    var open by rememberSaveable { mutableStateOf(false) }
    GIDCard(Modifier.fillMaxWidth(), onClick = { open = !open }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(section.title), style = MaterialTheme.typography.titleMedium, color = GIDText, modifier = Modifier.weight(1f))
            Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null, tint = GIDMutedText)
        }
        if (open) {
            Text(
                stringResource(section.body),
                style    = MaterialTheme.typography.bodyMedium,
                color    = GIDText,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
