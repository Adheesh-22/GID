package com.adheesh.gid.feature.timetable.domain

import com.adheesh.gid.core.database.TimetableEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderPlannerTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    // Monday 3 Nov 2025, 08:00
    private val mondayMorning = ZonedDateTime.of(2025, 11, 3, 8, 0, 0, 0, zone)

    private fun entry(day: Int, start: Int, reminder: Int = 10, enabled: Boolean = true) = TimetableEntryEntity(
        id = "e$day$start", subject = "DSA", dayOfWeek = day, startMinutes = start, endMinutes = start + 60,
        reminderMinutes = reminder, enabled = enabled, createdAt = 0, updatedAt = 0,
    )

    @Test fun `reminder fires N minutes before class`() {
        val plan = ReminderPlanner.plan(listOf(entry(1, 9 * 60)), emptySet(), mondayMorning, days = 1)
        assertEquals(1, plan.size)
        assertEquals(ZonedDateTime.of(2025, 11, 3, 8, 50, 0, 0, zone).toInstant().toEpochMilli(), plan[0].triggerAt)
    }

    @Test fun `one reminder per class day for a week`() {
        val plan = ReminderPlanner.plan(listOf(entry(1, 9 * 60), entry(3, 9 * 60)), emptySet(), mondayMorning)
        assertEquals(2, plan.size)   // Monday and Wednesday of this week
    }

    @Test fun `holidays are skipped`() {
        val holiday = LocalDate.of(2025, 11, 3).toEpochDay()
        val plan = ReminderPlanner.plan(listOf(entry(1, 9 * 60)), setOf(holiday), mondayMorning)
        assertTrue(plan.isEmpty())
    }

    @Test fun `past reminders and disabled classes are skipped`() {
        val plan = ReminderPlanner.plan(listOf(entry(1, 8 * 60), entry(1, 11 * 60, enabled = false)), emptySet(), mondayMorning, days = 1)
        assertTrue(plan.isEmpty())
    }
}
