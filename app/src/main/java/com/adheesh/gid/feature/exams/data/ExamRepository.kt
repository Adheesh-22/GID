package com.adheesh.gid.feature.exams.data

import com.adheesh.gid.core.database.ExamDao
import com.adheesh.gid.core.database.SyllabusDao
import com.adheesh.gid.core.database.ExamEntity
import com.adheesh.gid.core.database.ExamKind
import com.adheesh.gid.core.util.newId
import com.adheesh.gid.core.util.nowMillis
import com.adheesh.gid.core.util.today
import kotlinx.coroutines.flow.Flow

/** What the exam editor produces; [id] is null for a new one. */
data class ExamDraft(
    val id              : String?,
    val subjectId       : String,
    val title           : String,
    val kind            : ExamKind,
    val dateEpochDay    : Long,
    val timeMinutes     : Int?    = null,
    val room            : String? = null,
    val notes           : String  = "",
    val sourceCaptureId : String? = null,
)

// ── Exams and assignments (SPEC §2.10) ───────────────────────────────────────
interface ExamRepository {
    fun observeUpcoming(): Flow<List<ExamEntity>>
    fun observePast(): Flow<List<ExamEntity>>
    fun observeUpcomingForSubject(subjectId: String): Flow<List<ExamEntity>>
    fun observeBetween(fromDay: Long, toDay: Long): Flow<List<ExamEntity>>
    fun observe(id: String): Flow<ExamEntity?>
    suspend fun get(id: String): ExamEntity?
    suspend fun getUpcoming(): List<ExamEntity>

    suspend fun save(draft: ExamDraft): ExamEntity
    suspend fun setCompleted(id: String, completed: Boolean)
    suspend fun delete(id: String)
}

/**
 * [onChanged] runs after every write: it re-plans the reminders and refreshes
 * the home-screen widget (wired in AppContainer).
 */
class ExamRepositoryImpl(
    private val dao       : ExamDao,
    private val syllabus  : SyllabusDao,
    private val onChanged : suspend () -> Unit,
) : ExamRepository {

    override fun observeUpcoming()                           = dao.observeUpcoming(today().toEpochDay())
    override fun observePast()                               = dao.observePast(today().toEpochDay())
    override fun observeUpcomingForSubject(subjectId: String) = dao.observeUpcomingForSubject(subjectId, today().toEpochDay())
    override fun observeBetween(fromDay: Long, toDay: Long)  = dao.observeBetween(fromDay, toDay)
    override fun observe(id: String)                         = dao.observe(id)
    override suspend fun get(id: String)                     = dao.get(id)
    override suspend fun getUpcoming()                       = dao.getUpcoming(today().toEpochDay())

    override suspend fun save(draft: ExamDraft): ExamEntity {
        val now = nowMillis()
        val existing = draft.id?.let { dao.get(it) }
        val exam = ExamEntity(
            id              = existing?.id ?: newId(),
            subjectId       = draft.subjectId,
            title           = draft.title.trim(),
            kind            = draft.kind.name,
            dateEpochDay    = draft.dateEpochDay,
            timeMinutes     = draft.timeMinutes,
            room            = draft.room?.trim()?.ifBlank { null },
            notes           = draft.notes.trim(),
            completedAt     = existing?.completedAt,
            sourceCaptureId = existing?.sourceCaptureId ?: draft.sourceCaptureId,
            createdAt       = existing?.createdAt ?: now,
            updatedAt       = now,
        )
        dao.upsert(exam)
        onChanged()
        return exam
    }

    override suspend fun setCompleted(id: String, completed: Boolean) {
        val exam = dao.get(id) ?: return
        val now = nowMillis()
        dao.upsert(exam.copy(completedAt = if (completed) exam.completedAt ?: now else null, updatedAt = now))
        onChanged()
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
        syllabus.deleteExamUnits(id)
        onChanged()
    }
}
