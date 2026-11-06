package com.adheesh.gid.feature.marks.data

import com.adheesh.gid.core.database.MarkComponentEntity
import com.adheesh.gid.core.database.MarkEntryEntity
import com.adheesh.gid.core.database.MarksDao
import com.adheesh.gid.core.database.SubjectEntity
import com.adheesh.gid.core.util.newId
import com.adheesh.gid.core.util.nowMillis
import com.adheesh.gid.feature.marks.domain.ComponentMarks
import com.adheesh.gid.feature.marks.domain.MarkInput
import com.adheesh.gid.feature.marks.domain.MarksMath
import com.adheesh.gid.feature.marks.domain.SubjectMarks
import com.adheesh.gid.feature.marks.domain.VtopMarkCourse
import com.adheesh.gid.feature.subjects.data.SubjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** One part of a subject (theory or lab), worked out. */
data class MarkPart(
    val component : MarkComponentEntity,
    val entries   : List<MarkEntryEntity>,
    val marks     : ComponentMarks,
    /** This part's share of the subject, 0 to 1. */
    val share     : Double,
)

data class SubjectMarkCard(
    val subject : SubjectEntity,
    val parts   : List<MarkPart>,
    val totals  : SubjectMarks,
)

// ── Marks (SPEC §2.15) ───────────────────────────────────────────────────────
class MarksRepository(
    private val dao      : MarksDao,
    private val subjects : SubjectRepository,
) {
    /** Subjects in the semester in view that have marks or credits. */
    fun observeCards(): Flow<List<SubjectMarkCard>> =
        combine(subjects.observeActive(), dao.observeComponents(), dao.observeEntries()) { active, components, entries ->
            active.mapNotNull { card(it, components, entries) }
        }

    /** One subject, whichever semester is in view. */
    fun observeCard(subjectId: String): Flow<SubjectMarkCard?> =
        combine(subjects.observe(subjectId), dao.observeComponents(), dao.observeEntries()) { subject, components, entries ->
            subject?.let { card(it, components, entries) }
        }

    private fun card(
        subject    : SubjectEntity,
        components : List<MarkComponentEntity>,
        entries    : List<MarkEntryEntity>,
    ): SubjectMarkCard? {
        // Theory first, then lab, whatever order they were pasted in.
        val mine = components.filter { it.subjectId == subject.id }
            .sortedWith(compareBy({ it.courseType.contains(LAB) }, { it.courseType }))
        if (mine.isEmpty()) return null
        val byComponent = entries.groupBy { it.componentId }
        val worked = mine.map { c ->
            val rows = byComponent[c.id].orEmpty()
            val inputs = rows.map { MarkInput(it.maxMark, it.weightage, it.scoredMark, it.weightageMark) }
            Triple(c, rows, MarksMath.component(inputs, c.expected))
        }
        val totals = MarksMath.subject(worked.map { (c, _, m) -> c.credits to m })
        return SubjectMarkCard(
            subject = subject,
            parts   = worked.mapIndexed { i, (c, rows, m) -> MarkPart(c, rows, m, totals.shares[i]) },
            totals  = totals,
        )
    }

    /** From the timetable page: a part with its credits, before any marks exist. */
    suspend fun ensureComponent(semesterId: String?, subjectId: String, code: String, type: String, credits: Double?): MarkComponentEntity {
        val now = nowMillis()
        val existing = dao.findComponent(subjectId, code, type)
        val row = existing?.copy(credits = credits ?: existing.credits, updatedAt = now)
            ?: MarkComponentEntity(newId(), semesterId, subjectId, code, type, credits, null, now, now)
        dao.upsertComponent(row)
        return row
    }

    /** From the marks page: replaces what VTOP said before; credits and expectations stay. */
    suspend fun replaceMarks(semesterId: String?, subjectId: String, course: VtopMarkCourse) {
        val component = ensureComponent(semesterId, subjectId, course.code, course.type, null)
        val now = nowMillis()
        dao.replaceEntries(
            component.id,
            course.rows.mapIndexed { i, row ->
                MarkEntryEntity(
                    id            = newId(),
                    componentId   = component.id,
                    title         = row.title,
                    maxMark       = row.maxMark,
                    weightage     = row.weightage,
                    status        = row.status,
                    scoredMark    = row.scoredMark,
                    weightageMark = row.weightageMark,
                    remark        = row.remark,
                    sortOrder     = i,
                    createdAt     = now,
                    updatedAt     = now,
                )
            },
        )
    }

    suspend fun setExpected(componentId: String, expected: Double?) {
        val c = dao.getComponent(componentId) ?: return
        dao.upsertComponent(c.copy(expected = expected, updatedAt = nowMillis()))
    }

    suspend fun setCredits(componentId: String, credits: Double?) {
        val c = dao.getComponent(componentId) ?: return
        dao.upsertComponent(c.copy(credits = credits, updatedAt = nowMillis()))
    }

    private companion object {
        const val LAB = "Lab"
    }
}
