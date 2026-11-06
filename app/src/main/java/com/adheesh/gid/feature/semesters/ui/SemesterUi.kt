package com.adheesh.gid.feature.semesters.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.database.SemesterEntity
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDHolidayGreen
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.GIDTextField
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.feature.library.ui.NameDialog
import com.adheesh.gid.feature.semesters.data.SemesterNames

/** What the chip shows, provided once by the nav host so any screen can place one. */
class SemesterChipState(val label: String, val onClick: () -> Unit)

val LocalSemesterChip = compositionLocalOf<SemesterChipState?> { null }

// ── The semester in view (SPEC §2.14) ────────────────────────────────────────
/** Nothing until a semester exists. Has its own surface, so it reads on any wallpaper. */
@Composable
fun SemesterChip(modifier: Modifier = Modifier) {
    val state = LocalSemesterChip.current ?: return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(GIDSurface)
            .clickable(onClick = state.onClick)
            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Text(state.label, style = MaterialTheme.typography.labelLarge, color = GIDText)
        Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = GIDMutedText, modifier = Modifier.size(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterSheet(
    semesters : List<SemesterEntity>,
    viewedId  : String?,
    runningId : String?,
    onView    : (String?) -> Unit,
    onRunning : (String) -> Unit,
    onAdd     : (String) -> Unit,
    onDismiss : () -> Unit,
) {
    var addOpen by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = GIDSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
        ) {
            Text(stringResource(R.string.semester_title), style = MaterialTheme.typography.titleLarge, color = GIDText)
            Text(stringResource(R.string.semester_hint), style = MaterialTheme.typography.bodySmall, color = GIDMutedText, modifier = Modifier.padding(vertical = 8.dp))

            semesters.forEach { semester ->
                val running = semester.id == runningId
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { onView(semester.id) },
                ) {
                    RadioButton(
                        selected = viewedId == semester.id,
                        onClick  = { onView(semester.id) },
                        colors   = RadioButtonDefaults.colors(selectedColor = GIDCyan),
                    )
                    Text(SemesterNames.short(semester.name), style = MaterialTheme.typography.bodyLarge, color = GIDText, modifier = Modifier.weight(1f))
                    if (running) {
                        Text(stringResource(R.string.semester_running), style = MaterialTheme.typography.labelMedium, color = GIDHolidayGreen)
                    } else {
                        TextButton(onClick = { onRunning(semester.id) }) {
                            Text(stringResource(R.string.semester_set_running), color = GIDMutedText)
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onView(null) },
            ) {
                RadioButton(
                    selected = viewedId == null,
                    onClick  = { onView(null) },
                    colors   = RadioButtonDefaults.colors(selectedColor = GIDCyan),
                )
                Text(stringResource(R.string.semester_all), style = MaterialTheme.typography.bodyLarge, color = GIDText)
            }
            TextButton(onClick = { addOpen = true }) { Text(stringResource(R.string.semester_add), color = GIDCyan) }
        }
    }

    if (addOpen) NameDialog(stringResource(R.string.semester_add), onDismiss = { addOpen = false }, onSave = onAdd)
}

// ── Paste a page from VTOP ───────────────────────────────────────────────────
/** Shows what was understood before anything changes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> VtopPasteSheet(
    title     : String,
    hint      : String,
    field     : String,
    notFound  : String,
    action    : String,
    parse     : (String) -> T?,
    preview   : @Composable ColumnScope.(T) -> Unit,
    onImport  : (T) -> Unit,
    onDismiss : () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val parsed = remember(text) { parse(text) }
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
            Text(title, style = MaterialTheme.typography.titleLarge, color = GIDText)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = GIDMutedText)
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
                label         = field,
                singleLine    = false,
                modifier      = Modifier.heightIn(min = 120.dp, max = 220.dp),
            )

            if (parsed != null) {
                preview(parsed)
            } else if (text.isNotBlank()) {
                Text(notFound, style = MaterialTheme.typography.bodySmall, color = GIDCoral)
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
            ) { Text(action) }
        }
    }
}
