package com.adheesh.gid.app

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene

// ── Page motion (SPEC §1) ────────────────────────────────────────────────────
// Tabs sit side by side in bottom-bar order and slide sideways; full-screen
// pages slide up from the bottom over what is below and back down on Back.
// Nothing cross-fades, so two screens are never drawn on top of each other.

private const val TAB_INDEX = "gid.tabIndex"
private const val PAGE      = "gid.page"

private const val TAB_MILLIS       = 220
private const val PAGE_IN_MILLIS   = 300
private const val PAGE_OUT_MILLIS  = 250

/** Metadata for a bottom-bar tab at [index] (0 = Home, left to right). */
fun tabMetadata(index: Int): Map<String, Any> = mapOf(TAB_INDEX to index)

/** Metadata for a full-screen page that slides up from the bottom. */
val pageMetadata: Map<String, Any> = mapOf(PAGE to true)

private val Scene<NavKey>.tabIndex get() = metadata[TAB_INDEX] as? Int
private val Scene<NavKey>.isPage   get() = metadata[PAGE] == true

/** Going forward: a tab change, or a page opening. */
fun AnimatedContentTransitionScope<Scene<NavKey>>.gidPushTransition(): ContentTransform =
    tabSlide() ?: if (targetState.isPage) pageUp() else crossFade()

/** Going back: a tab change (to Home), or a page closing. */
fun AnimatedContentTransitionScope<Scene<NavKey>>.gidPopTransition(): ContentTransform =
    tabSlide() ?: if (initialState.isPage) pageDown() else crossFade()

private fun AnimatedContentTransitionScope<Scene<NavKey>>.tabSlide(): ContentTransform? {
    val from = initialState.tabIndex ?: return null
    val to   = targetState.tabIndex ?: return null
    // Moving right brings the new tab in from the right edge, and vice versa.
    val direction = if (to > from) 1 else -1
    val spec = tween<IntOffset>(TAB_MILLIS, easing = FastOutSlowInEasing)
    return slideInHorizontally(spec) { width -> width * direction } togetherWith
        slideOutHorizontally(spec) { width -> -width * direction }
}

private fun pageUp(): ContentTransform =
    slideInVertically(tween(PAGE_IN_MILLIS, easing = FastOutSlowInEasing)) { height -> height } togetherWith
        // Keeps the screen below fully visible and still until the page covers it.
        fadeOut(tween(PAGE_IN_MILLIS), targetAlpha = 1f)

private fun pageDown(): ContentTransform =
    (EnterTransition.None togetherWith
        slideOutVertically(tween(PAGE_OUT_MILLIS, easing = FastOutSlowInEasing)) { height -> height })
        // The closing page stays on top while it slides away.
        .apply { targetContentZIndex = -1f }

/** Sign-in to Home and other one-off changes. */
private fun crossFade(): ContentTransform = fadeIn(tween(TAB_MILLIS)) togetherWith fadeOut(tween(TAB_MILLIS))
