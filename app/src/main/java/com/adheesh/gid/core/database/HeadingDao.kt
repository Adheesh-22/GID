package com.adheesh.gid.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HeadingDao {

    @Query("SELECT * FROM headings WHERE subjectId = :subjectId ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeForSubject(subjectId: String): Flow<List<HeadingEntity>>

    @Query("SELECT * FROM headings WHERE subjectId = :subjectId ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    suspend fun getForSubject(subjectId: String): List<HeadingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(heading: HeadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(headings: List<HeadingEntity>)

    @Query("DELETE FROM headings WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM headings")
    suspend fun deleteAll()
}
