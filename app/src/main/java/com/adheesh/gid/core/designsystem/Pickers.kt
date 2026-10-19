package com.adheesh.gid.core.designsystem

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.adheesh.gid.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// ── Date and time pickers ────────────────────────────────────────────────────
// Material's DatePicker works in UTC milliseconds, so we convert to and from
// LocalDate at UTC to avoid the date shifting by a day in some time zones.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GIDDatePickerDialog(
    initial      : LocalDate?,
    onDismiss    : () -> Unit,
    onPicked     : (LocalDate) -> Unit,
    allowFuture  : Boolean = true,
) {
    val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = allowFuture || utcTimeMillis <= todayUtc
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text(stringResource(R.string.action_ok), color = GIDCyan) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = GIDMutedText) }
        },
        colors = DatePickerDefaults.colors(containerColor = GIDSurface),
    ) {
        DatePicker(
            state  = state,
            colors = DatePickerDefaults.colors(
                containerColor             = GIDSurface,
                selectedDayContainerColor  = GIDCoral,
                selectedDayContentColor    = OnCard,
                todayDateBorderColor       = GIDCyan,
                todayContentColor          = GIDCyan,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GIDTimePickerDialog(
    initialMinutes : Int,
    onDismiss      : () -> Unit,
    onPicked       : (minutesFromMidnight: Int) -> Unit,
) {
    val is24h = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val state = rememberTimePickerState(
        initialHour   = (initialMinutes / 60) % 24,
        initialMinute = initialMinutes % 60,
        is24Hour      = is24h,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = GIDSurface,
        shape            = MaterialTheme.shapes.medium,
        confirmButton = {
            TextButton(onClick = { onPicked(state.hour * 60 + state.minute); onDismiss() }) {
                Text(stringResource(R.string.action_ok), color = GIDCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = GIDMutedText) }
        },
        text = {
            TimePicker(
                state  = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor                 = GIDSurfaceHigh,
                    selectorColor                  = GIDCyan,
                    timeSelectorSelectedContainerColor   = GIDPeriwinkle,
                    timeSelectorSelectedContentColor     = OnCard,
                    timeSelectorUnselectedContainerColor = GIDSurfaceHigh,
                    timeSelectorUnselectedContentColor   = GIDText,
                    periodSelectorSelectedContainerColor = GIDPeriwinkle,
                    periodSelectorSelectedContentColor   = OnCard,
                ),
            )
        },
    )
}
