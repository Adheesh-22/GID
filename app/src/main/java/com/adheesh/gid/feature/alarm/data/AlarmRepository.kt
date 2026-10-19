package com.adheesh.gid.feature.alarm.data

import com.adheesh.gid.core.database.ActivityTypeDao
import com.adheesh.gid.core.database.ActivityTypeEntity
import com.adheesh.gid.core.database.AlarmDao
import com.adheesh.gid.core.database.AlarmEntity
import kotlinx.coroutines.flow.Flow

// ── ActivityType ──────────────────────────────────────────────────────────────
interface ActivityTypeRepository {
    fun observeAll(): Flow<List<ActivityTypeEntity>>
    suspend fun getById(id: String): ActivityTypeEntity?
    suspend fun save(type: ActivityTypeEntity)
    suspend fun saveAll(types: List<ActivityTypeEntity>)
    /** Returns false if the type is a default and cannot be deleted. */
    suspend fun delete(id: String): Boolean
    suspend fun count(): Int
    suspend fun deleteAll()
}

class ActivityTypeRepositoryImpl(private val dao: ActivityTypeDao) : ActivityTypeRepository {
    override fun observeAll()                                 = dao.observeAll()
    override suspend fun getById(id: String)                  = dao.getById(id)
    override suspend fun save(type: ActivityTypeEntity)       = dao.upsert(type)
    override suspend fun saveAll(types: List<ActivityTypeEntity>) = dao.upsertAll(types)
    override suspend fun delete(id: String): Boolean {
        dao.deleteNonDefault(id)
        // If the row still exists, it was a default – return false
        return dao.getById(id) == null
    }
    override suspend fun count()                              = dao.count()
    override suspend fun deleteAll()                          = dao.deleteAll()
}

// ── Alarm ─────────────────────────────────────────────────────────────────────
interface AlarmRepository {
    fun observeAll(): Flow<List<AlarmEntity>>
    suspend fun getEnabled(): List<AlarmEntity>
    suspend fun getById(id: String): AlarmEntity?
    suspend fun save(alarm: AlarmEntity)
    suspend fun saveAll(alarms: List<AlarmEntity>)
    suspend fun delete(id: String)
    suspend fun deleteAll()
}

class AlarmRepositoryImpl(private val dao: AlarmDao) : AlarmRepository {
    override fun observeAll()                              = dao.observeAll()
    override suspend fun getEnabled()                      = dao.getEnabled()
    override suspend fun getById(id: String)               = dao.getById(id)
    override suspend fun save(alarm: AlarmEntity)          = dao.upsert(alarm)
    override suspend fun saveAll(alarms: List<AlarmEntity>) = dao.upsertAll(alarms)
    override suspend fun delete(id: String)                = dao.deleteById(id)
    override suspend fun deleteAll()                       = dao.deleteAll()
}
