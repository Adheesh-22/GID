package com.adheesh.gid.feature.attendance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.database.AttendanceStatus
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDHolidayGreen
import com.adheesh.gid.core.designsystem.GIDMustard
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDSurfaceHigh
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.feature.attendance.domain.AttendanceLevel
import com.adheesh.gid.feature.attendance.domain.AttendanceSummary

// ── Shared attendance pieces (SPEC §2.12) ────────────────────────────────────

/** Coral below the target, mustard just above it, green when safe. */
fun AttendanceLevel.color(): Color = when (this) {
    AttendanceLevel.NONE  -> GIDMutedText
    AttendanceLevel.BELOW -> GIDCoral
    AttendanceLevel.CLOSE -> GIDMustard
    AttendanceLevel.SAFE  -> GIDHolidayGreen
}

/** 'You can miss 2 more' or 'Attend the next 3 to reach 75%'. */
@Composable
fun AttendanceSummary.advice(): String = when {
    percent == null    -> stringResource(R.string.attendance_no_data)
    mustAttend == null -> stringResource(R.string.attendance_unreachable, target)
    mustAttend > 0     -> pluralStringResource(R.plurals.attendance_must_attend, mustAttend, mustAttend, target)
    canMiss > 0        -> pluralStringResource(R.plurals.attendance_can_miss, canMiss.coerceAtMost(999), canMiss.coerceAtMost(999))
    else               -> stringResource(R.string.attendance_at_limit)
}

/** A bar filled to the percent, with a thin line at the target. */
@Composable
fun AttendanceBar(summary: AttendanceSummary, modifier: Modifier = Modifier) {
    val fraction = (summary.percent ?: 0) / 100f
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(GIDSurfaceHigh),
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(CircleShape).background(summary.level.color()))
        Box(
            Modifier
                .offset(x = maxWidth * (summary.target / 100f) - 1.dp)
                .width(2.dp)
                .fillMaxHeight()
                .background(GIDText),
        )
    }
}

/** Present / Absent / Cancelled; tapping the chosen one again clears it. */
@Composable
fun MarkButtons(
    subject  : String,
    current  : AttendanceStatus?,
    onMark   : (AttendanceStatus) -> Unit,
    modifier : Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MarkButton(Icons.Rounded.Check, stringResource(R.string.cd_mark_present, subject), GIDHolidayGreen, current == AttendanceStatus.PRESENT) {
            onMark(AttendanceStatus.PRESENT)
        }
        MarkButton(Icons.Rounded.Close, stringResource(R.string.cd_mark_absent, subject), GIDCoral, current == AttendanceStatus.ABSENT) {
            onMark(AttendanceStatus.ABSENT)
        }
        MarkButton(Icons.Rounded.Block, stringResource(R.string.cd_mark_cancelled, subject), GIDMutedText, current == AttendanceStatus.CANCELLED) {
            onMark(AttendanceStatus.CANCELLED)
        }
    }
}

@Composable
private fun MarkButton(icon: ImageVector, description: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    FilledIconButton(
        onClick  = onClick,
        colors   = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (selected) color else GIDSurfaceHigh,
            contentColor   = if (selected) OnCard else color,
        ),
        modifier = Modifier.size(36.dp),
    ) { Icon(icon, contentDescription = description, modifier = Modifier.size(18.dp)) }
}
