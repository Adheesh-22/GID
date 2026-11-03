package com.adheesh.gid.feature.attendance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adheesh.gid.core.database.AttendanceEntity
import com.adheesh.gid.core.database.AttendanceStatus
import com.adheesh.gid.core.database.GIDPreferences
import com.adheesh.gid.feature.attendance.data.AttendanceRepository
import com.adheesh.gid.feature.attendance.data.SubjectAttendance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceViewModel(private val attendance: AttendanceRepository) : ViewModel() {

    val summaries: StateFlow<List<SubjectAttendance>> = attendance.observeSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val defaultTarget: StateFlow<Int> = attendance.observeDefaultTarget()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GIDPreferences.DEFAULT_ATTENDANCE_TARGET)

    /** The subject whose sheet is open, if any. */
    private val _openSubjectId = MutableStateFlow<String?>(null)
    val openSubjectId: StateFlow<String?> = _openSubjectId

    val openMarks: StateFlow<List<AttendanceEntity>> = _openSubjectId
        .flatMapLatest { id -> id?.let { attendance.observeForSubject(it) } ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun open(subjectId: String?) { _openSubjectId.value = subjectId }

    fun setStatus(mark: AttendanceEntity, status: AttendanceStatus) = viewModelScope.launch { attendance.setStatus(mark, status) }
    fun delete(mark: AttendanceEntity) = viewModelScope.launch { attendance.delete(mark) }
    fun setTarget(subjectId: String, target: Int?) = viewModelScope.launch { attendance.setTarget(subjectId, target) }
    fun setPrior(subjectId: String, present: Int, total: Int) = viewModelScope.launch { attendance.setPrior(subjectId, present, total) }
}
