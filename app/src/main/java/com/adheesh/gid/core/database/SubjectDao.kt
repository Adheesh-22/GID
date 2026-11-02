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

    @Query("SELECT * FROM subjects WHERE archived = 0 ORDER BY name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<SubjectEntity>>

    @Query(
        """
        SELECT subjects.*, (SELECT COUNT(*) FROM captures
                            WHERE captures.subjectId = subjects.id AND captures.status = 'SORTED') AS itemCount
        FROM subjects WHERE archived = 0 ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun observeActiveWithCounts(): Flow<List<SubjectWithCount>>

    @Query("SELECT * FROM subjects WHERE archived = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeArchived(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects")
    fun observeAll(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    fun observe(id: String): Flow<SubjectEntity?>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun get(id: String): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE nameKey = :nameKey LIMIT 1")
    suspend fun getByKey(nameKey: String): SubjectEntity?

    @Query("SELECT * FROM subjects")
    suspend fun getAll(): List<SubjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subject: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(subjects: List<SubjectEntity>)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM subjects")
    suspend fun deleteAll()
}
