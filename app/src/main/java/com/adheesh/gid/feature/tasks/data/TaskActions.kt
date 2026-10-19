package com.adheesh.gid.feature.tasks.data

import com.adheesh.gid.core.database.Priority
import com.adheesh.gid.core.database.TaskEntity
import com.adheesh.gid.core.database.TaskStatus
import com.adheesh.gid.core.util.newId
import com.adheesh.gid.core.util.nowMillis
import com.adheesh.gid.feature.tasks.domain.TaskRules

/** What the add/edit form produces. */
data class TaskDraft(
    val title            : String,
    val description      : String,
    val dateEpochDay     : Long,
    val timeMinutes      : Int?,
    val priority         : Priority,
    val estimatedMinutes : Int?,
)

// ── Task actions shared by Calendar, Home and the evening prompt ─────────────
// Each ViewModel calls these instead of repeating the SPEC §3 rules.
class TaskActions(private val tasks: TaskRepository) {

    suspend fun create(draft: TaskDraft) {
        val now = nowMillis()
        tasks.save(
            TaskEntity(
                id               = newId(),
                title            = draft.title.trim(),
                description      = draft.description.trim(),
                dateEpochDay     = draft.dateEpochDay,
                timeMinutes      = draft.timeMinutes,
                priority         = draft.priority.name,
                estimatedMinutes = draft.estimatedMinutes,
                progressPercent  = 0,
                status           = TaskStatus.PENDING.name,
                createdAt        = now,
                updatedAt        = now,
            )
        )
    }

    suspend fun update(task: TaskEntity, draft: TaskDraft) {
        tasks.save(
            task.copy(
                title            = draft.title.trim(),
                description      = draft.description.trim(),
                dateEpochDay     = draft.dateEpochDay,
                timeMinutes      = draft.timeMinutes,
                priority         = draft.priority.name,
                estimatedMinutes = draft.estimatedMinutes,
                updatedAt        = nowMillis(),
            )
        )
    }

    suspend fun setProgress(taskId: String, percent: Int) {
        val task = tasks.getTask(taskId) ?: return
        tasks.save(TaskRules.withProgress(task, percent, nowMillis()))
    }

    /** The checkbox: ticking sets 100%, unticking goes back to 0%. */
    suspend fun toggleDone(task: TaskEntity) =
        setProgress(task.id, if (task.progressPercent >= 100) 0 else 100)

    suspend fun setPriority(taskId: String, priority: Priority) {
        val task = tasks.getTask(taskId) ?: return
        tasks.save(task.copy(priority = priority.name, updatedAt = nowMillis()))
    }

    suspend fun delete(taskId: String) = tasks.delete(taskId)

    /** Returns the id of the new copy on the next day. */
    suspend fun moveToTomorrow(taskId: String): String? {
        val task = tasks.getTask(taskId) ?: return null
        if (tasks.isCarriedForward(task.id)) return null
        val copy = TaskRules.carryToTomorrow(task, nowMillis())
        tasks.save(copy)
        return copy.id
    }

    /** Evening prompt: carry every unfinished task of [epochDay] to the next day. */
    suspend fun moveUnfinishedToTomorrow(epochDay: Long): Int {
        val unfinished = tasks.getTasksForDay(epochDay)
            .filter { it.progressPercent < 100 && !tasks.isCarriedForward(it.id) }
        unfinished.forEach { tasks.save(TaskRules.carryToTomorrow(it, nowMillis())) }
        return unfinished.size
    }
}
