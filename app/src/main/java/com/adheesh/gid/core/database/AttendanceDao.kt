package com.adheesh.gid.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Query("SELECT subjectId, status, COUNT(*) AS count FROM attendance GROUP BY subjectId, status")
    fun observeCounts(): Flow<List<AttendanceCount>>

    @Query("SELECT subjectId, status, COUNT(*) AS count FROM attendance WHERE subjectId = :subjectId GROUP BY status")
    suspend fun countsForSubject(subjectId: String): List<AttendanceCount>

    @Query("SELECT * FROM attendance WHERE dateEpochDay = :day")
    fun observeForDay(day: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE dateEpochDay BETWEEN :fromDay AND :toDay")
    suspend fun getBetween(fromDay: Long, toDay: Long): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE timetableEntryId = :entryId AND dateEpochDay = :day AND startMinutes = :startMinutes")
    suspend fun getFor(entryId: String, day: Long, startMinutes: Int): AttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mark: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(marks: List<AttendanceEntity>)

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM attendance WHERE subjectId = :subjectId")
    suspend fun deleteForSubject(subjectId: String)

    @Query("DELETE FROM attendance")
    suspend fun deleteAll()
}
