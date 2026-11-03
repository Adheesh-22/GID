package com.adheesh.gid.feature.attendance.data

import com.adheesh.gid.core.database.AttendanceCount
import com.adheesh.gid.core.database.AttendanceDao
import com.adheesh.gid.core.database.AttendanceEntity
import com.adheesh.gid.core.database.AttendanceStatus
import com.adheesh.gid.core.database.GIDPreferences
import com.adheesh.gid.core.database.SubjectDao
import com.adheesh.gid.core.database.SubjectEntity
import com.adheesh.gid.core.database.TimetableEntryEntity
import com.adheesh.gid.core.util.newId
import com.adheesh.gid.core.util.nowMillis
import com.adheesh.gid.feature.attendance.domain.AttendanceMath
import com.adheesh.gid.feature.attendance.domain.AttendanceSummary
import com.adheesh.gid.feature.attendance.domain.ClassSlot
import com.adheesh.gid.feature.subjects.data.SubjectRepository
import com.adheesh.gid.feature.timetable.data.TimetableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/** One subject's attendance, ready for a card. */
data class SubjectAttendance(val subject: SubjectEntity, val summary: AttendanceSummary)

// ── Attendance (SPEC §2.12) ──────────────────────────────────────────────────
interface AttendanceRepository {
    /** Every active subject that has a class or any marks, by name. */
    fun observeSummaries(): Flow<List<SubjectAttendance>>
    fun observeForSubject(subjectId: String): Flow<List<AttendanceEntity>>
    fun observeForDay(day: Long): Flow<List<AttendanceEntity>>
    /** The target used for subjects without their own (Settings). */
    fun observeDefaultTarget(): Flow<Int>

    suspend fun summaryFor(subjectId: String): SubjectAttendance?
    suspend fun isMarked(slot: ClassSlot): Boolean
    suspend fun markedBetween(fromDay: Long, toDay: Long): Set<ClassSlot>

    /** Marks [entry] on [day]; marking again replaces the earlier mark. */
    suspend fun mark(entry: TimetableEntryEntity, day: Long, status: AttendanceStatus): AttendanceEntity
    suspend fun setStatus(mark: AttendanceEntity, status: AttendanceStatus)
    suspend fun delete(mark: AttendanceEntity)
    suspend fun setTarget(subjectId: String, target: Int?)
    suspend fun setPrior(subjectId: String, present: Int, total: Int)
    suspend fun setDefaultTarget(percent: Int)
}

/**
 * [onChanged] runs after every mark change with the class it was for: it
 * re-plans the after-class prompts and clears a prompt that is still showing.
 */
class AttendanceRepositoryImpl(
    private val dao         : AttendanceDao,
    private val subjectDao  : SubjectDao,
    private val subjects    : SubjectRepository,
    private val timetable   : TimetableRepository,
    private val preferences : GIDPreferences,
    private val onChanged   : suspend (ClassSlot?) -> Unit,
) : AttendanceRepository {

    override fun observeSummaries(): Flow<List<SubjectAttendance>> = combine(
        subjects.observeActive(),
        dao.observeCounts(),
        timetable.observeAll(),
        preferences.attendanceTarget,
    ) { active, counts, classes, defaultTarget ->
        val bySubject = counts.groupBy { it.subjectId }
        val withClasses = classes.mapNotNull { it.subjectId }.toSet()
        active
            .filter { it.id in withClasses || it.id in bySubject || it.priorTotal > 0 }
            .map { SubjectAttendance(it, summarise(it, bySubject[it.id].orEmpty(), defaultTarget)) }
    }

    override fun observeForSubject(subjectId: String) = dao.observeForSubject(subjectId)
    override fun observeForDay(day: Long) = dao.observeForDay(day)
    override fun observeDefaultTarget() = preferences.attendanceTarget

    override suspend fun summaryFor(subjectId: String): SubjectAttendance? {
        val subject = subjects.get(subjectId) ?: return null
        return SubjectAttendance(subject, summarise(subject, dao.countsForSubject(subjectId), preferences.attendanceTarget.first()))
    }

    override suspend fun isMarked(slot: ClassSlot) = dao.getFor(slot.entryId, slot.dateEpochDay, slot.startMinutes) != null

    override suspend fun markedBetween(fromDay: Long, toDay: Long): Set<ClassSlot> =
        dao.getBetween(fromDay, toDay).mapNotNull { m ->
            m.timetableEntryId?.let { ClassSlot(it, m.dateEpochDay, m.startMinutes) }
        }.toSet()

    override suspend fun mark(entry: TimetableEntryEntity, day: Long, status: AttendanceStatus): AttendanceEntity {
        // A class saved before subjects existed may not be linked yet.
        val subjectId = entry.subjectId ?: subjects.ensure(entry.subject).id.also {
            timetable.save(entry.copy(subjectId = it))
        }
        val now = nowMillis()
        val existing = dao.getFor(entry.id, day, entry.startMinutes)
        val row = existing?.copy(subjectId = subjectId, status = status.name, updatedAt = now) ?: AttendanceEntity(
            id               = newId(),
            subjectId        = subjectId,
            timetableEntryId = entry.id,
            dateEpochDay     = day,
            startMinutes     = entry.startMinutes,
            status           = status.name,
            createdAt        = now,
            updatedAt        = now,
        )
        dao.upsert(row)
        onChanged(ClassSlot(entry.id, day, entry.startMinutes))
        return row
    }

    override suspend fun setStatus(mark: AttendanceEntity, status: AttendanceStatus) {
        dao.upsert(mark.copy(status = status.name, updatedAt = nowMillis()))
        onChanged(mark.slot())
    }

    override suspend fun delete(mark: AttendanceEntity) {
        dao.deleteById(mark.id)
        onChanged(mark.slot())
    }

    override suspend fun setTarget(subjectId: String, target: Int?) =
        subjectDao.setAttendanceTarget(subjectId, target?.coerceIn(0, 100), nowMillis())

    override suspend fun setPrior(subjectId: String, present: Int, total: Int) {
        val t = total.coerceAtLeast(0)
        subjectDao.setPriorAttendance(subjectId, present.coerceIn(0, t), t, nowMillis())
    }

    override suspend fun setDefaultTarget(percent: Int) = preferences.setAttendanceTarget(percent.coerceIn(0, 100))

    private fun summarise(subject: SubjectEntity, counts: List<AttendanceCount>, defaultTarget: Int): AttendanceSummary {
        fun count(status: AttendanceStatus) = counts.firstOrNull { it.status == status.name }?.count ?: 0
        return AttendanceMath.summary(
            present      = count(AttendanceStatus.PRESENT),
            absent       = count(AttendanceStatus.ABSENT),
            target       = subject.attendanceTarget ?: defaultTarget,
            priorPresent = subject.priorPresent,
            priorTotal   = subject.priorTotal,
        )
    }

    private fun AttendanceEntity.slot() = timetableEntryId?.let { ClassSlot(it, dateEpochDay, startMinutes) }
}
