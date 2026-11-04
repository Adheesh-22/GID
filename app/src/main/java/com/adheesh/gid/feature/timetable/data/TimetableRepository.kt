package com.adheesh.gid.feature.timetable.data

import com.adheesh.gid.core.database.TimetableDao
import com.adheesh.gid.core.database.TimetableEntryEntity
import com.adheesh.gid.feature.subjects.data.SubjectRepository
import kotlinx.coroutines.flow.Flow

interface TimetableRepository {
    fun observeAll(): Flow<List<TimetableEntryEntity>>
    fun observeForDay(dayOfWeek: Int): Flow<List<TimetableEntryEntity>>
    suspend fun getAllEnabled(): List<TimetableEntryEntity>
    suspend fun get(id: String): TimetableEntryEntity?
    suspend fun save(entry: TimetableEntryEntity)
    suspend fun saveAll(entries: List<TimetableEntryEntity>)
    suspend fun delete(id: String)
    suspend fun deleteAll()
    /** Swaps the whole timetable, e.g. for a new semester from VTOP. */
    suspend fun replaceAll(entries: List<TimetableEntryEntity>)
}

// Saving a class links it to the Subject of the same name (SPEC §2.7), so
// captures taken during that class can be suggested into it.
class TimetableRepositoryImpl(
    private val dao      : TimetableDao,
    private val subjects : SubjectRepository,
) : TimetableRepository {
    override fun observeAll()                                = dao.observeAll()
    override fun observeForDay(dayOfWeek: Int)               = dao.observeForDay(dayOfWeek)
    override suspend fun getAllEnabled()                     = dao.getAllEnabled()
    override suspend fun get(id: String)                     = dao.get(id)
    override suspend fun save(entry: TimetableEntryEntity)   = dao.upsert(linked(entry))
    override suspend fun saveAll(entries: List<TimetableEntryEntity>) = dao.upsertAll(entries.map { linked(it) })

    private suspend fun linked(entry: TimetableEntryEntity): TimetableEntryEntity =
        if (entry.subject.isBlank()) entry.copy(subjectId = null)
        else entry.copy(subjectId = subjects.ensure(entry.subject).id)

    override suspend fun delete(id: String)                  = dao.deleteById(id)
    override suspend fun deleteAll()                         = dao.deleteAll()
    override suspend fun replaceAll(entries: List<TimetableEntryEntity>) = dao.replaceAll(entries.map { linked(it) })
}
