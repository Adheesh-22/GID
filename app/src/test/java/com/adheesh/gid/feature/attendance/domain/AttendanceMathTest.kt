package com.adheesh.gid.feature.attendance.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttendanceMathTest {

    @Test fun `no classes yet has no percent`() {
        val s = AttendanceMath.summary(present = 0, absent = 0, target = 75)
        assertNull(s.percent)
        assertEquals(AttendanceLevel.NONE, s.level)
        assertEquals(0, s.canMiss)
        assertEquals(0, s.mustAttend)
    }

    @Test fun `above target says how many more can be missed`() {
        // 20 of 24 = 83%. 20 of 26 = 76.9% is fine, 20 of 27 = 74% is not.
        val s = AttendanceMath.summary(present = 20, absent = 4, target = 75)
        assertEquals(83, s.percent)
        assertEquals(2, s.canMiss)
        assertEquals(0, s.mustAttend)
        assertEquals(AttendanceLevel.SAFE, s.level)
    }

    @Test fun `below target says how many in a row are needed`() {
        // 6 of 10 = 60%. Six more presents make 12 of 16 = 75%.
        val s = AttendanceMath.summary(present = 6, absent = 4, target = 75)
        assertEquals(60, s.percent)
        assertEquals(0, s.canMiss)
        assertEquals(6, s.mustAttend)
        assertEquals(AttendanceLevel.BELOW, s.level)
    }

    @Test fun `just above target is close`() {
        val s = AttendanceMath.summary(present = 780, absent = 220, target = 75)   // 78%
        assertEquals(AttendanceLevel.CLOSE, s.level)
    }

    @Test fun `before GID counts are included`() {
        // 2 of 2 marked in GID, plus 18 of 22 before: 20 of 24.
        val s = AttendanceMath.summary(present = 2, absent = 0, target = 75, priorPresent = 18, priorTotal = 22)
        assertEquals(20, s.present)
        assertEquals(24, s.total)
        assertEquals(2, s.canMiss)
    }

    @Test fun `a 100 percent target can't be reached after a miss`() {
        val s = AttendanceMath.summary(present = 9, absent = 1, target = 100)
        assertNull(s.mustAttend)
        assertEquals(0, s.canMiss)
    }

    @Test fun `a perfect record at 100 percent target can miss none`() {
        val s = AttendanceMath.summary(present = 5, absent = 0, target = 100)
        assertEquals(0, s.canMiss)
        assertEquals(0, s.mustAttend)
    }
}
