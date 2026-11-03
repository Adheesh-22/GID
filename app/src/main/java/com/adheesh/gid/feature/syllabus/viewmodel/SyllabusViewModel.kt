package com.adheesh.gid.feature.syllabus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adheesh.gid.core.database.SubjectEntity
import com.adheesh.gid.core.database.SyllabusTopicEntity
import com.adheesh.gid.core.database.SyllabusUnitEntity
import com.adheesh.gid.feature.subjects.data.SubjectRepository
import com.adheesh.gid.feature.syllabus.data.SyllabusRepository
import com.adheesh.gid.feature.syllabus.data.UnitWithTopics
import com.adheesh.gid.feature.syllabus.domain.SyllabusProgress
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── Syllabus page (SPEC §2.13) ───────────────────────────────────────────────
class SyllabusViewModel(
    private val subjectId : String,
    private val syllabus  : SyllabusRepository,
    subjects              : SubjectRepository,
) : ViewModel() {

    val subject: StateFlow<SubjectEntity?> = subjects.observe(subjectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val units: StateFlow<List<UnitWithTopics>> = syllabus.observeUnits(subjectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val progress: StateFlow<SyllabusProgress> = syllabus.observeProgress(subjectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyllabusProgress.EMPTY)

    fun addUnit(name: String) = launch { syllabus.addUnit(subjectId, name) }
    fun renameUnit(unit: SyllabusUnitEntity, name: String) = launch { syllabus.renameUnit(unit, name) }
    fun moveUnit(unit: SyllabusUnitEntity, up: Boolean) = launch { syllabus.moveUnit(unit, up) }
    fun deleteUnit(unit: SyllabusUnitEntity) = launch { syllabus.deleteUnit(unit) }

    fun addTopic(unit: SyllabusUnitEntity, name: String) = launch { syllabus.addTopic(unit, name) }
    fun renameTopic(topic: SyllabusTopicEntity, name: String) = launch { syllabus.renameTopic(topic, name) }
    fun moveTopic(topic: SyllabusTopicEntity, up: Boolean) = launch { syllabus.moveTopic(topic, up) }
    fun setDone(topic: SyllabusTopicEntity, done: Boolean) = launch { syllabus.setDone(topic, done) }
    fun deleteTopic(topic: SyllabusTopicEntity) = launch { syllabus.deleteTopic(topic) }

    fun importText(text: String) = launch { syllabus.importText(subjectId, text) }

    /** Writes finish even if the page closes straight after. */
    private fun launch(block: suspend () -> Unit) =
        viewModelScope.launch { withContext(NonCancellable) { block() } }
}
