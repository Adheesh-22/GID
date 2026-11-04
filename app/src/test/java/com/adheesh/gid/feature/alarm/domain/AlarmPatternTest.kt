package com.adheesh.gid.feature.alarm.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AlarmPatternTest {

    @Test fun `every random pattern can be drawn exactly as shown`() {
        repeat(2_000) { seed ->
            val pattern = AlarmPattern.random(Random(seed))
            assertEquals(AlarmPattern.LENGTH, pattern.size)
            assertEquals(pattern.size, pattern.toSet().size)
            assertTrue(pattern.all { it in 0..8 })
            // Drawing it dot by dot must never pick up an extra dot on the way.
            val drawn = pattern.fold(emptyList<Int>()) { path, dot -> AlarmPattern.extend(path, dot) }
            assertEquals(pattern, drawn)
        }
    }

    @Test fun `patterns differ between rings`() {
        val many = (0 until 50).map { AlarmPattern.random(Random(it)) }.toSet()
        assertTrue(many.size > 30)
    }

    @Test fun `between finds the middle dot`() {
        assertEquals(1, AlarmPattern.between(0, 2))
        assertEquals(4, AlarmPattern.between(0, 8))
        assertEquals(4, AlarmPattern.between(2, 6))
        assertEquals(3, AlarmPattern.between(0, 6))
        assertNull(AlarmPattern.between(0, 1))
        assertNull(AlarmPattern.between(0, 5))
    }

    @Test fun `a line over an unused dot picks it up`() {
        assertEquals(listOf(0, 1, 2), AlarmPattern.extend(listOf(0), 2))
        // Already used: the line just passes over it.
        assertEquals(listOf(1, 0, 2), AlarmPattern.extend(listOf(1, 0), 2))
    }

    @Test fun `a dot already in the path is ignored`() {
        assertEquals(listOf(0, 1), AlarmPattern.extend(listOf(0, 1), 0))
    }
}
