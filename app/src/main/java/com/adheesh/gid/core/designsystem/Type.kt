package com.adheesh.gid.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.adheesh.gid.R

// ── GID Typography – SPEC §5 ─────────────────────────────────────────────────
// Titles → Space Grotesk Bold (uppercase, tight letter-spacing)
// Body  → DM Sans (regular / medium)
// Scale: title 28sp · section 20sp · body 16sp · caption 12sp

val SpaceGroteskFamily = FontFamily(
    Font(R.font.space_grotesk_bold, FontWeight.Bold)
)

val DmSansFamily = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium,  FontWeight.Medium),
)

val GIDTypography = Typography(
    // Screen / section titles  – Space Grotesk Bold, uppercase feel via letterSpacing
    displayLarge = TextStyle(
        fontFamily     = SpaceGroteskFamily,
        fontWeight     = FontWeight.Bold,
        fontSize       = 28.sp,
        letterSpacing  = 0.5.sp,
    ),
    // Section headings inside a screen
    titleLarge = TextStyle(
        fontFamily     = SpaceGroteskFamily,
        fontWeight     = FontWeight.Bold,
        fontSize       = 20.sp,
        letterSpacing  = 0.3.sp,
    ),
    // Card headings / list item primary text
    bodyLarge = TextStyle(
        fontFamily    = DmSansFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 16.sp,
        letterSpacing = 0.sp,
    ),
    // Secondary text
    bodyMedium = TextStyle(
        fontFamily    = DmSansFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 14.sp,
        letterSpacing = 0.sp,
    ),
    // Captions / badges / timestamps
    labelSmall = TextStyle(
        fontFamily    = DmSansFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 12.sp,
        letterSpacing = 0.sp,
    ),
    // Button labels
    labelLarge = TextStyle(
        fontFamily    = DmSansFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 16.sp,
        letterSpacing = 0.sp,
    ),
)
