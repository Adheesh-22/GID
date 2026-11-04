package com.adheesh.gid.core.database

// ── Value sets stored as strings in Room ─────────────────────────────────────
// Entities keep these as plain strings (enum.name) so no type converters are
// needed; these enums give the rest of the code one place to spell them.

enum class Priority(val weight: Int) {
    LOW(1), MEDIUM(2), HIGH(3);

    companion object {
        fun of(value: String): Priority = entries.firstOrNull { it.name == value.uppercase() } ?: MEDIUM
    }
}

enum class TaskStatus { PENDING, IN_PROGRESS, DONE }

enum class ActivityCategory {
    SLEEP, NAP, FOCUS, CLASS, FREE, OTHER;

    companion object {
        fun of(value: String): ActivityCategory = entries.firstOrNull { it.name == value.uppercase() } ?: OTHER
    }
}

enum class SessionSource { ALARM, TIMER, MANUAL }

enum class SessionStatus { RUNNING, COMPLETED, CANCELLED }

enum class CaptureKind {
    PHOTO, LINK, TEXT, PDF,
    /** A Word or PowerPoint file; mimeType says which. */
    DOCUMENT;

    companion object {
        fun of(value: String): CaptureKind = entries.firstOrNull { it.name == value } ?: TEXT
    }
}

enum class ExamKind {
    EXAM, TEST, ASSIGNMENT, PROJECT;

    /** Assignments and projects are submitted; exams and tests are sat. */
    val isSubmitted: Boolean get() = this == ASSIGNMENT || this == PROJECT

    companion object {
        fun of(value: String): ExamKind = entries.firstOrNull { it.name == value } ?: EXAM
    }
}

/** How a class went on one date (SPEC §2.12). Cancelled never counts. */
enum class AttendanceStatus {
    PRESENT, ABSENT, CANCELLED;

    companion object {
        fun of(value: String?): AttendanceStatus? = entries.firstOrNull { it.name == value }
    }
}

/** The categories every install starts with; their names are the seeded ids (SPEC §2.8). */
enum class CaptureType(val defaultName: String) {
    EXAM_QUESTION("Exam question"),
    RESOURCE("Resource"),
    NOTE("Note"),
    DEADLINE("Deadline"),
    IDEA("Idea"),
    NOTICE("Notice"),
}

enum class CaptureStatus { INBOX, SORTED, ARCHIVED }
