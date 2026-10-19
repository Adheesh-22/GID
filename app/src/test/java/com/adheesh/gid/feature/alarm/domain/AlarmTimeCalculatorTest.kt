package com.adheesh.gid.feature.alarm.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmTimeCalculatorTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    // Wednesday 5 Nov 2025, 08:00
    private val wednesday8am = ZonedDateTime.of(2025, 11, 5, 8, 0, 0, 0, zone)

    @Test fun `one-off alarm later today rings today`() {
        val next = AlarmTimeCalculator.nextTrigger(9, 30, 0, wednesday8am)
        assertEquals(ZonedDateTime.of(2025, 11, 5, 9, 30, 0, 0, zone), next)
    }

    @Test fun `one-off alarm earlier today rings tomorrow`() {
        val next = AlarmTimeCalculator.nextTrigger(7, 0, 0, wednesday8am)
        assertEquals(ZonedDateTime.of(2025, 11, 6, 7, 0, 0, 0, zone), next)
    }

    @Test fun `an alarm set for exactly now rings next time, not now`() {
        val next = AlarmTimeCalculator.nextTrigger(8, 0, 0, wednesday8am)
        assertEquals(6, next.dayOfMonth)
    }

    @Test fun `weekday repeat skips the weekend`() {
        val weekdays = 0b0011111
        // Friday 7 Nov 2025 at 23:00 -> next weekday alarm is Monday 10 Nov
        val fridayNight = ZonedDateTime.of(2025, 11, 7, 23, 0, 0, 0, zone)
        val next = AlarmTimeCalculator.nextTrigger(6, 30, weekdays, fridayNight)
        assertEquals(ZonedDateTime.of(2025, 11, 10, 6, 30, 0, 0, zone), next)
    }

    @Test fun `single repeat day a week ahead`() {
        val wednesdayOnly = 1 shl 2
        val next = AlarmTimeCalculator.nextTrigger(7, 0, wednesdayOnly, wednesday8am)
        assertEquals(ZonedDateTime.of(2025, 11, 12, 7, 0, 0, 0, zone), next)
    }

    @Test fun `mask helpers`() {
        var mask = 0
        mask = AlarmTimeCalculator.toggleDay(mask, 1)
        mask = AlarmTimeCalculator.toggleDay(mask, 7)
        assertTrue(AlarmTimeCalculator.isDayEnabled(mask, 1))
        assertTrue(AlarmTimeCalculator.isDayEnabled(mask, 7))
        assertFalse(AlarmTimeCalculator.isDayEnabled(mask, 3))
        mask = AlarmTimeCalculator.toggleDay(mask, 1)
        assertFalse(AlarmTimeCalculator.isDayEnabled(mask, 1))
    }
}
