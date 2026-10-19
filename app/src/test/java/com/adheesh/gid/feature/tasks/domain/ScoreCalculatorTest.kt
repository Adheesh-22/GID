package com.adheesh.gid.feature.tasks.domain

import com.adheesh.gid.core.database.TaskEntity
import org.junit.Assert.*
import org.junit.Test

// ── ScoreCalculator unit tests ────────────────────────────────────────────────
// SPEC §3 rules verified:
//  • HIGH=3, MEDIUM=2, LOW=1 weights
//  • DayScore is null when there are no tasks (not 0)
//  • WeekScore is average of non-null day scores
//  • Change vs last week: null last week → valid points, null percent change
class ScoreCalculatorTest {

    // ── Helper ────────────────────────────────────────────────────────────────
    private fun task(
        priority : String,
        progress : Int,
        id       : String = "id",
    ) = TaskEntity(
        id              = id,
        title           = "Test",
        dateEpochDay    = 0L,
        priority        = priority,
        progressPercent = progress,
        status          = if (progress == 100) "DONE" else "PENDING",
        createdAt       = 0L,
        updatedAt       = 0L,
    )

    // ── dayScore ──────────────────────────────────────────────────────────────

    @Test
    fun `dayScore returns null for empty list`() {
        assertNull(ScoreCalculator.dayScore(emptyList()))
    }

    @Test
    fun `dayScore all DONE returns 1_0`() {
        val tasks = listOf(
            task("HIGH",   100, "a"),
            task("MEDIUM", 100, "b"),
            task("LOW",    100, "c"),
        )
        // 3*1 + 2*1 + 1*1 / (3+2+1) = 6/6 = 1.0
        assertEquals(1.0, ScoreCalculator.dayScore(tasks)!!, 0.001)
    }

    @Test
    fun `dayScore all PENDING returns 0_0`() {
        val tasks = listOf(
            task("HIGH",   0, "a"),
            task("MEDIUM", 0, "b"),
        )
        assertEquals(0.0, ScoreCalculator.dayScore(tasks)!!, 0.001)
    }

    @Test
    fun `dayScore mixed priorities and progress`() {
        // HIGH (w=3) at 100% + LOW (w=1) at 0%
        // = (3*1.0 + 1*0.0) / (3+1) = 3/4 = 0.75
        val tasks = listOf(
            task("HIGH", 100, "a"),
            task("LOW",    0, "b"),
        )
        assertEquals(0.75, ScoreCalculator.dayScore(tasks)!!, 0.001)
    }

    @Test
    fun `dayScore single MEDIUM task at 50 percent`() {
        // (2 * 0.5) / 2 = 0.5
        val tasks = listOf(task("MEDIUM", 50))
        assertEquals(0.5, ScoreCalculator.dayScore(tasks)!!, 0.001)
    }

    // ── weekScore ─────────────────────────────────────────────────────────────

    @Test
    fun `weekScore returns null when all days have no tasks`() {
        val byDay = mapOf(0L to emptyList<TaskEntity>(), 1L to emptyList())
        assertNull(ScoreCalculator.weekScore(byDay))
    }

    @Test
    fun `weekScore averages non-null day scores`() {
        // Day 0: 1.0, Day 1: 0.0 → average = 0.5
        val byDay = mapOf(
            0L to listOf(task("HIGH", 100)),  // score 1.0
            1L to listOf(task("HIGH", 0)),    // score 0.0
            2L to emptyList(),                // null – excluded
        )
        assertEquals(0.5, ScoreCalculator.weekScore(byDay)!!, 0.001)
    }

    @Test
    fun `weekScore with single day`() {
        val byDay = mapOf(0L to listOf(task("MEDIUM", 60)))
        // (2 * 0.6) / 2 = 0.6
        assertEquals(0.6, ScoreCalculator.weekScore(byDay)!!, 0.001)
    }

    // ── weekChange ────────────────────────────────────────────────────────────

    @Test
    fun `weekChange returns null when thisWeek is null`() {
        assertNull(ScoreCalculator.weekChange(null, 0.5))
    }

    @Test
    fun `weekChange when lastWeek is null gives null percentChange`() {
        val change = ScoreCalculator.weekChange(0.8, null)!!
        // points = (0.8 - 0.0) * 100 = 80
        assertEquals(80.0, change.points, 0.001)
        assertNull(change.percentChange)
    }

    @Test
    fun `weekChange positive improvement`() {
        val change = ScoreCalculator.weekChange(0.8, 0.5)!!
        assertEquals(30.0, change.points, 0.001)
        // (0.8 - 0.5) / 0.5 * 100 = 60%
        assertEquals(60.0, change.percentChange!!, 0.001)
    }
}
