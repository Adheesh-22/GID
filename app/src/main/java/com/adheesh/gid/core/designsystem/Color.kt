package com.adheesh.gid.core.designsystem

import androidx.compose.ui.graphics.Color

// ── GID Design Tokens – all colors from SPEC §5 ──────────────────────────────
// These are the single source of truth. Never use raw Color() values in screens.

/** Page backdrop – near-black */
val Background     = Color(0xFF161618)

/** Elevated card surface */
val Surface        = Color(0xFF232326)

/** Higher-elevation surface (e.g. dialogs) */
val SurfaceHigh    = Color(0xFF2E2E32)

/** Primary text */
val TextPrimary    = Color(0xFFF4F3EE)

/** Subdued labels, placeholders */
val TextMuted      = Color(0xFF9B9BA1)

/** Sage – general accent */
val Sage           = Color(0xFFC6DAD0)

/** Periwinkle – Alarm card, Low priority, sleep history bar */
val Periwinkle     = Color(0xFF6E6BF2)

/** Coral – Tasks card, High priority, productivity history bar */
val Coral          = Color(0xFFF2675A)

/** Mustard – Classes card, Medium priority, score history bar */
val Mustard        = Color(0xFFF5C842)

/** Cyan – active bottom nav icon */
val Cyan           = Color(0xFF7FE3E6)

/** Holiday green – calendar holiday marker */
val HolidayGreen   = Color(0xFF5ED393)

/** Error / destructive actions */
val ErrorRed       = Color(0xFFFF5C5C)

/** Text placed on top of colored cards (periwinkle / coral / mustard) */
val OnCard         = Color(0xFF161618)

// ── GID-prefixed aliases (canonical names used in screens) ────────────────────
val GIDBackground  = Background
val GIDSurface     = Surface
val GIDSurfaceHigh = SurfaceHigh
val GIDText        = TextPrimary
val GIDMutedText   = TextMuted
val GIDCyan        = Cyan
val GIDPeriwinkle  = Periwinkle
val GIDCoral       = Coral
val GIDMustard     = Mustard
val GIDHolidayGreen = HolidayGreen
val Error          = ErrorRed
