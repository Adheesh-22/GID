package com.adheesh.gid.feature.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adheesh.gid.core.database.TaskEntity
import com.adheesh.gid.core.util.today
import com.adheesh.gid.feature.tasks.data.TaskActions
import com.adheesh.gid.feature.tasks.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProgressUiState(
    val loaded   : Boolean = false,
    val tasks    : List<TaskEntity> = emptyList(),
    /** Slider values the user is editing, by task id (saved together). */
    val percents : Map<String, Int> = emptyMap(),
    val movedCount : Int? = null,
)

// ── 'Update today's progress' (SPEC §3 evening prompt) ───────────────────────
class ProgressViewModel(
    private val tasks   : TaskRepository,
    private val actions : TaskActions,
) : ViewModel() {

    private val _ui = MutableStateFlow(ProgressUiState())
    val ui: StateFlow<ProgressUiState> = _ui.asStateFlow()

    init { reload() }

    /** Reads today's tasks again (the dialog can be opened many times). */
    fun reload() {
        viewModelScope.launch {
            val list = tasks.getTasksForDay(today().toEpochDay())
            _ui.value = ProgressUiState(loaded = true, tasks = list, percents = list.associate { it.id to it.progressPercent })
        }
    }

    fun setPercent(taskId: String, percent: Int) = _ui.update { it.copy(percents = it.percents + (taskId to percent)) }

    /** Saves the sliders; History scores read these percents. */
    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val s = _ui.value
        s.tasks.forEach { t -> s.percents[t.id]?.takeIf { it != t.progressPercent }?.let { actions.setProgress(t.id, it) } }
        onDone()
    }

    /** Saves first so the final percents stay on today, then carries the rest. */
    fun moveUnfinished() = viewModelScope.launch {
        val s = _ui.value
        s.tasks.forEach { t -> s.percents[t.id]?.takeIf { it != t.progressPercent }?.let { actions.setProgress(t.id, it) } }
        val moved = actions.moveUnfinishedToTomorrow(today().toEpochDay())
        _ui.update { it.copy(movedCount = moved) }
    }
}
