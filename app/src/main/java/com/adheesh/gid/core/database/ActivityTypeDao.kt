package com.adheesh.gid.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityTypeDao {

    @Query("SELECT * FROM activity_types ORDER BY name ASC")
    fun observeAll(): Flow<List<ActivityTypeEntity>>

    @Query("SELECT * FROM activity_types WHERE id = :id")
    suspend fun getById(id: String): ActivityTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(type: ActivityTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(types: List<ActivityTypeEntity>)

    // Guard: default types cannot be deleted (enforced in the repository layer)
    @Delete
    suspend fun delete(type: ActivityTypeEntity)

    @Query("DELETE FROM activity_types WHERE id = :id AND isDefault = 0")
    suspend fun deleteNonDefault(id: String)

    @Query("SELECT COUNT(*) FROM activity_types")
    suspend fun count(): Int

    @Query("DELETE FROM activity_types")
    suspend fun deleteAll()
}
