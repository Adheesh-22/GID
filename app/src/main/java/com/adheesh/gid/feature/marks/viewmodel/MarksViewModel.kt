package com.adheesh.gid.feature.marks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adheesh.gid.feature.marks.data.MarksRepository
import com.adheesh.gid.feature.marks.data.SubjectMarkCard
import com.adheesh.gid.feature.marks.domain.VtopMarks
import com.adheesh.gid.feature.semesters.data.VtopImporter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ── Marks page (SPEC §2.15) ──────────────────────────────────────────────────
class MarksViewModel(marks: MarksRepository, private val importer: VtopImporter) : ViewModel() {

    val cards: StateFlow<List<SubjectMarkCard>> = marks.observeCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun import(parsed: VtopMarks) = viewModelScope.launch { importer.importMarks(parsed) }
}

class MarksDetailViewModel(subjectId: String, private val marks: MarksRepository) : ViewModel() {

    val card: StateFlow<SubjectMarkCard?> = marks.observeCard(subjectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setExpected(componentId: String, expected: Double?) = viewModelScope.launch { marks.setExpected(componentId, expected) }
    fun setCredits(componentId: String, credits: Double?) = viewModelScope.launch { marks.setCredits(componentId, credits) }
}
