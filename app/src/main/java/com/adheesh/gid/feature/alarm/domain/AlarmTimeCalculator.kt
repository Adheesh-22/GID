package com.adheesh.gid.feature.alarm.domain

import java.time.LocalTime
import java.time.ZonedDateTime

// ── When does an alarm ring next? ────────────────────────────────────────────
// repeatDaysMask: bit 0 = Monday … bit 6 = Sunday. 0 means a one-off alarm,
// which rings at the next occurrence of hour:minute.
object AlarmTimeCalculator {

    fun isDayEnabled(mask: Int, isoDayOfWeek: Int): Boolean =
        mask and (1 shl (isoDayOfWeek - 1)) != 0

    fun toggleDay(mask: Int, isoDayOfWeek: Int): Int = mask xor (1 shl (isoDayOfWeek - 1))

    /** The first moment strictly after [from] at which the alarm should ring. */
    fun nextTrigger(hour: Int, minute: Int, repeatMask: Int, from: ZonedDateTime): ZonedDateTime {
        val time = LocalTime.of(hour, minute)
        // Look at today and the next 7 days; one of them always matches.
        for (offset in 0..7) {
            val date      = from.toLocalDate().plusDays(offset.toLong())
            val candidate = ZonedDateTime.of(date, time, from.zone)
            if (!candidate.isAfter(from)) continue
            if (repeatMask == 0 || isDayEnabled(repeatMask, date.dayOfWeek.value)) return candidate
        }
        // Unreachable for a valid mask; fall back to tomorrow at the same time.
        return ZonedDateTime.of(from.toLocalDate().plusDays(1), time, from.zone)
    }
}
