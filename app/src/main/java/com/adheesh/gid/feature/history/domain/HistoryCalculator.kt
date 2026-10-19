package com.adheesh.gid.feature.history.domain

import com.adheesh.gid.core.database.ActivitySessionEntity
import com.adheesh.gid.core.database.TaskEntity
import com.adheesh.gid.feature.sessions.domain.SessionCalculator
import com.adheesh.gid.feature.sessions.domain.SessionRules
import com.adheesh.gid.feature.tasks.domain.ScoreCalculator
import java.time.LocalDate
import java.time.ZoneId

/** Everything the History screen shows for one day. */
data class DaySummary(
    val date          : LocalDate,
    val tasks         : List<TaskEntity>,
    /** 0.0–1.0, or null when the day had no tasks ("no data", never 0). */
    val score         : Double?,
    val sleep         : SessionCalculator.SleepTotals,
    val sleepSessions : List<ActivitySessionEntity>,
    val focusByType   : Map<String, Long>,
    val focusMinutes  : Long,
) {
    val hasSleepData: Boolean get() = sleepSessions.isNotEmpty()
}

/** One week (Mon–Sun) plus the comparison with the week before. */
data class WeekSummary(
    val weekStart         : LocalDate,
    val days              : List<DaySummary>,
    val weekScore         : Double?,
    val previousWeekScore : Double?,
    val change            : ScoreCalculator.WeekChange?,
    val tasksCompleted    : Int,
    val tasksRegistered   : Int,
    val averagePercent    : Double?,
    val focusMinutes      : Long,
)

// ── History maths – SPEC §2.4 / §3 ───────────────────────────────────────────
// Deterministic functions only (no AI). Built on ScoreCalculator and
// SessionCalculator from Phase 1.
object HistoryCalculator {

    /**
     * @param tasks    tasks from the previous week's Monday to this week's Sunday
     * @param sessions completed sessions that ended during this week
     */
    fun summarize(
        weekStart : LocalDate,
        tasks     : List<TaskEntity>,
        sessions  : List<ActivitySessionEntity>,
        zone      : ZoneId,
    ): WeekSummary {
        val tasksByDay    = tasks.groupBy { it.dateEpochDay }
        // Midnight rule: a session belongs to the day it ended.
        val sessionsByDay = sessions.groupBy { SessionRules.countsOnDay(it, zone) }

        val days = (0L..6L).map { offset ->
            val date        = weekStart.plusDays(offset)
            val dayTasks    = tasksByDay[date.toEpochDay()].orEmpty()
            val daySessions = sessionsByDay[date].orEmpty()
            val focus       = SessionCalculator.focusByType(daySessions)
            DaySummary(
                date          = date,
                tasks         = dayTasks,
                score         = ScoreCalculator.dayScore(dayTasks),
                sleep         = SessionCalculator.sleepTotals(daySessions),
                sleepSessions = daySessions.filter { it.category == "SLEEP" || it.category == "NAP" },
                focusByType   = focus,
                focusMinutes  = focus.values.sum(),
            )
        }

        val previousStart = weekStart.minusDays(7)
        val previousWeek  = (0L..6L).associate { offset ->
            val day = previousStart.plusDays(offset).toEpochDay()
            day to tasksByDay[day].orEmpty()
        }
        val thisWeekTasks = days.flatMap { it.tasks }

        val weekScore     = ScoreCalculator.weekScore(days.associate { it.date.toEpochDay() to it.tasks })
        val previousScore = ScoreCalculator.weekScore(previousWeek)

        return WeekSummary(
            weekStart         = weekStart,
            days              = days,
            weekScore         = weekScore,
            previousWeekScore = previousScore,
            change            = if (previousScore == null) null else ScoreCalculator.weekChange(weekScore, previousScore),
            tasksCompleted    = thisWeekTasks.count { it.progressPercent >= 100 },
            tasksRegistered   = thisWeekTasks.size,
            averagePercent    = if (thisWeekTasks.isEmpty()) null else thisWeekTasks.map { it.progressPercent }.average(),
            focusMinutes      = days.sumOf { it.focusMinutes },
        )
    }
}
