package com.adheesh.gid.feature.subjects.data

import androidx.room.withTransaction
import com.adheesh.gid.core.database.GIDDatabase
import com.adheesh.gid.core.database.HeadingEntity
import com.adheesh.gid.core.database.SubjectEntity
import com.adheesh.gid.core.database.SubjectWithCount
import com.adheesh.gid.core.util.newId
import com.adheesh.gid.core.util.nowMillis
import kotlinx.coroutines.flow.Flow

// ── Subjects and their headings (SPEC §2.8) ──────────────────────────────────
interface SubjectRepository {
    fun observeActive(): Flow<List<SubjectEntity>>
    /** Archived ones too: old captures still need their subject's name. */
    fun observeAll(): Flow<List<SubjectEntity>>
    fun observeActiveWithCounts(): Flow<List<SubjectWithCount>>
    fun observe(id: String): Flow<SubjectEntity?>
    suspend fun get(id: String): SubjectEntity?

    /** The subject with this name (case and spaces ignored), created if missing. */
    suspend fun ensure(name: String): SubjectEntity
    suspend fun rename(id: String, name: String)
    suspend fun setColor(id: String, colorHex: String?)
    suspend fun setArchived(id: String, archived: Boolean)

    fun observeHeadings(subjectId: String): Flow<List<HeadingEntity>>
    suspend fun addHeading(subjectId: String, name: String): HeadingEntity
    suspend fun renameHeading(heading: HeadingEntity, name: String)
    /** Swaps the heading with its neighbour; [up] = towards the top. */
    suspend fun moveHeading(heading: HeadingEntity, up: Boolean)
    suspend fun deleteHeading(id: String)
}

class SubjectRepositoryImpl(private val db: GIDDatabase) : SubjectRepository {

    private val subjects = db.subjectDao()
    private val headings = db.headingDao()

    override fun observeActive()           = subjects.observeActive()
    override fun observeAll()              = subjects.observeAll()
    override fun observeActiveWithCounts() = subjects.observeActiveWithCounts()
    override fun observe(id: String)       = subjects.observe(id)
    override suspend fun get(id: String)   = subjects.get(id)

    override suspend fun ensure(name: String): SubjectEntity = db.withTransaction {
        val key = SubjectEntity.keyOf(name)
        subjects.getByKey(key)?.let { existing ->
            // Using an archived subject again brings it back.
            if (existing.archived) existing.copy(archived = false, updatedAt = nowMillis()).also { subjects.upsert(it) }
            else existing
        } ?: run {
            val now = nowMillis()
            SubjectEntity(id = newId(), name = name.trim(), nameKey = key, createdAt = now, updatedAt = now)
                .also { subjects.upsert(it) }
        }
    }

    override suspend fun rename(id: String, name: String) {
        val subject = subjects.get(id) ?: return
        val key = SubjectEntity.keyOf(name)
        if (key.isEmpty()) return
        // Another subject already has this name: keep both rather than silently merge.
        if (key != subject.nameKey && subjects.getByKey(key) != null) return
        subjects.upsert(subject.copy(name = name.trim(), nameKey = key, updatedAt = nowMillis()))
    }

    override suspend fun setColor(id: String, colorHex: String?) {
        val subject = subjects.get(id) ?: return
        subjects.upsert(subject.copy(colorHex = colorHex, updatedAt = nowMillis()))
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        val subject = subjects.get(id) ?: return
        subjects.upsert(subject.copy(archived = archived, updatedAt = nowMillis()))
    }

    override fun observeHeadings(subjectId: String) = headings.observeForSubject(subjectId)

    override suspend fun addHeading(subjectId: String, name: String): HeadingEntity = db.withTransaction {
        val nextOrder = (headings.getForSubject(subjectId).maxOfOrNull { it.sortOrder } ?: -1) + 1
        val now = nowMillis()
        HeadingEntity(id = newId(), subjectId = subjectId, name = name.trim(), sortOrder = nextOrder, createdAt = now, updatedAt = now)
            .also { headings.upsert(it) }
    }

    override suspend fun renameHeading(heading: HeadingEntity, name: String) {
        if (name.isBlank()) return
        headings.upsert(heading.copy(name = name.trim(), updatedAt = nowMillis()))
    }

    override suspend fun moveHeading(heading: HeadingEntity, up: Boolean) = db.withTransaction {
        // Renumber 0..n first so equal or gapped sortOrders can't make a swap a no-op.
        val list = headings.getForSubject(heading.subjectId).toMutableList()
        val from = list.indexOfFirst { it.id == heading.id }
        val to = if (up) from - 1 else from + 1
        if (from < 0 || to !in list.indices) return@withTransaction
        list.add(to, list.removeAt(from))
        val now = nowMillis()
        headings.upsertAll(list.mapIndexed { i, h -> h.copy(sortOrder = i, updatedAt = now) })
    }

    override suspend fun deleteHeading(id: String) = db.withTransaction {
        db.captureDao().clearHeading(id, nowMillis())
        headings.deleteById(id)
    }
}
