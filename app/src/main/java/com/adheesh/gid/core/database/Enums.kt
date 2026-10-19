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
