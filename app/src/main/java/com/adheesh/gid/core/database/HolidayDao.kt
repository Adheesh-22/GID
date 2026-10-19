package com.adheesh.gid.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {

    @Query("SELECT * FROM holidays ORDER BY dateEpochDay ASC")
    fun observeAll(): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays WHERE dateEpochDay = :epochDay LIMIT 1")
    suspend fun getHoliday(epochDay: Long): HolidayEntity?

    @Query("SELECT dateEpochDay FROM holidays")
    fun observeHolidayDays(): Flow<List<Long>>

    @Query("SELECT dateEpochDay FROM holidays WHERE dateEpochDay BETWEEN :from AND :to")
    suspend fun getHolidayDaysBetween(from: Long, to: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(holiday: HolidayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(holidays: List<HolidayEntity>)

    @Query("DELETE FROM holidays WHERE dateEpochDay = :epochDay")
    suspend fun deleteByDay(epochDay: Long)

    @Query("DELETE FROM holidays")
    suspend fun deleteAll()
}
