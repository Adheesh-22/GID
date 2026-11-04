package com.adheesh.gid.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {

    @Query("UPDATE timetable_entries SET subjectId = NULL WHERE subjectId = :subjectId")
    suspend fun unlinkSubject(subjectId: String)

    @Query("SELECT * FROM timetable_entries ORDER BY dayOfWeek ASC, startMinutes ASC")
    fun observeAll(): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE dayOfWeek = :dayOfWeek AND enabled = 1 ORDER BY startMinutes ASC")
    fun observeForDay(dayOfWeek: Int): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE id = :id")
    suspend fun get(id: String): TimetableEntryEntity?

    @Query("SELECT * FROM timetable_entries WHERE enabled = 1 ORDER BY dayOfWeek ASC, startMinutes ASC")
    suspend fun getAllEnabled(): List<TimetableEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: TimetableEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<TimetableEntryEntity>)

    @Delete
    suspend fun delete(entry: TimetableEntryEntity)

    @Query("DELETE FROM timetable_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM timetable_entries")
    suspend fun deleteAll()

    /** All or nothing, so a failed import can't leave half a timetable. */
    @Transaction
    suspend fun replaceAll(entries: List<TimetableEntryEntity>) {
        deleteAll()
        upsertAll(entries)
    }
}
