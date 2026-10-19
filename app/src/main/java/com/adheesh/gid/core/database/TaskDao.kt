package com.adheesh.gid.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // Observe all tasks on a given calendar day (epochDay), ordered by priority
    // desc then time asc.  The query uses CASE to turn string priorities into
    // sortable numbers; HIGH=3, MEDIUM=2, LOW=1.
    @Query("""
        SELECT * FROM tasks
        WHERE dateEpochDay = :epochDay
        ORDER BY
            CASE priority WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 ELSE 1 END DESC,
            COALESCE(timeMinutes, 99999) ASC
    """)
    fun observeTasksForDay(epochDay: Long): Flow<List<TaskEntity>>

    // All tasks between two epochDays inclusive (used for week views)
    @Query("SELECT * FROM tasks WHERE dateEpochDay BETWEEN :from AND :to ORDER BY dateEpochDay ASC")
    fun observeTasksForRange(from: Long, to: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeTask(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTask(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}
