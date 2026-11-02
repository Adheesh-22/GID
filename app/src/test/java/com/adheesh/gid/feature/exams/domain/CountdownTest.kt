package com.adheesh.gid.feature.exams.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownTest {

    private fun of(days: Long) = Countdown.of(1_000 + days, 1_000)

    @Test fun `today and tomorrow are urgent`() {
        assertEquals(Countdown(Countdown.Unit.TODAY, 0, Urgency.NOW), of(0))
        assertEquals(Countdown(Countdown.Unit.TOMORROW, 1, Urgency.NOW), of(1))
    }

    @Test fun `within a week is soon, then later`() {
        assertEquals(Countdown(Countdown.Unit.DAYS, 7, Urgency.SOON), of(7))
        assertEquals(Countdown(Countdown.Unit.DAYS, 8, Urgency.LATER), of(8))
    }

    @Test fun `days up to 13, then whole weeks`() {
        assertEquals(Countdown(Countdown.Unit.DAYS, 13, Urgency.LATER), of(13))
        assertEquals(Countdown(Countdown.Unit.WEEKS, 2, Urgency.LATER), of(14))
        assertEquals(Countdown(Countdown.Unit.WEEKS, 3, Urgency.LATER), of(23))
    }

    @Test fun `past dates`() {
        assertEquals(Countdown(Countdown.Unit.PAST, 2, Urgency.PAST), of(-2))
    }
}
