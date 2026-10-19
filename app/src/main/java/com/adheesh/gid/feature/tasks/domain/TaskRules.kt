package com.adheesh.gid.feature.tasks.domain

import com.adheesh.gid.core.database.TaskEntity
import com.adheesh.gid.core.database.TaskStatus
import java.util.UUID

// ── Task rules – SPEC §3 ─────────────────────────────────────────────────────
// Plain Kotlin so they can be unit tested without Android.
object TaskRules {

    /** 0 = PENDING, 1–99 = IN_PROGRESS, 100 = DONE. */
    fun statusFor(percent: Int): TaskStatus = when {
        percent >= 100 -> TaskStatus.DONE
        percent <= 0   -> TaskStatus.PENDING
        else           -> TaskStatus.IN_PROGRESS
    }

    /** Applies a new percent; completedAt is set when the task first reaches 100. */
    fun withProgress(task: TaskEntity, percent: Int, now: Long): TaskEntity {
        val clamped = percent.coerceIn(0, 100)
        val status  = statusFor(clamped)
        return task.copy(
            progressPercent = clamped,
            status          = status.name,
            completedAt     = if (status == TaskStatus.DONE) task.completedAt ?: now else null,
            updatedAt       = now,
        )
    }

    /**
     * 'Move to tomorrow': a NEW task on the next day pointing back at the
     * original. The original is left untouched so its day's history stays true.
     * The copy keeps the percent reached so far, so the user carries on from it.
     */
    fun carryToTomorrow(task: TaskEntity, now: Long, newId: String = UUID.randomUUID().toString()): TaskEntity =
        task.copy(
            id            = newId,
            dateEpochDay  = task.dateEpochDay + 1,
            carriedFromId = task.id,
            completedAt   = null,
            createdAt     = now,
            updatedAt     = now,
        )

    /** Overdue = date before today and under 100%. */
    fun isOverdue(task: TaskEntity, todayEpochDay: Long): Boolean =
        task.dateEpochDay < todayEpochDay && task.progressPercent < 100
}
