package com.adheesh.gid.feature.attendance.domain

import com.adheesh.gid.core.database.TimetableEntryEntity
import java.time.LocalTime
import java.time.ZonedDateTime

/** One class on one date, the key a mark is stored under. */
data class ClassSlot(val entryId: String, val dateEpochDay: Long, val startMinutes: Int)

/** One 'Were you in …?' notification to schedule. */
data class PlannedAttendancePrompt(val triggerAt: Long, val slot: ClassSlot)

// ── After-class prompts (SPEC §2.12) ─────────────────────────────────────────
// One prompt when each class ends, for the next [days] days, skipping
// holidays, disabled classes, times already past and classes already marked.
// Same shape as the class-reminder ReminderPlanner.
object AttendancePromptPlanner {

    const val MAX_PROMPTS = 60

    fun plan(
        entries     : List<TimetableEntryEntity>,
        holidayDays : Set<Long>,
        marked      : Set<ClassSlot>,
        now         : ZonedDateTime,
        days        : Int = 7,
    ): List<PlannedAttendancePrompt> {
        val nowMs = now.toInstant().toEpochMilli()
        val result = mutableListOf<PlannedAttendancePrompt>()
        for (offset in 0 until days) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            val day = date.toEpochDay()
            if (day in holidayDays) continue
            for (entry in entries) {
                if (!entry.enabled || entry.dayOfWeek != date.dayOfWeek.value) continue
                val slot = ClassSlot(entry.id, day, entry.startMinutes)
                if (slot in marked) continue
                val end = entry.endMinutes.coerceIn(0, 24 * 60 - 1)
                val trigger = ZonedDateTime.of(date, LocalTime.of(end / 60, end % 60), now.zone)
                    .toInstant().toEpochMilli()
                if (trigger <= nowMs) continue
                result += PlannedAttendancePrompt(trigger, slot)
            }
        }
        return result.sortedBy { it.triggerAt }.take(MAX_PROMPTS)
    }
}
