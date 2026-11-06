package com.adheesh.gid.feature.semesters.data

import com.adheesh.gid.core.database.TimetableEntryEntity
import com.adheesh.gid.core.util.newId
import com.adheesh.gid.core.util.nowMillis
import com.adheesh.gid.feature.exams.data.ExamRepository
import com.adheesh.gid.feature.exams.data.ImportedAssessment
import com.adheesh.gid.feature.marks.data.MarksRepository
import com.adheesh.gid.feature.marks.domain.VtopAssessmentCourse
import com.adheesh.gid.feature.marks.domain.VtopMarks
import com.adheesh.gid.feature.subjects.data.SubjectRepository
import com.adheesh.gid.feature.timetable.data.TimetableRepository
import com.adheesh.gid.feature.timetable.domain.VtopTimetable
import kotlinx.coroutines.flow.first

// ── Pages pasted from VTOP go into their own semester (SPEC §2.14) ───────────
// Each page names its semester, so its subjects land there, and subjects that had
// no semester yet move in by name. That's what untangles what is already saved.
class VtopImporter(
    private val semesters : SemesterRepository,
    private val subjects  : SubjectRepository,
    private val timetable : TimetableRepository,
    private val marks     : MarksRepository,
    private val exams     : ExamRepository,
    private val defaultReminderMinutes: suspend () -> Int,
) {
    /** A pasted page without its heading goes to the semester in view. */
    private suspend fun semesterFor(name: String?): String =
        name?.let { semesters.ensure(it).id } ?: semesters.viewedId.first() ?: semesters.ensure(UNNAMED).id

    suspend fun importTimetable(parsed: VtopTimetable, running: Boolean) {
        val semesterId = semesterFor(parsed.semester)
        parsed.courses.forEach { course ->
            val subject = subjects.ensureIn(course.name, semesterId)
            marks.ensureComponent(semesterId, subject.id, course.code, course.type, course.credits)
        }
        val now = nowMillis()
        val reminder = defaultReminderMinutes()
        timetable.replaceSemester(
            semesterId,
            parsed.classes.map { c ->
                TimetableEntryEntity(
                    id              = newId(),
                    subject         = c.courseName,
                    dayOfWeek       = c.dayOfWeek,
                    startMinutes    = c.startMinutes,
                    endMinutes      = c.endMinutes,
                    room            = if (c.isLab) "${c.venue} · $LAB_LABEL" else c.venue,
                    reminderMinutes = reminder,
                    enabled         = true,
                    semesterId      = semesterId,
                    createdAt       = now,
                    updatedAt       = now,
                )
            },
        )
        semesters.view(semesterId)
        if (running) semesters.setRunning(semesterId)
    }

    suspend fun importMarks(parsed: VtopMarks) {
        val semesterId = semesterFor(parsed.semester)
        parsed.courses.forEach { course ->
            val subject = subjects.ensureIn(course.title, semesterId)
            marks.replaceMarks(semesterId, subject.id, course)
        }
        semesters.view(semesterId)
    }

    /** Returns how many are new. */
    suspend fun importAssessments(courses: List<VtopAssessmentCourse>): Int {
        val items = courses.flatMap { course ->
            val semesterId = semesters.ensure(course.semester).id
            val subject = subjects.ensureIn(course.title, semesterId)
            course.items.map { item ->
                ImportedAssessment(
                    subjectId   = subject.id,
                    title       = item.title,
                    dueEpochDay = item.dueEpochDay,
                    notes       = "Max ${item.maxMark.clean()} · weightage ${item.weightage.clean()}%",
                    uploaded    = item.uploaded,
                    importKey   = "${course.classNumber}|${item.title}",
                )
            }
        }
        val added = exams.importAssessments(items)
        courses.firstOrNull()?.let { semesters.view(semesters.ensure(it.semester).id) }
        return added
    }

    private fun Double.clean(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

    private companion object {
        const val LAB_LABEL = "Lab"
        const val UNNAMED = "My semester"
    }
}
