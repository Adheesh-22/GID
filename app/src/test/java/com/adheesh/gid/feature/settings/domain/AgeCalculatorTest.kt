package com.adheesh.gid.feature.settings.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AgeCalculatorTest {

    @Test
    fun `returns null when dobEpochDay is null`() {
        assertNull(AgeCalculator.ageYears(null))
    }

    @Test
    fun `age on birthday returns correct year`() {
        val dob   = LocalDate.of(2000, 10, 19)
        val today = LocalDate.of(2026, 10, 19)
        assertEquals(26, AgeCalculator.ageYears(dob.toEpochDay(), today))
    }

    @Test
    fun `age one day before birthday is one less year`() {
        val dob   = LocalDate.of(2000, 10, 20)
        val today = LocalDate.of(2026, 10, 19)
        assertEquals(25, AgeCalculator.ageYears(dob.toEpochDay(), today))
    }

    @Test
    fun `age one day after birthday is correct`() {
        val dob   = LocalDate.of(2000, 10, 18)
        val today = LocalDate.of(2026, 10, 19)
        assertEquals(26, AgeCalculator.ageYears(dob.toEpochDay(), today))
    }

    @Test
    fun `age for newborn is 0`() {
        val dob   = LocalDate.of(2026, 10, 19)
        val today = LocalDate.of(2026, 10, 19)
        assertEquals(0, AgeCalculator.ageYears(dob.toEpochDay(), today))
    }
}
