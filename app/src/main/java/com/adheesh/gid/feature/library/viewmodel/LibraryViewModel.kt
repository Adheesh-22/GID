package com.adheesh.gid.feature.library.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adheesh.gid.core.database.CaptureEntity
import com.adheesh.gid.core.database.HeadingEntity
import com.adheesh.gid.core.database.SubjectEntity
import com.adheesh.gid.core.database.SubjectWithCount
import com.adheesh.gid.feature.capture.data.CaptureRepository
import com.adheesh.gid.feature.capture.data.SortChoice
import com.adheesh.gid.feature.subjects.data.SubjectRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Everything the Sort sheet needs, shared by Library, SubjectDetail and
 * CaptureDetail so each sorts captures the same way.
 */
open class SortingViewModel(
    protected val captures : CaptureRepository,
    protected val subjects : SubjectRepository,
) : ViewModel() {

    /** Active subjects, for the Sort sheet's chips. */
    val activeSubjects: StateFlow<List<SubjectEntity>> = subjects.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every subject by id, including archived ones, so old captures still show their subject. */
    val subjectById: StateFlow<Map<String, SubjectEntity>> = subjects.observeAll()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun headingsFor(subjectId: String): Flow<List<HeadingEntity>> = subjects.observeHeadings(subjectId)

    suspend fun createSubject(name: String): SubjectEntity = subjects.ensure(name)

    suspend fun createHeading(subjectId: String, name: String): HeadingEntity = subjects.addHeading(subjectId, name)

    fun sort(ids: List<String>, choice: SortChoice, then: () -> Unit = {}) {
        viewModelScope.launch {
            captures.sort(ids, choice)
            then()
        }
    }
}

// ── Library tab (SPEC §2.8) ──────────────────────────────────────────────────
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class LibraryViewModel(
    captures : CaptureRepository,
    subjects : SubjectRepository,
) : SortingViewModel(captures, subjects) {

    val inbox: StateFlow<List<CaptureEntity>> = captures.observeInbox()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val subjectList: StateFlow<List<SubjectWithCount>> = subjects.observeActiveWithCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** null while the search box is empty, so the screen shows Inbox and Subjects instead. */
    val results: StateFlow<List<CaptureEntity>?> = _query
        .debounce(200)   // don't query on every keystroke
        .flatMapLatest { q -> if (q.isBlank()) flowOf(null) else captures.search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _selection = MutableStateFlow<Set<String>>(emptySet())
    val selection: StateFlow<Set<String>> = _selection.asStateFlow()

    fun setQuery(q: String) { _query.value = q }

    fun toggleSelected(id: String) {
        _selection.value = _selection.value.let { if (id in it) it - id else it + id }
    }

    fun clearSelection() { _selection.value = emptySet() }
}
