package com.adheesh.gid.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// ── GID Theme – SPEC §5 – dark theme only ────────────────────────────────────
// We force the dark color scheme; there is no light variant in V1.
// Material 3 roles are mapped to spec tokens:
//   background        → Background  (page backdrop)
//   surface           → Surface     (elevated card surface)
//   surfaceVariant    → SurfaceHigh (dialogs / higher elevation)
//   primary           → Periwinkle  (primary accent)
//   secondary         → Coral       (tasks / high-priority)
//   tertiary          → Mustard     (calendar / medium-priority)
//   onBackground      → TextPrimary
//   onSurface         → TextPrimary
//   onPrimary         → OnCard      (text on colored cards)
//   error             → ErrorRed

private val GIDDarkColorScheme = darkColorScheme(
    background       = Background,
    surface          = Surface,
    surfaceVariant   = SurfaceHigh,
    primary          = Periwinkle,
    onPrimary        = OnCard,
    secondary        = Coral,
    onSecondary      = OnCard,
    tertiary         = Mustard,
    onTertiary       = OnCard,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    onSurfaceVariant = TextMuted,
    error            = ErrorRed,
)

@Composable
fun GIDTheme(content: @Composable () -> Unit) {
    // We always use our dark scheme – no dynamic color, no light variant in V1.
    MaterialTheme(
        colorScheme = GIDDarkColorScheme,
        typography  = GIDTypography,
        shapes      = GIDShapes,
        content     = content,
    )
}
