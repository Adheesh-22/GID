package com.adheesh.gid.feature.capture.data

import androidx.room.withTransaction
import com.adheesh.gid.core.database.CaptureCategoryEntity
import com.adheesh.gid.core.database.GIDDatabase
import com.adheesh.gid.core.util.newId
import com.adheesh.gid.core.util.nowMillis
import kotlinx.coroutines.flow.Flow

// ── Capture categories (SPEC §2.8) ───────────────────────────────────────────
interface CategoryRepository {
    fun observeAll(): Flow<List<CaptureCategoryEntity>>
    /** Adding a name that already exists returns that category instead of a twin. */
    suspend fun add(name: String): CaptureCategoryEntity
    suspend fun rename(category: CaptureCategoryEntity, name: String)
    /** Items in it stay under All, with no category. */
    suspend fun remove(category: CaptureCategoryEntity)
}

class CategoryRepositoryImpl(private val db: GIDDatabase) : CategoryRepository {

    private val dao = db.captureCategoryDao()

    override fun observeAll() = dao.observeAll()

    override suspend fun add(name: String): CaptureCategoryEntity = db.withTransaction {
        val trimmed = name.trim()
        val all = dao.getAll()
        all.firstOrNull { it.name.equals(trimmed, ignoreCase = true) } ?: run {
            val now = nowMillis()
            val next = (all.maxOfOrNull { it.sortOrder } ?: -1) + 1
            CaptureCategoryEntity(newId(), trimmed, next, now, now).also { dao.upsert(it) }
        }
    }

    override suspend fun rename(category: CaptureCategoryEntity, name: String) =
        dao.upsert(category.copy(name = name.trim(), updatedAt = nowMillis()))

    override suspend fun remove(category: CaptureCategoryEntity) {
        if (!category.isRemovable) return
        db.withTransaction {
            db.captureDao().clearCategory(category.id, nowMillis())
            dao.deleteById(category.id)
        }
    }
}
