package com.adheesh.gid.feature.home.viewmodel

import com.adheesh.gid.core.database.TimetableEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class UpcomingClassesTest {

    private val monday = LocalDate.of(2025, 11, 3)

    private fun entry(day: Int, start: Int, name: String = "C$day-$start") = TimetableEntryEntity(
        id = name, subject = name, dayOfWeek = day, startMinutes = start, endMinutes = start + 60,
        createdAt = 0, updatedAt = 0,
    )

    @Test fun `shows the next three classes still to come today`() {
        val entries = listOf(entry(1, 8 * 60), entry(1, 10 * 60), entry(1, 12 * 60), entry(1, 14 * 60), entry(1, 16 * 60))
        val result = HomeViewModel.upcomingClasses(entries, emptySet(), monday, LocalTime.of(9, 30))
        // 08:00-09:00 is over; 10, 12 and 14 are next.
        assertEquals(listOf("C1-600", "C1-720", "C1-840"), result.classes.map { it.subject })
        assertEquals(monday, result.day)
    }

    @Test fun `after the last class it shows the next class day`() {
        val entries = listOf(entry(1, 9 * 60), entry(3, 11 * 60))
        val result = HomeViewModel.upcomingClasses(entries, emptySet(), monday, LocalTime.of(20, 0))
        assertEquals(monday.plusDays(2), result.day)
        assertEquals("C3-660", result.classes.single().subject)
    }

    @Test fun `holiday today shows no classes`() {
        val result = HomeViewModel.upcomingClasses(listOf(entry(1, 9 * 60)), setOf(monday.toEpochDay()), monday, LocalTime.of(7, 0))
        assertTrue(result.isHoliday)
        assertTrue(result.classes.isEmpty())
    }
}
