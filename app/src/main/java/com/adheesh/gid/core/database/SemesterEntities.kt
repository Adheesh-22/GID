package com.adheesh.gid.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// ── Semester (SPEC §2.14) ─────────────────────────────────────────────────────
// Subjects belong to one semester, and everything else follows the subject.
// sortKey orders them: Fall 2026-27 = 20261, Winter 2026-27 = 20262.
@Entity(tableName = "semesters", indices = [Index(value = ["name"], unique = true)])
data class SemesterEntity(
    @PrimaryKey val id : String,
    val name           : String,
    val sortKey        : Int,
    val createdAt      : Long,
    val updatedAt      : Long,
)

// ── Marks (SPEC §2.15) ────────────────────────────────────────────────────────
/**
 * One marked part of a subject, e.g. its Embedded Theory or its Embedded Lab.
 * credits decides its share of the subject (3 and 1 = 75% and 25%).
 * expected: what the student expects from the marks not uploaded yet.
 */
@Entity(
    tableName = "mark_components",
    indices   = [
        Index(value = ["semesterId", "courseCode", "courseType"], unique = true),
        Index(value = ["subjectId"]),
    ],
)
data class MarkComponentEntity(
    @PrimaryKey val id : String,
    val semesterId     : String?,
    val subjectId      : String,
    val courseCode     : String,
    val courseType     : String,
    val credits        : Double?  = null,
    val expected       : Double?  = null,
    val createdAt      : Long,
    val updatedAt      : Long,
)

/** One row of VTOP's Marks View. */
@Entity(tableName = "mark_entries", indices = [Index(value = ["componentId"])])
data class MarkEntryEntity(
    @PrimaryKey val id : String,
    val componentId    : String,
    val title          : String,
    val maxMark        : Double,
    val weightage      : Double,
    val status         : String,
    val scoredMark     : Double?,
    val weightageMark  : Double?,
    val remark         : String,
    val sortOrder      : Int,
    val createdAt      : Long,
    val updatedAt      : Long,
)

@Dao
interface SemesterDao {

    /** Newest first. */
    @Query("SELECT * FROM semesters ORDER BY sortKey DESC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByName(name: String): SemesterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(semester: SemesterEntity)
}

@Dao
interface MarksDao {

    @Query("SELECT * FROM mark_components")
    fun observeComponents(): Flow<List<MarkComponentEntity>>

    @Query("SELECT * FROM mark_entries ORDER BY sortOrder ASC")
    fun observeEntries(): Flow<List<MarkEntryEntity>>

    @Query("SELECT * FROM mark_components WHERE subjectId = :subjectId AND courseCode = :code AND courseType = :type LIMIT 1")
    suspend fun findComponent(subjectId: String, code: String, type: String): MarkComponentEntity?

    @Query("SELECT * FROM mark_components WHERE id = :id")
    suspend fun getComponent(id: String): MarkComponentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComponent(component: MarkComponentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<MarkEntryEntity>)

    @Query("DELETE FROM mark_entries WHERE componentId = :componentId")
    suspend fun deleteEntries(componentId: String)

    /** A fresh paste replaces what VTOP said before; credits and expectations stay. */
    @Transaction
    suspend fun replaceEntries(componentId: String, entries: List<MarkEntryEntity>) {
        deleteEntries(componentId)
        insertEntries(entries)
    }

    @Query("DELETE FROM mark_entries WHERE componentId IN (SELECT id FROM mark_components WHERE subjectId = :subjectId)")
    suspend fun deleteEntriesForSubject(subjectId: String)

    @Query("DELETE FROM mark_components WHERE subjectId = :subjectId")
    suspend fun deleteComponentsForSubject(subjectId: String)
}
