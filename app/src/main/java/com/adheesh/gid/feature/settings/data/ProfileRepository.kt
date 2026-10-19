package com.adheesh.gid.feature.settings.data

import com.adheesh.gid.core.database.LocalProfileEntity
import com.adheesh.gid.core.database.ProfileDao
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observe(): Flow<LocalProfileEntity?>
    suspend fun get(): LocalProfileEntity?
    suspend fun save(profile: LocalProfileEntity)
    suspend fun deleteAll()
}

class ProfileRepositoryImpl(private val dao: ProfileDao) : ProfileRepository {
    override fun observe()                               = dao.observe()
    override suspend fun get()                           = dao.get()
    override suspend fun save(profile: LocalProfileEntity) = dao.upsert(profile)
    override suspend fun deleteAll()                     = dao.deleteAll()
}
