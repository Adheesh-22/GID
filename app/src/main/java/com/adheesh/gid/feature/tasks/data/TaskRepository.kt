package com.adheesh.gid.feature.tasks.data

import com.adheesh.gid.core.database.TaskEntity
import kotlinx.coroutines.flow.Flow

// ── TaskRepository interface ──────────────────────────────────────────────────
// Repositories are interfaces so a sync-backed V2 can swap the implementation
// without touching any screen code (RULES §4).
interface TaskRepository {
    fun observeTasksForDay(epochDay: Long): Flow<List<TaskEntity>>
    fun observeTasksForRange(fromDay: Long, toDay: Long): Flow<List<TaskEntity>>
    fun observeTask(id: String): Flow<TaskEntity?>
    fun observeTaskDays(fromDay: Long, toDay: Long): Flow<List<Long>>
    suspend fun getTasksForDay(epochDay: Long): List<TaskEntity>
    suspend fun isCarriedForward(id: String): Boolean
    suspend fun getTask(id: String): TaskEntity?
    suspend fun save(task: TaskEntity)
    suspend fun saveAll(tasks: List<TaskEntity>)
    suspend fun delete(id: String)
    suspend fun deleteAll()
}
