package com.adheesh.gid.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profile LIMIT 1")
    fun observe(): Flow<LocalProfileEntity?>

    @Query("SELECT * FROM profile LIMIT 1")
    suspend fun get(): LocalProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: LocalProfileEntity)

    @Query("DELETE FROM profile")
    suspend fun deleteAll()
}
