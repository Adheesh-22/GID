package com.adheesh.gid.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ── GID Shape Tokens – SPEC §5 ───────────────────────────────────────────────
// cards = 28dp radius | pill buttons = fully round | dialogs = 28dp radius

val GIDShapes = Shapes(
    // Small components (chips, text fields)
    small  = RoundedCornerShape(8.dp),
    // Cards and dialogs
    medium = RoundedCornerShape(28.dp),
    // Pill buttons and bottom bar
    large  = RoundedCornerShape(50.dp),
    // Extra large (same as card, used by M3 bottom sheets)
    extraLarge = RoundedCornerShape(28.dp),
)
