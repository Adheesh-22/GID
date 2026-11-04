package com.adheesh.gid.feature.library.viewmodel

import com.adheesh.gid.feature.capture.data.CategoryRepository
import androidx.lifecycle.viewModelScope
import com.adheesh.gid.core.database.CaptureEntity
import com.adheesh.gid.core.database.HeadingEntity
import com.adheesh.gid.feature.capture.data.CaptureRepository
import com.adheesh.gid.feature.subjects.data.SubjectRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ── CaptureDetail (SPEC §2.8) ────────────────────────────────────────────────
@OptIn(ExperimentalCoroutinesApi::class)
class CaptureDetailViewModel(
    private val captureId : String,
    captures              : CaptureRepository,
    subjects              : SubjectRepository,
    categories            : CategoryRepository,
) : SortingViewModel(captures, subjects, categories) {

    val capture: StateFlow<CaptureEntity?> = captures.observe(captureId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The heading it's filed under, if any. */
    val heading: StateFlow<HeadingEntity?> = capture
        .flatMapLatest { c ->
            val sid = c?.subjectId ?: return@flatMapLatest flowOf(null)
            subjects.observeHeadings(sid).map { list -> list.firstOrNull { it.id == c.headingId } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun markOpened() { viewModelScope.launch { captures.markOpened(captureId) } }

    fun delete(then: () -> Unit) {
        viewModelScope.launch {
            captures.delete(captureId)
            then()
        }
    }
}
