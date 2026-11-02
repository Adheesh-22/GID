package com.adheesh.gid.feature.capture.domain

import com.adheesh.gid.core.database.TimetableEntryEntity
import java.time.LocalDateTime

// ── Subject suggestion from the timetable (SPEC §6 capture rules) ────────────
// A photo taken during (or just around) a class most likely belongs to that
// class. The window opens a little early (board photos before the bell) and
// stays open a little after (questions asked as class ends).
object SubjectSuggester {

    const val MINUTES_BEFORE = 10
    const val MINUTES_AFTER  = 15

    /**
     * Returns the subjectId of the class running at [at], or null when there is
     * none, the day is a holiday, or the class isn't linked to a subject.
     * If two classes overlap the window, the one whose start is closest wins.
     */
    fun suggest(
        at          : LocalDateTime,
        entries     : List<TimetableEntryEntity>,
        holidayDays : Set<Long>,
    ): String? {
        if (at.toLocalDate().toEpochDay() in holidayDays) return null
        val minute = at.hour * 60 + at.minute
        return entries
            .asSequence()
            .filter { it.enabled && it.subjectId != null && it.dayOfWeek == at.dayOfWeek.value }
            .filter { minute in (it.startMinutes - MINUTES_BEFORE)..(it.endMinutes + MINUTES_AFTER) }
            .minByOrNull { kotlin.math.abs(minute - it.startMinutes) }
            ?.subjectId
    }
}
