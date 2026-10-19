package com.adheesh.gid.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivitySessionDao {

    // Observe all sessions that ended on a given epoch day.
    // SPEC §3: "Sleep counts on the day the session ENDS."
    // endAt / 86400000 gives the epoch-day (UTC); we use integer division.
    @Query("""
        SELECT * FROM activity_sessions
        WHERE (endAt / 86400000) = :epochDay
           OR (status = 'RUNNING' AND (startAt / 86400000) = :epochDay)
        ORDER BY startAt ASC
    """)
    fun observeSessionsForDay(epochDay: Long): Flow<List<ActivitySessionEntity>>

    // Sessions in an epoch-day range (for history week view)
    @Query("""
        SELECT * FROM activity_sessions
        WHERE (endAt / 86400000) BETWEEN :fromDay AND :toDay
           OR (status = 'RUNNING' AND (startAt / 86400000) BETWEEN :fromDay AND :toDay)
        ORDER BY startAt ASC
    """)
    fun observeSessionsForRange(fromDay: Long, toDay: Long): Flow<List<ActivitySessionEntity>>

    // Currently running session (should be at most one at a time)
    @Query("SELECT * FROM activity_sessions WHERE status = 'RUNNING' LIMIT 1")
    fun observeRunningSession(): Flow<ActivitySessionEntity?>

    @Query("SELECT * FROM activity_sessions WHERE id = :id")
    suspend fun getById(id: String): ActivitySessionEntity?

    // Find sessions running for > 16 h that haven't been auto-closed yet
    @Query("""
        SELECT * FROM activity_sessions
        WHERE status = 'RUNNING'
          AND (:nowMs - startAt) > 57600000
    """) // 57600000 ms = 16 h
    suspend fun getOverdueRunning(nowMs: Long): List<ActivitySessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ActivitySessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sessions: List<ActivitySessionEntity>)

    @Delete
    suspend fun delete(session: ActivitySessionEntity)

    @Query("DELETE FROM activity_sessions")
    suspend fun deleteAll()
}
