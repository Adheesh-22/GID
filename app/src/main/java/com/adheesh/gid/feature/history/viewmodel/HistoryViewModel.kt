package com.adheesh.gid.feature.history.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adheesh.gid.core.util.startMillis
import com.adheesh.gid.core.util.today
import com.adheesh.gid.core.util.weekStart
import com.adheesh.gid.core.util.zone
import com.adheesh.gid.feature.alarm.data.ActivityTypeRepository
import com.adheesh.gid.feature.history.domain.HistoryCalculator
import com.adheesh.gid.feature.history.domain.WeekSummary
import com.adheesh.gid.feature.sessions.data.SessionRepository
import com.adheesh.gid.feature.tasks.data.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val tasks    : TaskRepository,
    private val sessions : SessionRepository,
    types                : ActivityTypeRepository,
) : ViewModel() {

    private val _weekStart = MutableStateFlow(today().weekStart())
    val weekStart: StateFlow<LocalDate> = _weekStart.asStateFlow()

    /** null while the first query is loading. */
    val summary: StateFlow<WeekSummary?> = _weekStart.flatMapLatest { start ->
        val previous = start.minusDays(7)
        combine(
            // Two weeks of tasks: this week plus last week for the comparison.
            tasks.observeTasksForRange(previous.toEpochDay(), start.plusDays(6).toEpochDay()),
            sessions.observeEndedBetween(start.startMillis(), start.plusDays(7).startMillis()),
        ) { taskList, sessionList ->
            HistoryCalculator.summarize(start, taskList, sessionList, zone())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val typeNames: StateFlow<Map<String, String>> = types.observeAll()
        .map { list -> list.associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val isCurrentWeek: StateFlow<Boolean> = _weekStart.map { it == today().weekStart() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun previousWeek() { _weekStart.value = _weekStart.value.minusWeeks(1) }

    fun nextWeek() {
        // No future weeks: there is nothing to show yet.
        if (_weekStart.value < today().weekStart()) _weekStart.value = _weekStart.value.plusWeeks(1)
    }
}
