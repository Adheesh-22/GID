package com.adheesh.gid.feature.exams.viewmodel

import com.adheesh.gid.core.database.SyllabusTopicEntity
import com.adheesh.gid.feature.syllabus.data.ExamSyllabus
import com.adheesh.gid.feature.syllabus.data.SyllabusRepository
import com.adheesh.gid.feature.syllabus.data.UnitWithTopics
import kotlinx.coroutines.flow.Flow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adheesh.gid.core.database.CaptureEntity
import com.adheesh.gid.core.database.ExamEntity
import com.adheesh.gid.core.database.SubjectEntity
import com.adheesh.gid.feature.capture.data.CaptureRepository
import com.adheesh.gid.feature.exams.data.ExamDraft
import com.adheesh.gid.feature.exams.data.ExamRepository
import com.adheesh.gid.feature.subjects.data.SubjectRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the exam editor needs: subjects to pick from and a way to save. */
open class ExamEditingViewModel(
    protected val exams    : ExamRepository,
    protected val subjects : SubjectRepository,
    protected val syllabus : SyllabusRepository,
) : ViewModel() {

    val activeSubjects: StateFlow<List<SubjectEntity>> = subjects.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every subject by id, archived too, so old exams still show their subject. */
    val subjectById: StateFlow<Map<String, SubjectEntity>> = subjects.observeAll()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    suspend fun createSubject(name: String): SubjectEntity = subjects.ensure(name)

    /** A subject's units, for the 'Covers' chips in the editor (SPEC §2.13). */
    fun unitsFor(subjectId: String): Flow<List<UnitWithTopics>> = syllabus.observeUnits(subjectId)

    suspend fun examUnitIds(examId: String): Set<String> = syllabus.getExamUnitIds(examId)

    /** [unitIds]: the units the exam covers; empty = the whole subject. */
    fun save(draft: ExamDraft, unitIds: Set<String> = emptySet()) {
        // Finishes even if the screen closes straight after.
        viewModelScope.launch {
            withContext(NonCancellable) {
                val saved = exams.save(draft)
                syllabus.setExamUnits(saved.id, unitIds)
            }
        }
    }
}

// ── Exams page (SPEC §2.10) ──────────────────────────────────────────────────
class ExamsViewModel(exams: ExamRepository, subjects: SubjectRepository, syllabus: SyllabusRepository) :
    ExamEditingViewModel(exams, subjects, syllabus) {

    val upcoming: StateFlow<List<ExamEntity>> = exams.observeUpcoming()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val past: StateFlow<List<ExamEntity>> = exams.observePast()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

// ── One exam (SPEC §2.10) ────────────────────────────────────────────────────
@OptIn(ExperimentalCoroutinesApi::class)
class ExamDetailViewModel(
    private val examId : String,
    exams              : ExamRepository,
    subjects           : SubjectRepository,
    captures           : CaptureRepository,
    syllabus           : SyllabusRepository,
) : ExamEditingViewModel(exams, subjects, syllabus) {

    val exam: StateFlow<ExamEntity?> = exams.observe(examId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The subject's sorted Library items: where revision starts. */
    val savedItems: StateFlow<List<CaptureEntity>> = exams.observe(examId)
        .flatMapLatest { e -> e?.let { captures.observeForSubject(it.subjectId) } ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The topics this exam covers and how many are done (SPEC §2.13). */
    val examSyllabus: StateFlow<ExamSyllabus> = exams.observe(examId)
        .flatMapLatest { e -> syllabus.observeExamSyllabus(examId, e?.subjectId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExamSyllabus(emptyList(), emptySet()))

    fun setTopicDone(topic: SyllabusTopicEntity, done: Boolean) {
        viewModelScope.launch { syllabus.setDone(topic, done) }
    }

    fun setCompleted(completed: Boolean) {
        viewModelScope.launch { exams.setCompleted(examId, completed) }
    }

    fun delete() {
        viewModelScope.launch { withContext(NonCancellable) { exams.delete(examId) } }
    }
}
