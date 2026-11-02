package com.adheesh.gid.feature.capture.data

import android.net.Uri
import androidx.room.withTransaction
import com.adheesh.gid.core.database.CaptureEntity
import com.adheesh.gid.core.database.CaptureKind
import com.adheesh.gid.core.database.CaptureStatus
import com.adheesh.gid.core.database.CaptureType
import com.adheesh.gid.core.database.ExamKind
import com.adheesh.gid.core.database.GIDDatabase
import com.adheesh.gid.core.database.Priority
import com.adheesh.gid.core.database.TaskEntity
import com.adheesh.gid.core.database.TaskStatus
import com.adheesh.gid.core.util.newId
import com.adheesh.gid.core.util.nowMillis
import com.adheesh.gid.core.util.toLocalDate
import com.adheesh.gid.core.util.toLocalDateTime
import com.adheesh.gid.feature.capture.domain.CaptureTimeResolver
import com.adheesh.gid.feature.capture.domain.LinkExtractor
import com.adheesh.gid.feature.capture.domain.SubjectSuggester
import com.adheesh.gid.feature.exams.data.ExamDraft
import com.adheesh.gid.feature.exams.data.ExamRepository
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** What the Sort sheet decided for one or more captures. */
data class SortChoice(
    val type        : CaptureType,
    val subjectId   : String?,
    val headingId   : String?,
    /** null = leave as is (batch sorting never overwrites each item's own words). */
    val title       : String?   = null,
    val note        : String?   = null,
    val dueEpochDay : Long?     = null,
    val createTask  : Boolean   = false,
    /** A Deadline can instead become an exam or assignment (needs a subject). */
    val createExam  : ExamKind? = null,
)

// ── Capture Inbox (SPEC §2.8) ────────────────────────────────────────────────
interface CaptureRepository {
    fun observeInbox(): Flow<List<CaptureEntity>>
    fun observeInboxCount(): Flow<Int>
    fun observeForSubject(subjectId: String): Flow<List<CaptureEntity>>
    fun observe(id: String): Flow<CaptureEntity?>
    fun search(query: String): Flow<List<CaptureEntity>>
    suspend fun inboxCount(): Int

    /** Shared or pasted text; becomes a LINK when it contains one. */
    suspend fun captureText(shared: String): CaptureEntity?
    /** A shared or picked photo or PDF; null when the file couldn't be read. */
    suspend fun captureFile(uri: Uri, mimeType: String?): CaptureEntity?
    /** A photo the camera already wrote into app storage. */
    suspend fun captureCameraPhoto(id: String, file: File): CaptureEntity
    /** Removes a camera file when the user backed out of the camera. */
    suspend fun discardFile(file: File)

    suspend fun sort(ids: List<String>, choice: SortChoice)
    suspend fun markOpened(id: String)
    suspend fun delete(id: String)
}

class CaptureRepositoryImpl(
    private val db    : GIDDatabase,
    private val files : CaptureFileStore,
    private val exams : ExamRepository,
) : CaptureRepository {

    private val dao = db.captureDao()

    override fun observeInbox()                       = dao.observeInbox()
    override fun observeInboxCount()                  = dao.observeInboxCount()
    override fun observeForSubject(subjectId: String) = dao.observeForSubject(subjectId)
    override fun observe(id: String)                  = dao.observe(id)
    override suspend fun inboxCount()                 = dao.inboxCount()

    override fun search(query: String): Flow<List<CaptureEntity>> {
        val fts = LinkExtractor.ftsQuery(query)
        return if (fts.isEmpty()) flowOf(emptyList()) else dao.search(fts, query.trim())
    }

    override suspend fun captureText(shared: String): CaptureEntity? {
        if (shared.isBlank()) return null
        val extracted = LinkExtractor.extract(shared)
        val now = nowMillis()
        return save(
            newCapture(
                kind       = if (extracted.url != null) CaptureKind.LINK else CaptureKind.TEXT,
                capturedAt = now,
                // A link's words become its title; plain text is the note itself.
                title      = if (extracted.url != null) extracted.text else "",
                note       = if (extracted.url != null) "" else extracted.text,
                url        = extracted.url,
            )
        )
    }

    override suspend fun captureFile(uri: Uri, mimeType: String?): CaptureEntity? {
        val id = newId()
        val stored = files.copyIn(uri, id, mimeType) ?: return null
        val now = nowMillis()
        val isPdf = stored.mimeType == "application/pdf"
        return save(
            newCapture(
                id         = id,
                kind       = if (isPdf) CaptureKind.PDF else CaptureKind.PHOTO,
                // A PDF's file name is usually its best title; a photo's (IMG_2041) isn't.
                title      = if (isPdf) stored.displayName?.substringBeforeLast('.').orEmpty() else "",
                capturedAt = CaptureTimeResolver.resolve(stored.exifMillis, stored.mediaStoreMillis, now),
                filePath   = stored.path,
                mimeType   = stored.mimeType,
            )
        )
    }

    override suspend fun captureCameraPhoto(id: String, file: File): CaptureEntity {
        val now = nowMillis()
        return save(
            newCapture(
                id         = id,
                kind       = CaptureKind.PHOTO,
                capturedAt = CaptureTimeResolver.resolve(files.exifTime(file), null, now),
                filePath   = file.absolutePath,
                mimeType   = "image/jpeg",
            )
        )
    }

    override suspend fun discardFile(file: File) = files.delete(file.absolutePath)

    override suspend fun sort(ids: List<String>, choice: SortChoice) {
        sortRows(ids, choice)
        // Outside the transaction: saving an exam re-plans reminders and the widget.
        val kind = choice.createExam ?: return
        val subjectId = choice.subjectId ?: return
        val due = choice.dueEpochDay ?: return
        if (choice.type != CaptureType.DEADLINE) return
        dao.getAll(ids).forEach { capture ->
            if (db.examDao().getBySource(capture.id) != null) return@forEach
            exams.save(
                ExamDraft(
                    id              = null,
                    subjectId       = subjectId,
                    title           = capture.title.ifBlank { capture.note.lineSequence().firstOrNull()?.take(80).orEmpty() },
                    kind            = kind,
                    dateEpochDay    = due,
                    notes           = listOfNotNull(capture.note.takeIf { it.isNotBlank() && it != capture.title }, capture.url).joinToString("\n"),
                    sourceCaptureId = capture.id,
                )
            )
        }
    }

    private suspend fun sortRows(ids: List<String>, choice: SortChoice) = db.withTransaction {
        val now = nowMillis()
        dao.getAll(ids).forEach { capture ->
            val title = choice.title ?: capture.title
            val taskId = if (choice.createTask && choice.type == CaptureType.DEADLINE && choice.dueEpochDay != null && capture.linkedTaskId == null) {
                createDeadlineTask(capture.copy(title = title), choice.dueEpochDay, now)
            } else capture.linkedTaskId
            dao.upsert(
                capture.copy(
                    type         = choice.type.name,
                    status       = CaptureStatus.SORTED.name,
                    subjectId    = choice.subjectId,
                    headingId    = choice.headingId,
                    title        = title,
                    note         = choice.note ?: capture.note,
                    dueEpochDay  = if (choice.type == CaptureType.DEADLINE) choice.dueEpochDay ?: capture.dueEpochDay else null,
                    linkedTaskId = taskId,
                    updatedAt    = now,
                )
            )
        }
    }

    override suspend fun markOpened(id: String) {
        val capture = dao.get(id) ?: return
        dao.upsert(capture.copy(openedAt = nowMillis(), updatedAt = nowMillis()))
    }

    override suspend fun delete(id: String) {
        val capture = dao.get(id) ?: return
        dao.deleteById(id)
        files.delete(capture.filePath)
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private suspend fun save(capture: CaptureEntity): CaptureEntity {
        val withSuggestion = capture.copy(suggestedSubjectId = suggestFor(capture.capturedAt))
        dao.upsert(withSuggestion)
        return withSuggestion
    }

    private suspend fun suggestFor(capturedAt: Long): String? {
        val day = capturedAt.toLocalDate().toEpochDay()
        val holidays = db.holidayDao().getHolidayDaysBetween(day, day).toSet()
        return SubjectSuggester.suggest(capturedAt.toLocalDateTime(), db.timetableDao().getAllEnabled(), holidays)
    }

    private suspend fun createDeadlineTask(capture: CaptureEntity, dueEpochDay: Long, now: Long): String {
        val subjectName = capture.subjectId?.let { db.subjectDao().get(it)?.name }
        val task = TaskEntity(
            id           = newId(),
            title        = capture.title.ifBlank { capture.note.lineSequence().firstOrNull()?.take(80) ?: "" }
                               .ifBlank { "Deadline" + (subjectName?.let { " · $it" } ?: "") },
            description  = listOfNotNull(subjectName?.let { "From Library: $it" }, capture.url).joinToString("\n"),
            dateEpochDay = dueEpochDay,
            priority     = Priority.HIGH.name,
            status       = TaskStatus.PENDING.name,
            createdAt    = now,
            updatedAt    = now,
        )
        db.taskDao().upsert(task)
        return task.id
    }

    private fun newCapture(
        kind       : CaptureKind,
        capturedAt : Long,
        id         : String  = newId(),
        title      : String  = "",
        note       : String  = "",
        url        : String? = null,
        filePath   : String? = null,
        mimeType   : String? = null,
    ): CaptureEntity {
        val now = nowMillis()
        return CaptureEntity(
            id = id, kind = kind.name, title = title, note = note, url = url, filePath = filePath,
            mimeType = mimeType, capturedAt = capturedAt, createdAt = now, updatedAt = now,
        )
    }
}
