package com.adheesh.gid.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivitySessionDao {

    // Sessions that ENDED inside [fromMs, toMs) (SPEC §3: sleep counts on the
    // day the session ends). The caller converts local days to millis, so the
    // phone's time zone is respected instead of UTC days.
    @Query("""
        SELECT * FROM activity_sessions
        WHERE status = 'COMPLETED' AND endAt >= :fromMs AND endAt < :toMs
        ORDER BY startAt ASC
    """)
    fun observeEndedBetween(fromMs: Long, toMs: Long): Flow<List<ActivitySessionEntity>>

    @Query("SELECT * FROM activity_sessions WHERE status = 'RUNNING' ORDER BY startAt ASC")
    fun observeAllRunning(): Flow<List<ActivitySessionEntity>>

    @Query("SELECT * FROM activity_sessions WHERE status = 'RUNNING' ORDER BY startAt ASC")
    suspend fun getAllRunning(): List<ActivitySessionEntity>

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
