package com.adheesh.gid.feature.tasks.domain

import com.adheesh.gid.core.database.TaskEntity

// ── Score calculation – SPEC §3 ───────────────────────────────────────────────
// Weights: HIGH=3, MEDIUM=2, LOW=1
// DayScore  = Σ(weight × percentDone) / Σ(weight)  → null if no tasks
// WeekScore = average of non-null DayScores         → null if all days are null
//
// These are plain Kotlin objects with no Android or Coroutine dependencies so
// they are trivially testable with standard JUnit (no Robolectric needed).

object ScoreCalculator {

    // Map a priority string to its numeric weight
    private fun weight(priority: String) = when (priority.uppercase()) {
        "HIGH"   -> 3
        "MEDIUM" -> 2
        else     -> 1   // LOW and anything unknown
    }

    /**
     * Calculate the productivity score (0.0–1.0) for a list of tasks on one day.
     * Returns null when [tasks] is empty (shown as "no data" in the UI).
     */
    fun dayScore(tasks: List<TaskEntity>): Double? {
        if (tasks.isEmpty()) return null

        var weightedSum = 0.0
        var totalWeight = 0.0
        for (task in tasks) {
            val w = weight(task.priority).toDouble()
            weightedSum += w * task.progressPercent / 100.0
            totalWeight += w
        }
        return if (totalWeight == 0.0) null else weightedSum / totalWeight
    }

    /**
     * Weekly score: average of the non-null day scores for the given week.
     * [tasksByDay] maps epochDay → list of tasks for that day.
     * Returns null when every day has no data.
     */
    fun weekScore(tasksByDay: Map<Long, List<TaskEntity>>): Double? {
        val nonNullScores = tasksByDay.values
            .mapNotNull { dayScore(it) }
        return if (nonNullScores.isEmpty()) null
        else nonNullScores.average()
    }

    /**
     * Change vs previous week, in points (0–100 scale) and percent change.
     * Returns null for either value if data is missing.
     */
    data class WeekChange(val points: Double, val percentChange: Double?)

    fun weekChange(thisWeek: Double?, lastWeek: Double?): WeekChange? {
        thisWeek  ?: return null
        val pts = (thisWeek - (lastWeek ?: 0.0)) * 100.0
        val pct = if (lastWeek != null && lastWeek != 0.0)
            (thisWeek - lastWeek) / lastWeek * 100.0 else null
        return WeekChange(pts, pct)
    }
}
