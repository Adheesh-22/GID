package com.adheesh.gid.feature.history.domain

import com.adheesh.gid.core.database.ActivitySessionEntity
import com.adheesh.gid.core.database.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class HistoryCalculatorTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val monday = LocalDate.of(2025, 11, 3)

    private fun task(day: LocalDate, priority: String, percent: Int) = TaskEntity(
        id = "${day}$priority$percent", title = "t", dateEpochDay = day.toEpochDay(), priority = priority,
        progressPercent = percent, createdAt = 0, updatedAt = 0,
    )

    private fun session(category: String, start: ZonedDateTime, end: ZonedDateTime, type: String = category) = ActivitySessionEntity(
        id = "$category$start", typeId = type, category = category,
        startAt = start.toInstant().toEpochMilli(), endAt = end.toInstant().toEpochMilli(),
        source = "MANUAL", status = "COMPLETED", createdAt = 0, updatedAt = 0,
    )

    private fun time(day: Int, hour: Int) = ZonedDateTime.of(2025, 11, day, hour, 0, 0, 0, zone)

    @Test fun `a week with no data has null scores, never zero`() {
        val week = HistoryCalculator.summarize(monday, emptyList(), emptyList(), zone)
        assertEquals(7, week.days.size)
        week.days.forEach { assertNull(it.score) }
        assertNull(week.weekScore)
        assertNull(week.previousWeekScore)
        assertNull(week.change)
        assertNull(week.averagePercent)
        assertEquals(0, week.tasksRegistered)
    }

    @Test fun `day score is weighted by priority`() {
        // HIGH 100% (w3) + LOW 0% (w1) = 300 / 4 = 75%
        val week = HistoryCalculator.summarize(monday, listOf(task(monday, "HIGH", 100), task(monday, "LOW", 0)), emptyList(), zone)
        assertEquals(0.75, week.days[0].score!!, 1e-9)
        assertNull(week.days[1].score)
        assertEquals(0.75, week.weekScore!!, 1e-9)
        assertEquals(1, week.tasksCompleted)
        assertEquals(2, week.tasksRegistered)
    }

    @Test fun `change against last week in points and percent`() {
        val lastMonday = monday.minusDays(7)
        val tasks = listOf(task(monday, "MEDIUM", 80), task(lastMonday, "MEDIUM", 40))
        val week = HistoryCalculator.summarize(monday, tasks, emptyList(), zone)
        assertEquals(0.4, week.previousWeekScore!!, 1e-9)
        assertEquals(40.0, week.change!!.points, 1e-9)
        assertEquals(100.0, week.change!!.percentChange!!, 1e-9)
    }

    @Test fun `sleep crossing midnight lands on the wake-up day and naps are separate`() {
        val sessions = listOf(
            session("SLEEP", time(3, 23), time(4, 7)),   // Mon 23:00 -> Tue 07:00
            session("NAP", time(4, 14), time(4, 15)),
            session("FOCUS", time(4, 10), time(4, 11), type = "study"),
        )
        val week = HistoryCalculator.summarize(monday, emptyList(), sessions, zone)
        val mon = week.days[0]
        val tue = week.days[1]
        assertEquals(0L, mon.sleep.totalMinutes)
        assertEquals(480L, tue.sleep.longSleepMinutes)
        assertEquals(60L, tue.sleep.napMinutes)
        assertEquals(540L, tue.sleep.totalMinutes)
        assertEquals(mapOf("study" to 60L), tue.focusByType)
        assertEquals(60L, week.focusMinutes)
    }
}
