package com.adheesh.gid.feature.timetable.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.GIDTextField
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.feature.timetable.domain.VtopParser
import com.adheesh.gid.feature.timetable.domain.VtopTimetable

// ── Paste the timetable from VTOP (SPEC §2.7) ────────────────────────────────
// Everything is read as it is pasted, and the preview shows what was understood
// before anything in the current timetable changes.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VtopImportSheet(
    existingClasses : Int,
    onImport        : (VtopTimetable) -> Unit,
    onDismiss       : () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val parsed = remember(text) { VtopParser.parse(text) }
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = GIDSurface,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
        ) {
            Text(stringResource(R.string.vtop_title), style = MaterialTheme.typography.titleLarge, color = GIDText)
            Text(stringResource(R.string.vtop_hint), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)

            OutlinedButton(
                onClick  = {
                    val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
                    clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()?.let { text = it }
                },
                shape    = CircleShape,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.vtop_paste_clipboard), color = GIDCyan) }

            GIDTextField(
                value         = text,
                onValueChange = { text = it },
                label         = stringResource(R.string.vtop_field),
                singleLine    = false,
                modifier      = Modifier.heightIn(min = 120.dp, max = 220.dp),
            )

            if (parsed != null) {
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
                if (existingClasses > 0) {
                    Text(
                        pluralStringResource(R.plurals.vtop_replaces, existingClasses, existingClasses),
                        style = MaterialTheme.typography.bodySmall,
                        color = GIDCoral,
                    )
                }
            } else if (text.isNotBlank()) {
                Text(stringResource(R.string.vtop_not_found), style = MaterialTheme.typography.bodySmall, color = GIDCoral)
            }

            Button(
                enabled  = parsed != null,
                onClick  = {
                    parsed?.let(onImport)
                    onDismiss()
                },
                shape    = CircleShape,
                colors   = ButtonDefaults.buttonColors(containerColor = GIDCyan, contentColor = OnCard),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.vtop_import)) }
        }
    }
}
