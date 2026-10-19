package com.adheesh.gid.feature.calendar.data

import com.adheesh.gid.core.database.HolidayDao
import com.adheesh.gid.core.database.HolidayEntity
import kotlinx.coroutines.flow.Flow

interface HolidayRepository {
    fun observeAll(): Flow<List<HolidayEntity>>
    fun observeHolidayDays(): Flow<List<Long>>
    suspend fun getHoliday(epochDay: Long): HolidayEntity?
    suspend fun getHolidayDaysBetween(fromDay: Long, toDay: Long): List<Long>
    suspend fun save(holiday: HolidayEntity)
    suspend fun saveAll(holidays: List<HolidayEntity>)
    suspend fun delete(epochDay: Long)
    suspend fun deleteAll()
}

class HolidayRepositoryImpl(private val dao: HolidayDao) : HolidayRepository {
    override fun observeAll()                                = dao.observeAll()
    override fun observeHolidayDays()                        = dao.observeHolidayDays()
    override suspend fun getHoliday(epochDay: Long)          = dao.getHoliday(epochDay)
    override suspend fun getHolidayDaysBetween(fromDay: Long, toDay: Long) = dao.getHolidayDaysBetween(fromDay, toDay)
    override suspend fun save(holiday: HolidayEntity)        = dao.upsert(holiday)
    override suspend fun saveAll(holidays: List<HolidayEntity>) = dao.upsertAll(holidays)
    override suspend fun delete(epochDay: Long)              = dao.deleteByDay(epochDay)
    override suspend fun deleteAll()                         = dao.deleteAll()
}
