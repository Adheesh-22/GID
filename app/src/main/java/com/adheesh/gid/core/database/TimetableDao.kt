package com.adheesh.gid.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {

    @Query("SELECT * FROM timetable_entries ORDER BY dayOfWeek ASC, startMinutes ASC")
    fun observeAll(): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE dayOfWeek = :dayOfWeek AND enabled = 1 ORDER BY startMinutes ASC")
    fun observeForDay(dayOfWeek: Int): Flow<List<TimetableEntryEntity>>

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
}
