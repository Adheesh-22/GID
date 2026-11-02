package com.adheesh.gid.feature.exams.domain

import com.adheesh.gid.core.database.ExamEntity
import com.adheesh.gid.core.database.ExamKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ExamReminderPlannerTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    // Monday 3 Nov 2025, 08:00
    private val now = ZonedDateTime.of(2025, 11, 3, 8, 0, 0, 0, zone)
    private val today = now.toLocalDate()

    private fun exam(
        inDays    : Long,
        kind      : ExamKind = ExamKind.EXAM,
        time      : Int?     = null,
        completed : Boolean  = false,
    ) = ExamEntity(
        id = "x$inDays$kind", subjectId = "s", title = "Mid-sem", kind = kind.name,
        dateEpochDay = today.plusDays(inDays).toEpochDay(), timeMinutes = time,
        completedAt = if (completed) 1L else null, createdAt = 0, updatedAt = 0,
    )

    private fun ms(date: LocalDate, h: Int, m: Int = 0) =
        ZonedDateTime.of(date, java.time.LocalTime.of(h, m), zone).toInstant().toEpochMilli()

    @Test fun `exam gets week-before, day-before and morning reminders`() {
        val plan = ExamReminderPlanner.plan(listOf(exam(10)), now)
        val day = today.plusDays(10)
        assertEquals(
            listOf(ms(day.minusDays(7), 19), ms(day.minusDays(1), 19), ms(day, 7)),
            plan.map { it.triggerAt },
        )
        assertEquals(listOf(ExamReminderStep.WEEK_BEFORE, ExamReminderStep.DAY_BEFORE, ExamReminderStep.DAY_OF), plan.map { it.step })
    }

    @Test fun `assignment skips the week-before reminder`() {
        val plan = ExamReminderPlanner.plan(listOf(exam(10, ExamKind.ASSIGNMENT)), now)
        assertEquals(listOf(ExamReminderStep.DAY_BEFORE, ExamReminderStep.DAY_OF), plan.map { it.step })
    }

    @Test fun `early exam is reminded two hours before it starts`() {
        val plan = ExamReminderPlanner.plan(listOf(exam(10, time = 8 * 60)), now)
        assertEquals(ms(today.plusDays(10), 6), plan.last().triggerAt)
    }

    @Test fun `late exam still gets the 07 00 reminder`() {
        val plan = ExamReminderPlanner.plan(listOf(exam(10, time = 14 * 60)), now)
        assertEquals(ms(today.plusDays(10), 7), plan.last().triggerAt)
    }

    @Test fun `past reminders are skipped`() {
        // Exam in 3 days: the week-before reminder is already past.
        val plan = ExamReminderPlanner.plan(listOf(exam(3)), now)
        assertEquals(listOf(ExamReminderStep.DAY_BEFORE, ExamReminderStep.DAY_OF), plan.map { it.step })
        // Exam today at 07:00 has already been reminded (it's 08:00 now).
        assertTrue(ExamReminderPlanner.plan(listOf(exam(0)), now).isEmpty())
    }

    @Test fun `done items get no reminders`() {
        assertTrue(ExamReminderPlanner.plan(listOf(exam(10, completed = true)), now).isEmpty())
    }

    @Test fun `reminders are sorted and capped`() {
        val many = (8L..60L).map { exam(it) }
        val plan = ExamReminderPlanner.plan(many, now)
        assertEquals(ExamReminderPlanner.MAX_REMINDERS, plan.size)
        assertEquals(plan.sortedBy { it.triggerAt }, plan)
    }
}
