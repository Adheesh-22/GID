package com.adheesh.gid.feature.timetable.data

import com.adheesh.gid.core.database.GIDPreferences
import com.adheesh.gid.core.database.SubjectEntity
import com.adheesh.gid.core.database.TimetableDao
import com.adheesh.gid.core.database.TimetableEntryEntity
import com.adheesh.gid.feature.subjects.data.SubjectRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest

interface TimetableRepository {
    /** The semester in view: the Timetable popup. */
    fun observeAll(): Flow<List<TimetableEntryEntity>>
    /** The semester whose classes are running: Home's cards. */
    fun observeRunning(): Flow<List<TimetableEntryEntity>>
    /** Every semester. */
    fun observeEverything(): Flow<List<TimetableEntryEntity>>
    /** The running semester's enabled classes: reminders, prompts, suggestions. */
    suspend fun getAllEnabled(): List<TimetableEntryEntity>
    suspend fun get(id: String): TimetableEntryEntity?
    suspend fun save(entry: TimetableEntryEntity)
    suspend fun saveAll(entries: List<TimetableEntryEntity>)
    suspend fun delete(id: String)
    suspend fun deleteAll()
    /** Swaps one semester's timetable, e.g. from VTOP; other semesters stay. */
    suspend fun replaceSemester(semesterId: String, entries: List<TimetableEntryEntity>)
}

// Saving a class links it to the Subject of the same name (SPEC §2.7), so
// captures taken during that class can be suggested into it.
@OptIn(ExperimentalCoroutinesApi::class)
class TimetableRepositoryImpl(
    private val dao      : TimetableDao,
    private val subjects : SubjectRepository,
    private val prefs    : GIDPreferences,
) : TimetableRepository {
    override fun observeAll()        = prefs.viewedSemesterId.flatMapLatest { dao.observeIn(it) }
    override fun observeRunning()    = prefs.runningSemesterId.flatMapLatest { dao.observeIn(it) }
    override fun observeEverything() = dao.observeAll()
    override suspend fun getAllEnabled()                     = dao.getEnabledIn(prefs.runningSemesterId.first())
    override suspend fun get(id: String)                     = dao.get(id)
    override suspend fun save(entry: TimetableEntryEntity)   = dao.upsert(linked(entry))
    override suspend fun saveAll(entries: List<TimetableEntryEntity>) = dao.upsertAll(entries.map { linked(it) })

    private suspend fun linked(entry: TimetableEntryEntity): TimetableEntryEntity =
        if (entry.subject.isBlank()) entry.copy(subjectId = null)
        else entry.copy(subjectId = subjects.ensureIn(entry.subject, entry.semesterId).id)

    override suspend fun delete(id: String)                  = dao.deleteById(id)
    override suspend fun deleteAll()                         = dao.deleteAll()
    override suspend fun replaceSemester(semesterId: String, entries: List<TimetableEntryEntity>) {
        val linkedEntries = entries.map { linked(it) }
        dao.replaceSemester(semesterId, entries.map { SubjectEntity.keyOf(it.subject) }.distinct(), linkedEntries)
    }
}
