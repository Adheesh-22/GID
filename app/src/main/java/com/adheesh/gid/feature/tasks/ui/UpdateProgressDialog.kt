package com.adheesh.gid.feature.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.database.Priority
import com.adheesh.gid.core.designsystem.Dot
import com.adheesh.gid.core.designsystem.EmptyText
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDDialog
import com.adheesh.gid.core.designsystem.GIDHolidayGreen
import com.adheesh.gid.core.designsystem.GIDSurfaceHigh
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.designsystem.color
import com.adheesh.gid.feature.tasks.viewmodel.ProgressViewModel
import kotlin.math.roundToInt

/** Opened from the 21:00 notification: one slider per task of today. */
@Composable
fun UpdateProgressDialog(viewModel: ProgressViewModel, onDismiss: () -> Unit) {
    val ui by viewModel.ui.collectAsState()
    LaunchedEffect(Unit) { viewModel.reload() }

    GIDDialog(title = stringResource(R.string.progress_title), onDismiss = onDismiss) {
        Column(
            Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (ui.loaded && ui.tasks.isEmpty()) EmptyText(stringResource(R.string.progress_empty))
            ui.tasks.forEach { task ->
                val percent = ui.percents[task.id] ?: task.progressPercent
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(Priority.of(task.priority).color(), size = 10.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        task.title,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = GIDText,
                        modifier = Modifier.weight(1f),
                    )
                    Text(stringResource(R.string.percent_value, percent), style = MaterialTheme.typography.labelMedium, color = GIDText)
                }
                Slider(
                    value         = percent.toFloat(),
                    onValueChange = { viewModel.setPercent(task.id, it.roundToInt()) },
                    valueRange    = 0f..100f,
                    steps         = 19,
                    colors        = SliderDefaults.colors(thumbColor = GIDCoral, activeTrackColor = GIDCoral, inactiveTrackColor = GIDSurfaceHigh),
                )
            }
            ui.movedCount?.let {
                Text(pluralStringResource(R.plurals.progress_moved, it, it), style = MaterialTheme.typography.bodySmall, color = GIDHolidayGreen)
            }
            if (ui.tasks.isNotEmpty()) {
                OutlinedButton(
                    onClick  = viewModel::moveUnfinished,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = MaterialTheme.shapes.large,
                ) { Text(stringResource(R.string.progress_move_unfinished), color = GIDCyan) }
            }
            Button(
                onClick  = { viewModel.save(onDismiss) },
                modifier = Modifier.fillMaxWidth(),
                shape    = MaterialTheme.shapes.large,
                colors   = ButtonDefaults.buttonColors(containerColor = GIDCyan, contentColor = OnCard),
            ) { Text(stringResource(R.string.action_save)) }
        }
    }
}
