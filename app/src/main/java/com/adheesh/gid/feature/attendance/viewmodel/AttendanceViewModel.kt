package com.adheesh.gid.feature.attendance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adheesh.gid.core.database.GIDPreferences
import com.adheesh.gid.feature.attendance.data.AttendanceRepository
import com.adheesh.gid.feature.attendance.data.SubjectAttendance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AttendanceViewModel(private val attendance: AttendanceRepository) : ViewModel() {

    val summaries: StateFlow<List<SubjectAttendance>> = attendance.observeSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val defaultTarget: StateFlow<Int> = attendance.observeDefaultTarget()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GIDPreferences.DEFAULT_ATTENDANCE_TARGET)

    /** The subject whose sheet is open, if any. */
    private val _openSubjectId = MutableStateFlow<String?>(null)
    val openSubjectId: StateFlow<String?> = _openSubjectId

    fun open(subjectId: String?) { _openSubjectId.value = subjectId }

    fun setTarget(subjectId: String, target: Int?) = viewModelScope.launch { attendance.setTarget(subjectId, target) }
    fun setPrior(subjectId: String, present: Int, total: Int) = viewModelScope.launch { attendance.setPrior(subjectId, present, total) }
}
