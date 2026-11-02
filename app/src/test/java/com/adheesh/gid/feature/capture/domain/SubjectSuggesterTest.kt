package com.adheesh.gid.feature.capture.domain

import com.adheesh.gid.core.database.TimetableEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SubjectSuggesterTest {

    // Tuesday 4 Nov 2025
    private val tuesday = LocalDate.of(2025, 11, 4)

    private fun entry(subjectId: String?, start: Int, end: Int, day: Int = 2, enabled: Boolean = true) =
        TimetableEntryEntity(
            id = "e$start", subject = subjectId ?: "Free", subjectId = subjectId, dayOfWeek = day,
            startMinutes = start, endMinutes = end, enabled = enabled, createdAt = 0, updatedAt = 0,
        )

    private fun at(h: Int, m: Int) = LocalDateTime.of(tuesday, java.time.LocalTime.of(h, m))

    private val dbms = entry("dbms", 11 * 60, 12 * 60)

    @Test fun `photo during class suggests that subject`() {
        assertEquals("dbms", SubjectSuggester.suggest(at(11, 20), listOf(dbms), emptySet()))
    }

    @Test fun `window opens 10 minutes early and closes 15 minutes late`() {
        assertEquals("dbms", SubjectSuggester.suggest(at(10, 50), listOf(dbms), emptySet()))
        assertEquals("dbms", SubjectSuggester.suggest(at(12, 15), listOf(dbms), emptySet()))
        assertNull(SubjectSuggester.suggest(at(10, 49), listOf(dbms), emptySet()))
        assertNull(SubjectSuggester.suggest(at(12, 16), listOf(dbms), emptySet()))
    }

    @Test fun `other days, disabled classes and unlinked classes give nothing`() {
        assertNull(SubjectSuggester.suggest(at(11, 20), listOf(entry("dbms", 660, 720, day = 3)), emptySet()))
        assertNull(SubjectSuggester.suggest(at(11, 20), listOf(entry("dbms", 660, 720, enabled = false)), emptySet()))
        assertNull(SubjectSuggester.suggest(at(11, 20), listOf(entry(null, 660, 720)), emptySet()))
    }

    @Test fun `holidays give nothing`() {
        assertNull(SubjectSuggester.suggest(at(11, 20), listOf(dbms), setOf(tuesday.toEpochDay())))
    }

    @Test fun `back-to-back classes pick the one whose start is closest`() {
        val os = entry("os", 12 * 60, 13 * 60)
        // 12:05 is inside DBMS's late window and inside OS itself; OS just started.
        assertEquals("os", SubjectSuggester.suggest(at(12, 5), listOf(dbms, os), emptySet()))
    }
}
