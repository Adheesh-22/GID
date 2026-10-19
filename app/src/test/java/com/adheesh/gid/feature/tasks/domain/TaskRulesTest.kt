package com.adheesh.gid.feature.tasks.domain

import com.adheesh.gid.core.database.TaskEntity
import com.adheesh.gid.core.database.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskRulesTest {

    private fun task(percent: Int = 0, day: Long = 100, completedAt: Long? = null) = TaskEntity(
        id = "t1", title = "Essay", dateEpochDay = day, priority = "HIGH",
        progressPercent = percent, status = TaskRules.statusFor(percent).name,
        completedAt = completedAt, createdAt = 0, updatedAt = 0,
    )

    @Test fun `percent drives status`() {
        assertEquals(TaskStatus.PENDING, TaskRules.statusFor(0))
        assertEquals(TaskStatus.IN_PROGRESS, TaskRules.statusFor(1))
        assertEquals(TaskStatus.IN_PROGRESS, TaskRules.statusFor(99))
        assertEquals(TaskStatus.DONE, TaskRules.statusFor(100))
    }

    @Test fun `reaching 100 sets completedAt and dropping below clears it`() {
        val done = TaskRules.withProgress(task(), 100, now = 5_000)
        assertEquals("DONE", done.status)
        assertEquals(5_000L, done.completedAt)

        val reopened = TaskRules.withProgress(done, 40, now = 9_000)
        assertEquals("IN_PROGRESS", reopened.status)
        assertNull(reopened.completedAt)
    }

    @Test fun `completedAt keeps the first completion time`() {
        val done = TaskRules.withProgress(task(100, completedAt = 1_000), 100, now = 9_000)
        assertEquals(1_000L, done.completedAt)
    }

    @Test fun `progress is clamped to 0-100`() {
        assertEquals(100, TaskRules.withProgress(task(), 140, 0).progressPercent)
        assertEquals(0, TaskRules.withProgress(task(), -5, 0).progressPercent)
    }

    @Test fun `move to tomorrow makes a new task and leaves the original alone`() {
        val original = task(percent = 60, day = 200)
        val copy = TaskRules.carryToTomorrow(original, now = 7_000, newId = "t2")
        assertEquals("t2", copy.id)
        assertNotEquals(original.id, copy.id)
        assertEquals(201L, copy.dateEpochDay)
        assertEquals("t1", copy.carriedFromId)
        assertEquals(60, copy.progressPercent)
        // The original is not modified by the function.
        assertEquals(200L, original.dateEpochDay)
        assertEquals(60, original.progressPercent)
    }

    @Test fun `overdue means an earlier day and under 100`() {
        assertTrue(TaskRules.isOverdue(task(50, day = 10), todayEpochDay = 11))
        assertFalse(TaskRules.isOverdue(task(100, day = 10), todayEpochDay = 11))
        assertFalse(TaskRules.isOverdue(task(0, day = 11), todayEpochDay = 11))
    }
}
