package com.adheesh.gid.feature.attendance.domain

import com.adheesh.gid.core.database.TimetableEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class AttendancePromptPlannerTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    // Monday 3 Nov 2025, 08:00
    private val mondayMorning = ZonedDateTime.of(2025, 11, 3, 8, 0, 0, 0, zone)
    private val monday = LocalDate.of(2025, 11, 3).toEpochDay()

    private fun entry(day: Int, start: Int, enabled: Boolean = true) = TimetableEntryEntity(
        id = "e$day$start", subject = "DSA", dayOfWeek = day, startMinutes = start, endMinutes = start + 60,
        enabled = enabled, createdAt = 0, updatedAt = 0,
    )

    @Test fun `prompt fires when the class ends`() {
        val plan = AttendancePromptPlanner.plan(listOf(entry(1, 9 * 60)), emptySet(), emptySet(), mondayMorning, days = 1)
        assertEquals(1, plan.size)
        assertEquals(ZonedDateTime.of(2025, 11, 3, 10, 0, 0, 0, zone).toInstant().toEpochMilli(), plan[0].triggerAt)
        assertEquals(ClassSlot("e1540", monday, 9 * 60), plan[0].slot)
    }

    @Test fun `marked classes, holidays, disabled and finished classes are skipped`() {
        val entries = listOf(entry(1, 9 * 60), entry(1, 11 * 60, enabled = false), entry(1, 6 * 60), entry(2, 9 * 60))
        val marked = setOf(ClassSlot("e1540", monday, 9 * 60))
        val tuesday = monday + 1
        val plan = AttendancePromptPlanner.plan(entries, setOf(tuesday), marked, mondayMorning, days = 2)
        assertTrue(plan.isEmpty())
    }

    @Test fun `one prompt per class day for a week, soonest first`() {
        val plan = AttendancePromptPlanner.plan(listOf(entry(3, 9 * 60), entry(1, 9 * 60)), emptySet(), emptySet(), mondayMorning)
        assertEquals(listOf(monday, monday + 2), plan.map { it.slot.dateEpochDay })
    }
}
