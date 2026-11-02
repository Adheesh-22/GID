package com.adheesh.gid.feature.exams.domain

import com.adheesh.gid.core.database.ExamEntity
import com.adheesh.gid.core.database.ExamKind
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

enum class ExamReminderStep { WEEK_BEFORE, DAY_BEFORE, DAY_OF }

/** One exam reminder to schedule. */
data class PlannedExamReminder(
    val triggerAt : Long,   // epoch millis
    val examId    : String,
    val step      : ExamReminderStep,
)

// ── Exam reminders (SPEC §3 Exams) ───────────────────────────────────────────
// Exams and tests: 19:00 a week before, 19:00 the day before, 07:00 on the day
// (or 2 hours before the start when that's earlier). Assignments and projects
// skip the week-before one. Nothing for done items or times already past.
object ExamReminderPlanner {

    val EVENING: LocalTime = LocalTime.of(19, 0)
    val MORNING: LocalTime = LocalTime.of(7, 0)
    const val HOURS_BEFORE_START = 2L
    const val MAX_REMINDERS = 60

    fun plan(exams: List<ExamEntity>, now: ZonedDateTime): List<PlannedExamReminder> {
        val nowMs = now.toInstant().toEpochMilli()
        return exams
            .filter { it.completedAt == null }
            .flatMap { exam -> remindersFor(exam, now) }
            .filter { it.triggerAt > nowMs }
            .sortedBy { it.triggerAt }
            .take(MAX_REMINDERS)
    }

    private fun remindersFor(exam: ExamEntity, now: ZonedDateTime): List<PlannedExamReminder> {
        val zone = now.zone
        val day = LocalDate.ofEpochDay(exam.dateEpochDay)
        fun at(date: LocalDate, time: LocalTime) = ZonedDateTime.of(date, time, zone).toInstant().toEpochMilli()

        // 07:00, unless the exam starts so early that 2 hours before is earlier still.
        val dayOf = exam.timeMinutes
            ?.let { ZonedDateTime.of(day, LocalTime.of(it / 60, it % 60), zone).minusHours(HOURS_BEFORE_START).toInstant().toEpochMilli() }
            ?.let { minOf(it, at(day, MORNING)) }
            ?: at(day, MORNING)

        val steps = buildList {
            if (!ExamKind.of(exam.kind).isSubmitted) add(ExamReminderStep.WEEK_BEFORE to at(day.minusDays(7), EVENING))
            add(ExamReminderStep.DAY_BEFORE to at(day.minusDays(1), EVENING))
            add(ExamReminderStep.DAY_OF to dayOf)
        }
        return steps.map { (step, trigger) -> PlannedExamReminder(trigger, exam.id, step) }
    }
}
