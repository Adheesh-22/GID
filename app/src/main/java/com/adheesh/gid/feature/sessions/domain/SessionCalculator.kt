package com.adheesh.gid.feature.sessions.domain

import com.adheesh.gid.core.database.ActivitySessionEntity

// ── Sleep & Focus totals – SPEC §3 ───────────────────────────────────────────
// "Sleep counts on the day the session ENDS."
// Long sleep = SLEEP sessions; Nap = NAP sessions; Total = both.
// Focus time = FOCUS sessions grouped by typeId.
//
// All functions accept a list of sessions already pre-filtered to the target
// day by the DAO (endAt epochDay == target epochDay).

object SessionCalculator {

    /** Duration of a session in minutes (0 if not completed). */
    private fun durationMinutes(session: ActivitySessionEntity): Long {
        val end = session.endAt ?: return 0L
        return (end - session.startAt).coerceAtLeast(0L) / 60_000L
    }

    data class SleepTotals(
        val longSleepMinutes : Long,   // SLEEP sessions
        val napMinutes       : Long,   // NAP sessions
        val totalMinutes     : Long,   // long + nap
    )

    /**
     * Compute sleep totals for a given list of sessions.
     * [sessions] should be the sessions that END on the target day.
     */
    fun sleepTotals(sessions: List<ActivitySessionEntity>): SleepTotals {
        var longSleep = 0L
        var nap       = 0L
        for (s in sessions) {
            val mins = durationMinutes(s)
            when (s.category.uppercase()) {
                "SLEEP" -> longSleep += mins
                "NAP"   -> nap       += mins
            }
        }
        return SleepTotals(longSleep, nap, longSleep + nap)
    }

    /**
     * Focus time in minutes, grouped by activity typeId.
     * [sessions] should be pre-filtered to the relevant day/range.
     */
    fun focusByType(sessions: List<ActivitySessionEntity>): Map<String, Long> {
        return sessions
            .filter { it.category.uppercase() == "FOCUS" }
            .groupBy { it.typeId }
            .mapValues { (_, list) -> list.sumOf { durationMinutes(it) } }
    }

    /** Total focus minutes across all types. */
    fun totalFocusMinutes(sessions: List<ActivitySessionEntity>): Long =
        focusByType(sessions).values.sum()
}
