package com.adheesh.gid.feature.sessions.domain

import com.adheesh.gid.core.database.SessionSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class SessionRulesTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(day: Int, hour: Int, minute: Int = 0) =
        ZonedDateTime.of(2025, 11, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    @Test fun `start creates a running session`() {
        val s = SessionRules.start("sleep", "SLEEP", SessionSource.ALARM, now = 1_000)
        assertEquals("RUNNING", s.status)
        assertNull(s.endAt)
        assertEquals(1_000L, s.startAt)
    }

    @Test fun `closing records the real duration`() {
        val s = SessionRules.start("study", "FOCUS", SessionSource.TIMER, now = at(5, 10), plannedMinutes = 45)
        val closed = SessionRules.close(s, now = at(5, 10, 20))
        assertEquals("COMPLETED", closed.status)
        assertEquals(at(5, 10, 20), closed.endAt)
        assertEquals(at(5, 10, 45), SessionRules.endsAt(s))
    }

    @Test fun `closing never ends before it started`() {
        val s = SessionRules.start("nap", "NAP", SessionSource.MANUAL, now = 5_000)
        assertEquals(5_000L, SessionRules.close(s, now = 1_000).endAt)
    }

    @Test fun `sleep crossing midnight counts on the day it ends`() {
        val s = SessionRules.start("sleep", "SLEEP", SessionSource.ALARM, now = at(4, 23, 30))
        val closed = SessionRules.close(s, now = at(5, 7))
        assertEquals(LocalDate.of(2025, 11, 5), SessionRules.countsOnDay(closed, zone))
    }

    @Test fun `running session has no day yet`() {
        val s = SessionRules.start("sleep", "SLEEP", SessionSource.ALARM, now = at(4, 23))
        assertNull(SessionRules.countsOnDay(s, zone))
    }

    @Test fun `sessions over 16 hours are closed at the 16 hour mark and flagged`() {
        val s = SessionRules.start("sleep", "SLEEP", SessionSource.ALARM, now = at(4, 22))
        val closed = SessionRules.autoCloseIfStale(s, now = at(5, 20))!!
        assertTrue(closed.flagged)
        assertEquals("COMPLETED", closed.status)
        assertEquals(at(5, 14), closed.endAt)
    }

    @Test fun `sessions under 16 hours are left running`() {
        val s = SessionRules.start("sleep", "SLEEP", SessionSource.ALARM, now = at(4, 22))
        assertNull(SessionRules.autoCloseIfStale(s, now = at(5, 7)))
        val closed = SessionRules.close(s, at(5, 7))
        assertNull(SessionRules.autoCloseIfStale(closed, now = at(6, 7)))
        assertFalse(closed.flagged)
    }
}
