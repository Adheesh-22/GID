package com.adheesh.gid.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// ── Navigation destinations (NavKeys) ────────────────────────────────────────
// Each object/class is a type-safe key for Navigation 3.
// @Serializable is required by Nav3's back-stack serialization.

@Serializable data object Auth     : NavKey
@Serializable data object Home     : NavKey
@Serializable data object Calendar : NavKey
@Serializable data object Library  : NavKey
@Serializable data class SubjectDetail(val subjectId: String) : NavKey
@Serializable data class CaptureDetail(val captureId: String) : NavKey
@Serializable data object Exams    : NavKey
@Serializable data class ExamDetail(val examId: String) : NavKey
@Serializable data object Attendance : NavKey
@Serializable data class Syllabus(val subjectId: String) : NavKey
@Serializable data object History  : NavKey
@Serializable data object Settings : NavKey

/** A request from outside the app to open Library, optionally sorting [sortCaptureIds]. */
data class LibraryRequest(val sortCaptureIds: List<String>)

/** Debug-only destination – guarded by BuildConfig.DEBUG in the nav host. */
@Serializable data object Debug    : NavKey

