package com.adheesh.gid.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** A subject plus how many sorted captures it holds (Library's subject list). */
data class SubjectWithCount(
    @Embedded val subject : SubjectEntity,
    val itemCount         : Int,
)

@Dao
interface SubjectDao {

    /** [sem] null = every semester. A subject with no semester yet shows under all of them. */
    @Query("SELECT * FROM subjects WHERE archived = 0 AND (:sem IS NULL OR semesterId IS NULL OR semesterId = :sem) ORDER BY name COLLATE NOCASE ASC")
    fun observeActive(sem: String?): Flow<List<SubjectEntity>>

    @Query(
        """
        SELECT subjects.*, (SELECT COUNT(*) FROM captures
                            WHERE captures.subjectId = subjects.id AND captures.status = 'SORTED') AS itemCount
        FROM subjects WHERE archived = 0 AND (:sem IS NULL OR semesterId IS NULL OR semesterId = :sem) ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun observeActiveWithCounts(sem: String?): Flow<List<SubjectWithCount>>

    @Query("SELECT * FROM subjects WHERE archived = 1 AND (:sem IS NULL OR semesterId IS NULL OR semesterId = :sem) ORDER BY name COLLATE NOCASE ASC")
    fun observeArchived(sem: String?): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects")
    fun observeAll(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    fun observe(id: String): Flow<SubjectEntity?>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun get(id: String): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE nameKey = :nameKey LIMIT 1")
    suspend fun getByKey(nameKey: String): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE nameKey = :nameKey AND semesterId = :sem LIMIT 1")
    suspend fun getByKeyIn(nameKey: String, sem: String): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE nameKey = :nameKey AND semesterId IS NULL LIMIT 1")
    suspend fun getByKeyUnassigned(nameKey: String): SubjectEntity?

    @Query("SELECT * FROM subjects")
    suspend fun getAll(): List<SubjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subject: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(subjects: List<SubjectEntity>)

    @Query("UPDATE subjects SET attendanceTarget = :target, updatedAt = :now WHERE id = :id")
    suspend fun setAttendanceTarget(id: String, target: Int?, now: Long)

    @Query("UPDATE subjects SET priorPresent = :present, priorTotal = :total, updatedAt = :now WHERE id = :id")
    suspend fun setPriorAttendance(id: String, present: Int, total: Int, now: Long)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM subjects")
    suspend fun deleteAll()
}
