package com.adheesh.gid.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {

    /** Not done and not before [fromDay], soonest first; [sem] null = every semester. */
    @Query("SELECT * FROM exams WHERE completedAt IS NULL AND dateEpochDay >= :fromDay AND (:sem IS NULL OR subjectId IN (SELECT id FROM subjects WHERE semesterId IS NULL OR semesterId = :sem)) ORDER BY dateEpochDay ASC, timeMinutes ASC")
    fun observeUpcoming(fromDay: Long, sem: String?): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE completedAt IS NULL AND dateEpochDay >= :fromDay ORDER BY dateEpochDay ASC, timeMinutes ASC")
    suspend fun getUpcoming(fromDay: Long): List<ExamEntity>

    /** Done, or its day has passed; most recent first. */
    @Query("SELECT * FROM exams WHERE (completedAt IS NOT NULL OR dateEpochDay < :fromDay) AND (:sem IS NULL OR subjectId IN (SELECT id FROM subjects WHERE semesterId IS NULL OR semesterId = :sem)) ORDER BY dateEpochDay DESC")
    fun observePast(fromDay: Long, sem: String?): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE subjectId = :subjectId AND completedAt IS NULL AND dateEpochDay >= :fromDay ORDER BY dateEpochDay ASC, timeMinutes ASC")
    fun observeUpcomingForSubject(subjectId: String, fromDay: Long): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE dateEpochDay BETWEEN :fromDay AND :toDay AND (:sem IS NULL OR subjectId IN (SELECT id FROM subjects WHERE semesterId IS NULL OR semesterId = :sem)) ORDER BY dateEpochDay ASC, timeMinutes ASC")
    fun observeBetween(fromDay: Long, toDay: Long, sem: String?): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE id = :id")
    fun observe(id: String): Flow<ExamEntity?>

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun get(id: String): ExamEntity?

    @Query("SELECT * FROM exams WHERE importKey = :key LIMIT 1")
    suspend fun getByImportKey(key: String): ExamEntity?

    @Query("SELECT * FROM exams WHERE sourceCaptureId = :captureId LIMIT 1")
    suspend fun getBySource(captureId: String): ExamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(exam: ExamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(exams: List<ExamEntity>)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM exams WHERE subjectId = :subjectId")
    suspend fun deleteForSubject(subjectId: String)

    @Query("DELETE FROM exams")
    suspend fun deleteAll()
}
