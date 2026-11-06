package com.adheesh.gid.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {

    @Query("UPDATE timetable_entries SET subjectId = NULL WHERE subjectId = :subjectId")
    suspend fun unlinkSubject(subjectId: String)

    @Query("SELECT * FROM timetable_entries ORDER BY dayOfWeek ASC, startMinutes ASC")
    fun observeAll(): Flow<List<TimetableEntryEntity>>

    /** [sem] null = every semester. */
    @Query("SELECT * FROM timetable_entries WHERE (:sem IS NULL OR semesterId IS NULL OR semesterId = :sem) ORDER BY dayOfWeek ASC, startMinutes ASC")
    fun observeIn(sem: String?): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE dayOfWeek = :dayOfWeek AND enabled = 1 ORDER BY startMinutes ASC")
    fun observeForDay(dayOfWeek: Int): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE id = :id")
    suspend fun get(id: String): TimetableEntryEntity?

    @Query("SELECT * FROM timetable_entries WHERE enabled = 1 AND (:sem IS NULL OR semesterId IS NULL OR semesterId = :sem) ORDER BY dayOfWeek ASC, startMinutes ASC")
    suspend fun getEnabledIn(sem: String?): List<TimetableEntryEntity>

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

    @Query("DELETE FROM timetable_entries WHERE semesterId = :sem")
    suspend fun deleteIn(sem: String)

    /** Classes from before semesters existed, if they are one of these courses. */
    @Query("DELETE FROM timetable_entries WHERE semesterId IS NULL AND lower(trim(subject)) IN (:subjectKeys)")
    suspend fun deleteUnassigned(subjectKeys: List<String>)

    /** All or nothing, so a failed import can't leave half a timetable. Other semesters stay. */
    @Transaction
    suspend fun replaceSemester(sem: String, subjectKeys: List<String>, entries: List<TimetableEntryEntity>) {
        deleteIn(sem)
        deleteUnassigned(subjectKeys)
        upsertAll(entries)
    }
}
