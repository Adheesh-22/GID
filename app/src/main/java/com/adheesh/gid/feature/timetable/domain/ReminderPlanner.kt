package com.adheesh.gid.feature.timetable.domain

import com.adheesh.gid.core.database.TimetableEntryEntity
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** One class reminder to schedule. */
data class PlannedReminder(
    val triggerAt    : Long,     // epoch millis
    val entryId      : String,
    val subject      : String,
    val room         : String?,
    val startMinutes : Int,
)

// ── Class-reminder planning – Phase 5 ────────────────────────────────────────
// For each enabled class, a reminder N minutes before it starts, for the next
// [days] days, skipping holidays and anything already in the past.
object ReminderPlanner {

    fun plan(
        entries     : List<TimetableEntryEntity>,
        holidayDays : Set<Long>,
        now         : ZonedDateTime,
        days        : Int = 7,
    ): List<PlannedReminder> {
        val zone: ZoneId = now.zone
        val nowMs = now.toInstant().toEpochMilli()
        val result = mutableListOf<PlannedReminder>()
        for (offset in 0 until days) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (date.toEpochDay() in holidayDays) continue
            for (entry in entries) {
                if (!entry.enabled || entry.dayOfWeek != date.dayOfWeek.value) continue
                val start = ZonedDateTime.of(
                    date, LocalTime.of(entry.startMinutes / 60, entry.startMinutes % 60), zone,
                )
                val trigger = start.minusMinutes(entry.reminderMinutes.toLong()).toInstant().toEpochMilli()
                if (trigger <= nowMs) continue
                result += PlannedReminder(trigger, entry.id, entry.subject, entry.room, entry.startMinutes)
            }
        }
        return result.sortedBy { it.triggerAt }
    }
}
