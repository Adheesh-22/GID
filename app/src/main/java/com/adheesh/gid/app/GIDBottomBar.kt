package com.adheesh.gid.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.adheesh.gid.core.designsystem.Cyan
import com.adheesh.gid.core.designsystem.Coral
import com.adheesh.gid.core.designsystem.SurfaceHigh
import com.adheesh.gid.core.designsystem.TextMuted

// ── Data model for a single bottom-bar tab ───────────────────────────────────
data class BottomNavItem(
    val destination : Any,       // NavKey
    val icon        : ImageVector,
    val label       : String,
    /** When true a small dot badge is shown on the icon (used for Settings) */
    val showBadge   : Boolean = false,
)

// ── Floating pill-shaped bottom navigation bar ────────────────────────────────
// Spec §5: "a floating pill-shaped bottom bar"
// The bar floats above the content with padding on the sides to create the
// pill illusion.  Shape.large (50dp) gives fully rounded ends.
@Composable
fun GIDBottomBar(
    items          : List<BottomNavItem>,
    currentDest    : Any,
    onItemSelected : (Any) -> Unit,
    modifier       : Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))        // pill shape
                .background(SurfaceHigh)
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { item ->
                BottomBarTab(
                    item       = item,
                    isSelected = currentDest == item.destination,
                    onClick    = { onItemSelected(item.destination) },
                    modifier   = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomBarTab(
    item       : BottomNavItem,
    isSelected : Boolean,
    onClick    : () -> Unit,
    modifier   : Modifier = Modifier,
) {
    // Icon color animates between Cyan (active) and TextMuted (inactive)
    val iconColor by animateColorAsState(
        targetValue    = if (isSelected) Cyan else TextMuted,
        animationSpec  = tween(durationMillis = 200),
        label          = "iconColor",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,       // custom ripple handled by clip
                onClick           = onClick,
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BadgedBox(
            badge = {
                if (item.showBadge) {
                    // Small coral dot to signal incomplete profile
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Coral)
                    )
                }
            }
        ) {
            Icon(
                imageVector         = item.icon,
                contentDescription  = item.label,
                tint                = iconColor,
                modifier            = Modifier.size(26.dp),
            )
        }
    }
}
