package com.adheesh.gid.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// ── Navigation destinations (NavKeys) ────────────────────────────────────────
// Each object/class is a type-safe key for Navigation 3.
// @Serializable is required by Nav3's back-stack serialization.

@Serializable data object Auth     : NavKey
@Serializable data object Home     : NavKey
@Serializable data object Calendar : NavKey
@Serializable data object History  : NavKey
@Serializable data object Settings : NavKey

/** Debug-only destination – guarded by BuildConfig.DEBUG in the nav host. */
@Serializable data object Debug    : NavKey

