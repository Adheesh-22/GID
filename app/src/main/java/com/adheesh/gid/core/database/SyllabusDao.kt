package com.adheesh.gid.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SyllabusDao {

    // ── Units ────────────────────────────────────────────────────────────────
    @Query("SELECT * FROM syllabus_units WHERE subjectId = :subjectId ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeUnits(subjectId: String): Flow<List<SyllabusUnitEntity>>

    @Query("SELECT * FROM syllabus_units WHERE subjectId = :subjectId ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    suspend fun getUnits(subjectId: String): List<SyllabusUnitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUnit(unit: SyllabusUnitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUnits(units: List<SyllabusUnitEntity>)

    @Query("DELETE FROM syllabus_units WHERE id = :id")
    suspend fun deleteUnit(id: String)

    // ── Topics ───────────────────────────────────────────────────────────────
    @Query("SELECT * FROM syllabus_topics WHERE subjectId = :subjectId ORDER BY sortOrder ASC")
    fun observeTopics(subjectId: String): Flow<List<SyllabusTopicEntity>>

    @Query("SELECT * FROM syllabus_topics WHERE unitId = :unitId ORDER BY sortOrder ASC")
    suspend fun getTopics(unitId: String): List<SyllabusTopicEntity>

    @Query("SELECT * FROM syllabus_topics WHERE id = :id")
    suspend fun getTopic(id: String): SyllabusTopicEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTopic(topic: SyllabusTopicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTopics(topics: List<SyllabusTopicEntity>)

    @Query("DELETE FROM syllabus_topics WHERE id = :id")
    suspend fun deleteTopic(id: String)

    @Query("DELETE FROM syllabus_topics WHERE unitId = :unitId")
    suspend fun deleteTopicsForUnit(unitId: String)

    // ── Exam links ───────────────────────────────────────────────────────────
    @Query("SELECT unitId FROM exam_units WHERE examId = :examId")
    fun observeExamUnitIds(examId: String): Flow<List<String>>

    @Query("SELECT unitId FROM exam_units WHERE examId = :examId")
    suspend fun getExamUnitIds(examId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamUnits(links: List<ExamUnitEntity>)

    @Query("DELETE FROM exam_units WHERE examId = :examId")
    suspend fun deleteExamUnits(examId: String)

    @Query("DELETE FROM exam_units WHERE unitId = :unitId")
    suspend fun deleteLinksForUnit(unitId: String)

    // ── Whole subject ────────────────────────────────────────────────────────
    @Query("DELETE FROM exam_units WHERE unitId IN (SELECT id FROM syllabus_units WHERE subjectId = :subjectId)")
    suspend fun deleteLinksForSubject(subjectId: String)

    @Query("DELETE FROM syllabus_topics WHERE subjectId = :subjectId")
    suspend fun deleteTopicsForSubject(subjectId: String)

    @Query("DELETE FROM syllabus_units WHERE subjectId = :subjectId")
    suspend fun deleteUnitsForSubject(subjectId: String)

    @Query("DELETE FROM exam_units")
    suspend fun deleteAllLinks()

    @Query("DELETE FROM syllabus_topics")
    suspend fun deleteAllTopics()

    @Query("DELETE FROM syllabus_units")
    suspend fun deleteAllUnits()
}
