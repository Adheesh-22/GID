package com.adheesh.gid.feature.library.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adheesh.gid.core.database.CaptureEntity
import com.adheesh.gid.core.database.HeadingEntity
import com.adheesh.gid.core.database.SubjectEntity
import com.adheesh.gid.feature.capture.data.CaptureRepository
import com.adheesh.gid.feature.subjects.data.SubjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One heading and the items filed under it; heading = null is "Unfiled". */
data class HeadingSection(val heading: HeadingEntity?, val items: List<CaptureEntity>)

// ── SubjectDetail (SPEC §2.8) ────────────────────────────────────────────────
class SubjectDetailViewModel(
    private val subjectId : String,
    captures              : CaptureRepository,
    private val subjects  : SubjectRepository,
) : ViewModel() {

    val subject: StateFlow<SubjectEntity?> = subjects.observe(subjectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val headings: StateFlow<List<HeadingEntity>> = subjects.observeHeadings(subjectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Headings in their order, each with its items, then Unfiled (only when it has items). */
    val sections: StateFlow<List<HeadingSection>> =
        combine(subjects.observeHeadings(subjectId), captures.observeForSubject(subjectId)) { headings, items ->
            val byHeading = items.groupBy { it.headingId }
            val known = headings.map { it.id }.toSet()
            // Items whose heading no longer exists count as Unfiled too.
            val unfiled = items.filter { it.headingId == null || it.headingId !in known }
            headings.map { HeadingSection(it, byHeading[it.id].orEmpty()) } +
                listOfNotNull(unfiled.takeIf { it.isNotEmpty() }?.let { HeadingSection(null, it) })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun rename(name: String)            = launch { subjects.rename(subjectId, name) }
    fun setColor(hex: String?)          = launch { subjects.setColor(subjectId, hex) }
    fun archive()                       = launch { subjects.setArchived(subjectId, true) }
    fun addHeading(name: String)        = launch { subjects.addHeading(subjectId, name) }
    fun renameHeading(h: HeadingEntity, name: String) = launch { subjects.renameHeading(h, name) }
    fun moveHeading(h: HeadingEntity, up: Boolean)    = launch { subjects.moveHeading(h, up) }
    fun deleteHeading(id: String)       = launch { subjects.deleteHeading(id) }

    private fun launch(block: suspend () -> Unit) { viewModelScope.launch { block() } }
}
