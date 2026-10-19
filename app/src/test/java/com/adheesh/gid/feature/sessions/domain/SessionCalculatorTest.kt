package com.adheesh.gid.feature.sessions.domain

import com.adheesh.gid.core.database.ActivitySessionEntity
import org.junit.Assert.*
import org.junit.Test

// ── SessionCalculator unit tests ──────────────────────────────────────────────
// Covers: sleep totals (long / nap / total), focus by type, cross-midnight sleep
class SessionCalculatorTest {

    // Epoch day 0 = 1970-01-01 UTC; 1 day = 86,400,000 ms
    private val DAY_MS = 86_400_000L

    private fun session(
        category : String,
        typeId   : String = category,
        startMs  : Long,
        endMs    : Long?,
        status   : String = "COMPLETED",
    ) = ActivitySessionEntity(
        id        = "$category-$startMs",
        typeId    = typeId,
        category  = category,
        startAt   = startMs,
        endAt     = endMs,
        source    = "MANUAL",
        status    = status,
        createdAt = 0L,
        updatedAt = 0L,
    )

    // ── sleepTotals ───────────────────────────────────────────────────────────

    @Test
    fun `sleepTotals returns zeros for empty list`() {
        val totals = SessionCalculator.sleepTotals(emptyList())
        assertEquals(0L, totals.longSleepMinutes)
        assertEquals(0L, totals.napMinutes)
        assertEquals(0L, totals.totalMinutes)
    }

    @Test
    fun `sleepTotals counts only SLEEP and NAP categories`() {
        val sessions = listOf(
            session("SLEEP", startMs = 0, endMs = 7 * 60 * 60_000L),    // 7h
            session("NAP",   startMs = 0, endMs = 30 * 60_000L),         // 30m
            session("FOCUS", startMs = 0, endMs = 60 * 60_000L),         // ignored
        )
        val totals = SessionCalculator.sleepTotals(sessions)
        assertEquals(420L, totals.longSleepMinutes)  // 7h = 420m
        assertEquals(30L,  totals.napMinutes)
        assertEquals(450L, totals.totalMinutes)
    }

    @Test
    fun `sleepTotals handles session crossing midnight`() {
        // Starts at 23:00 on day 0 (23*60*60_000 ms from epoch)
        // Ends at 06:00 on day 1 (1*DAY_MS + 6*3600_000)
        val startMs = 23 * 3_600_000L
        val endMs   = DAY_MS + 6 * 3_600_000L         // 7 hours total
        val sessions = listOf(session("SLEEP", startMs = startMs, endMs = endMs))
        val totals   = SessionCalculator.sleepTotals(sessions)
        assertEquals(420L, totals.longSleepMinutes)   // 7h = 420 min
    }

    @Test
    fun `sleepTotals returns 0 for RUNNING session with no endAt`() {
        val sessions = listOf(session("SLEEP", startMs = 0, endMs = null, status = "RUNNING"))
        val totals   = SessionCalculator.sleepTotals(sessions)
        assertEquals(0L, totals.longSleepMinutes)
    }

    // ── focusByType ───────────────────────────────────────────────────────────

    @Test
    fun `focusByType returns empty map for no focus sessions`() {
        val sessions = listOf(session("SLEEP", startMs = 0, endMs = 3_600_000L))
        assertTrue(SessionCalculator.focusByType(sessions).isEmpty())
    }

    @Test
    fun `focusByType groups by typeId and sums minutes`() {
        val sessions = listOf(
            session("FOCUS", typeId = "study", startMs = 0,          endMs = 25 * 60_000L),
            session("FOCUS", typeId = "study", startMs = 3_600_000L, endMs = 3_600_000L + 45 * 60_000L),
            session("FOCUS", typeId = "dsa",   startMs = 0,          endMs = 60 * 60_000L),
        )
        val result = SessionCalculator.focusByType(sessions)
        assertEquals(70L, result["study"])    // 25 + 45 = 70 min
        assertEquals(60L, result["dsa"])
        assertNull(result["sleep"])
    }

    @Test
    fun `totalFocusMinutes sums all focus sessions`() {
        val sessions = listOf(
            session("FOCUS", typeId = "a", startMs = 0, endMs = 25 * 60_000L),
            session("FOCUS", typeId = "b", startMs = 0, endMs = 60 * 60_000L),
        )
        assertEquals(85L, SessionCalculator.totalFocusMinutes(sessions))
    }
}
