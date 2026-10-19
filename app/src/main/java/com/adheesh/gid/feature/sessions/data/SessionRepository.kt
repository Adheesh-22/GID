package com.adheesh.gid.feature.sessions.data

import com.adheesh.gid.core.database.ActivitySessionDao
import com.adheesh.gid.core.database.ActivitySessionEntity
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeEndedBetween(fromMs: Long, toMs: Long): Flow<List<ActivitySessionEntity>>
    fun observeAllRunning(): Flow<List<ActivitySessionEntity>>
    suspend fun getAllRunning(): List<ActivitySessionEntity>
    fun observeRunningSession(): Flow<ActivitySessionEntity?>
    suspend fun getById(id: String): ActivitySessionEntity?
    suspend fun getOverdueRunning(nowMs: Long): List<ActivitySessionEntity>
    suspend fun save(session: ActivitySessionEntity)
    suspend fun saveAll(sessions: List<ActivitySessionEntity>)
    suspend fun delete(session: ActivitySessionEntity)
    suspend fun deleteAll()
}

class SessionRepositoryImpl(private val dao: ActivitySessionDao) : SessionRepository {
    override fun observeEndedBetween(fromMs: Long, toMs: Long)         = dao.observeEndedBetween(fromMs, toMs)
    override fun observeAllRunning()                                   = dao.observeAllRunning()
    override suspend fun getAllRunning()                               = dao.getAllRunning()
    override fun observeRunningSession()                               = dao.observeRunningSession()
    override suspend fun getById(id: String)                           = dao.getById(id)
    override suspend fun getOverdueRunning(nowMs: Long)                = dao.getOverdueRunning(nowMs)
    override suspend fun save(session: ActivitySessionEntity)          = dao.upsert(session)
    override suspend fun saveAll(sessions: List<ActivitySessionEntity>) = dao.upsertAll(sessions)
    override suspend fun delete(session: ActivitySessionEntity)        = dao.delete(session)
    override suspend fun deleteAll()                                   = dao.deleteAll()
}
