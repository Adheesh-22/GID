package com.adheesh.gid.feature.timetable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adheesh.gid.core.database.GIDPreferences
import com.adheesh.gid.core.database.TimetableEntryEntity
import com.adheesh.gid.core.scheduling.ClassReminderScheduler
import com.adheesh.gid.core.util.newId
import com.adheesh.gid.core.util.nowMillis
import com.adheesh.gid.feature.timetable.data.TimetableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ClassDraft(
    val subject         : String,
    val dayOfWeek       : Int,
    val startMinutes    : Int,
    val endMinutes      : Int,
    val room            : String,
    val reminderMinutes : Int,
    val enabled         : Boolean,
)

class TimetableViewModel(
    private val timetable : TimetableRepository,
    private val reminders : ClassReminderScheduler,
    preferences           : GIDPreferences,
) : ViewModel() {

    /** Classes grouped by ISO day (1 = Monday), each day sorted by start time. */
    val byDay: StateFlow<Map<Int, List<TimetableEntryEntity>>> = timetable.observeAll()
        .map { list -> list.groupBy { it.dayOfWeek }.toSortedMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val defaultReminder: StateFlow<Int> = preferences.defaultReminderMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GIDPreferences.DEFAULT_REMINDER_MINUTES_VALUE)

    private val _editMode = MutableStateFlow(false)
    val editMode: StateFlow<Boolean> = _editMode.asStateFlow()

    fun toggleEditMode() { _editMode.value = !_editMode.value }

    fun save(existing: TimetableEntryEntity?, draft: ClassDraft) = edit {
        val now = nowMillis()
        timetable.save(
            TimetableEntryEntity(
                id              = existing?.id ?: newId(),
                subject         = draft.subject.trim(),
                dayOfWeek       = draft.dayOfWeek,
                startMinutes    = draft.startMinutes,
                endMinutes      = draft.endMinutes,
                room            = draft.room.trim().ifBlank { null },
                reminderMinutes = draft.reminderMinutes,
                enabled         = draft.enabled,
                createdAt       = existing?.createdAt ?: now,
                updatedAt       = now,
            )
        )
    }

    fun setEnabled(entry: TimetableEntryEntity, enabled: Boolean) = edit {
        timetable.save(entry.copy(enabled = enabled, updatedAt = nowMillis()))
    }

    fun delete(entry: TimetableEntryEntity) = edit { timetable.delete(entry.id) }

    /** Every timetable edit re-plans the reminders (Phase 5 step 2). */
    private fun edit(block: suspend () -> Unit) = viewModelScope.launch {
        block()
        reminders.replan()
    }
}
