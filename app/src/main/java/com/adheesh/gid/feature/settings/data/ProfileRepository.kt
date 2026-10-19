package com.adheesh.gid.feature.settings.data

import com.adheesh.gid.core.database.LocalProfileEntity
import com.adheesh.gid.core.database.ProfileDao
import com.adheesh.gid.core.util.nowMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Result of saving the profile: synced right away, or queued for later. */
enum class SaveOutcome { SYNCED, PENDING_SYNC }

interface ProfileRepository {
    fun observe(): Flow<LocalProfileEntity?>
    /** True while name or date of birth is missing (drives the Settings dot). */
    fun observeIncomplete(): Flow<Boolean>
    suspend fun get(): LocalProfileEntity?
    suspend fun isComplete(): Boolean

    /** Writes locally first, then tries Supabase; offline sets needsSync. */
    suspend fun save(profile: LocalProfileEntity, email: String): SaveOutcome

    /** Pushes a profile that was saved offline. Safe to call on every launch. */
    suspend fun syncPending(email: String)

    /** After sign-in: copy the cloud row down so a returning user skips setup. */
    suspend fun pullFromRemote(userId: String)

    suspend fun deleteAll()
}

class ProfileRepositoryImpl(
    private val dao    : ProfileDao,
    private val remote : ProfileRemoteSource,
) : ProfileRepository {

    override fun observe() = dao.observe()

    override fun observeIncomplete(): Flow<Boolean> = dao.observe().map { !it.isComplete() }

    override suspend fun get() = dao.get()

    override suspend fun isComplete(): Boolean = dao.get().isComplete()

    override suspend fun save(profile: LocalProfileEntity, email: String): SaveOutcome {
        val now = nowMillis()
        val local = profile.copy(needsSync = true, updatedAt = now)
        dao.upsert(local)
        return if (push(local, email)) SaveOutcome.SYNCED else SaveOutcome.PENDING_SYNC
    }

    override suspend fun syncPending(email: String) {
        val local = dao.get() ?: return
        if (local.needsSync) push(local, email)
    }

    override suspend fun pullFromRemote(userId: String) {
        val local = dao.get()
        // A different account signed in on this phone: forget the old profile.
        if (local != null && local.id != userId) dao.deleteAll()
        // Unsynced local edits win over the cloud copy.
        if (local?.id == userId && local.needsSync) return

        val row = try {
            remote.fetch(userId)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null   // offline: keep whatever we have locally
        } ?: return

        val now = nowMillis()
        dao.upsert(
            LocalProfileEntity(
                id          = userId,
                fullName    = row.fullName,
                dobEpochDay = row.dateOfBirth?.let { runCatching { LocalDate.parse(it).toEpochDay() }.getOrNull() },
                phone       = row.phone,
                institution = row.institution,
                needsSync   = false,
                createdAt   = local?.createdAt ?: now,
                updatedAt   = now,
            )
        )
    }

    override suspend fun deleteAll() = dao.deleteAll()

    /** Returns true when the cloud accepted the row. */
    private suspend fun push(local: LocalProfileEntity, email: String): Boolean = try {
        remote.upsert(
            ProfileRow(
                id          = local.id,
                email       = email.ifBlank { null },
                fullName    = local.fullName,
                dateOfBirth = local.dobEpochDay?.let { LocalDate.ofEpochDay(it).toString() },
                phone       = local.phone,
                institution = local.institution,
            )
        )
        dao.upsert(local.copy(needsSync = false))
        true
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        false
    }
}

/** Name and date of birth are the two required fields (SPEC §2.5). */
fun LocalProfileEntity?.isComplete(): Boolean =
    this != null && !fullName.isNullOrBlank() && dobEpochDay != null
