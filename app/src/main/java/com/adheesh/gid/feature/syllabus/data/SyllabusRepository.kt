package com.adheesh.gid.feature.syllabus.data

import androidx.room.withTransaction
import com.adheesh.gid.core.database.ExamUnitEntity
import com.adheesh.gid.core.database.GIDDatabase
import com.adheesh.gid.core.database.SyllabusTopicEntity
import com.adheesh.gid.core.database.SyllabusUnitEntity
import com.adheesh.gid.core.util.newId
import com.adheesh.gid.core.util.nowMillis
import com.adheesh.gid.feature.syllabus.domain.SyllabusParser
import com.adheesh.gid.feature.syllabus.domain.SyllabusProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** A unit with its topics, in order. */
data class UnitWithTopics(val unit: SyllabusUnitEntity, val topics: List<SyllabusTopicEntity>) {
    val progress: SyllabusProgress get() = SyllabusProgress(topics.count { it.doneAt != null }, topics.size)
}

/** An exam's share of the syllabus: the units it covers (all of them when none are picked). */
data class ExamSyllabus(val units: List<UnitWithTopics>, val pickedUnitIds: Set<String>) {
    val progress: SyllabusProgress get() =
        SyllabusProgress(units.sumOf { it.progress.done }, units.sumOf { it.progress.total })
}

// ── Syllabus (SPEC §2.13) ────────────────────────────────────────────────────
interface SyllabusRepository {
    fun observeUnits(subjectId: String): Flow<List<UnitWithTopics>>
    fun observeProgress(subjectId: String): Flow<SyllabusProgress>
    /** [subjectId] may be null while an exam is loading. */
    fun observeExamSyllabus(examId: String, subjectId: String?): Flow<ExamSyllabus>

    suspend fun addUnit(subjectId: String, name: String)
    suspend fun renameUnit(unit: SyllabusUnitEntity, name: String)
    suspend fun moveUnit(unit: SyllabusUnitEntity, up: Boolean)
    /** Deletes its topics and drops it from every exam. */
    suspend fun deleteUnit(unit: SyllabusUnitEntity)

    suspend fun addTopic(unit: SyllabusUnitEntity, name: String)
    suspend fun renameTopic(topic: SyllabusTopicEntity, name: String)
    suspend fun moveTopic(topic: SyllabusTopicEntity, up: Boolean)
    suspend fun setDone(topic: SyllabusTopicEntity, done: Boolean)
    suspend fun deleteTopic(topic: SyllabusTopicEntity)

    /** Adds a pasted syllabus; a unit with an existing name gets the new topics. Returns topics added. */
    suspend fun importText(subjectId: String, text: String): Int

    suspend fun getExamUnitIds(examId: String): Set<String>
    suspend fun setExamUnits(examId: String, unitIds: Set<String>)
}

class SyllabusRepositoryImpl(private val db: GIDDatabase) : SyllabusRepository {

    private val dao = db.syllabusDao()

    override fun observeUnits(subjectId: String): Flow<List<UnitWithTopics>> =
        combine(dao.observeUnits(subjectId), dao.observeTopics(subjectId)) { units, topics ->
            val byUnit = topics.groupBy { it.unitId }
            units.map { UnitWithTopics(it, byUnit[it.id].orEmpty()) }
        }

    override fun observeProgress(subjectId: String): Flow<SyllabusProgress> =
        dao.observeTopics(subjectId).map { topics ->
            SyllabusProgress(topics.count { it.doneAt != null }, topics.size)
        }

    override fun observeExamSyllabus(examId: String, subjectId: String?): Flow<ExamSyllabus> {
        if (subjectId == null) return flowOf(ExamSyllabus(emptyList(), emptySet()))
        return combine(observeUnits(subjectId), dao.observeExamUnitIds(examId)) { units, picked ->
            val pickedSet = picked.toSet()
            ExamSyllabus(if (pickedSet.isEmpty()) units else units.filter { it.unit.id in pickedSet }, pickedSet)
        }
    }

    override suspend fun addUnit(subjectId: String, name: String) {
        val now = nowMillis()
        val next = (dao.getUnits(subjectId).maxOfOrNull { it.sortOrder } ?: -1) + 1
        dao.upsertUnit(SyllabusUnitEntity(newId(), subjectId, name.trim(), next, now, now))
    }

    override suspend fun renameUnit(unit: SyllabusUnitEntity, name: String) =
        dao.upsertUnit(unit.copy(name = name.trim(), updatedAt = nowMillis()))

    override suspend fun moveUnit(unit: SyllabusUnitEntity, up: Boolean) = db.withTransaction {
        val list = dao.getUnits(unit.subjectId).toMutableList()
        if (!list.swap(unit.id, up) { it.id }) return@withTransaction
        val now = nowMillis()
        dao.upsertUnits(list.mapIndexed { i, u -> u.copy(sortOrder = i, updatedAt = now) })
    }

    override suspend fun deleteUnit(unit: SyllabusUnitEntity) = db.withTransaction {
        dao.deleteLinksForUnit(unit.id)
        dao.deleteTopicsForUnit(unit.id)
        dao.deleteUnit(unit.id)
    }

    override suspend fun addTopic(unit: SyllabusUnitEntity, name: String) {
        val now = nowMillis()
        val next = (dao.getTopics(unit.id).maxOfOrNull { it.sortOrder } ?: -1) + 1
        dao.upsertTopic(
            SyllabusTopicEntity(newId(), unit.id, unit.subjectId, name.trim(), next, createdAt = now, updatedAt = now)
        )
    }

    override suspend fun renameTopic(topic: SyllabusTopicEntity, name: String) =
        dao.upsertTopic(topic.copy(name = name.trim(), updatedAt = nowMillis()))

    override suspend fun moveTopic(topic: SyllabusTopicEntity, up: Boolean) = db.withTransaction {
        val list = dao.getTopics(topic.unitId).toMutableList()
        if (!list.swap(topic.id, up) { it.id }) return@withTransaction
        val now = nowMillis()
        dao.upsertTopics(list.mapIndexed { i, t -> t.copy(sortOrder = i, updatedAt = now) })
    }

    override suspend fun setDone(topic: SyllabusTopicEntity, done: Boolean) {
        // Read it fresh so a quick double tap can't undo itself with stale data.
        val current = dao.getTopic(topic.id) ?: return
        val now = nowMillis()
        dao.upsertTopic(current.copy(doneAt = if (done) current.doneAt ?: now else null, updatedAt = now))
    }

    override suspend fun deleteTopic(topic: SyllabusTopicEntity) = dao.deleteTopic(topic.id)

    override suspend fun importText(subjectId: String, text: String): Int = db.withTransaction {
        val parsed = SyllabusParser.parse(text)
        val now = nowMillis()
        val existing = dao.getUnits(subjectId).toMutableList()
        var added = 0
        parsed.forEach { p ->
            val unit = existing.firstOrNull { it.name.equals(p.name, ignoreCase = true) }
                ?: SyllabusUnitEntity(newId(), subjectId, p.name, (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1, now, now)
                    .also { dao.upsertUnit(it); existing += it }
            val start = (dao.getTopics(unit.id).maxOfOrNull { it.sortOrder } ?: -1) + 1
            dao.upsertTopics(p.topics.mapIndexed { i, name ->
                SyllabusTopicEntity(newId(), unit.id, subjectId, name, start + i, createdAt = now, updatedAt = now)
            })
            added += p.topics.size
        }
        added
    }

    override suspend fun getExamUnitIds(examId: String) = dao.getExamUnitIds(examId).toSet()

    override suspend fun setExamUnits(examId: String, unitIds: Set<String>) = db.withTransaction {
        dao.deleteExamUnits(examId)
        dao.insertExamUnits(unitIds.map { ExamUnitEntity(examId, it) })
    }

    /** Moves the item with [id] one place; false if it can't move. */
    private fun <T> MutableList<T>.swap(id: String, up: Boolean, idOf: (T) -> String): Boolean {
        val from = indexOfFirst { idOf(it) == id }
        val to = if (up) from - 1 else from + 1
        if (from < 0 || to !in indices) return false
        add(to, removeAt(from))
        return true
    }
}
