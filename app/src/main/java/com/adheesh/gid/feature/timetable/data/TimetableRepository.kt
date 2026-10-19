package com.adheesh.gid.feature.timetable.data

import com.adheesh.gid.core.database.TimetableDao
import com.adheesh.gid.core.database.TimetableEntryEntity
import kotlinx.coroutines.flow.Flow

interface TimetableRepository {
    fun observeAll(): Flow<List<TimetableEntryEntity>>
    fun observeForDay(dayOfWeek: Int): Flow<List<TimetableEntryEntity>>
    suspend fun getAllEnabled(): List<TimetableEntryEntity>
    suspend fun save(entry: TimetableEntryEntity)
    suspend fun saveAll(entries: List<TimetableEntryEntity>)
    suspend fun delete(id: String)
    suspend fun deleteAll()
}

class TimetableRepositoryImpl(private val dao: TimetableDao) : TimetableRepository {
    override fun observeAll()                                = dao.observeAll()
    override fun observeForDay(dayOfWeek: Int)               = dao.observeForDay(dayOfWeek)
    override suspend fun getAllEnabled()                     = dao.getAllEnabled()
    override suspend fun save(entry: TimetableEntryEntity)   = dao.upsert(entry)
    override suspend fun saveAll(entries: List<TimetableEntryEntity>) = dao.upsertAll(entries)
    override suspend fun delete(id: String)                  = dao.deleteById(id)
    override suspend fun deleteAll()                         = dao.deleteAll()
}
