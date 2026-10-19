package com.adheesh.gid.feature.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adheesh.gid.core.database.HolidayEntity
import com.adheesh.gid.core.database.Priority
import com.adheesh.gid.core.database.TaskEntity
import com.adheesh.gid.core.util.newId
import com.adheesh.gid.core.util.nowMillis
import com.adheesh.gid.core.util.today
import com.adheesh.gid.core.util.weekStart
import com.adheesh.gid.feature.calendar.data.HolidayRepository
import com.adheesh.gid.feature.tasks.data.TaskActions
import com.adheesh.gid.feature.tasks.data.TaskDraft
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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** The visible month grid: always whole weeks, Monday first. */
data class MonthGrid(val month: YearMonth, val days: List<LocalDate>) {
    companion object {
        fun of(month: YearMonth): MonthGrid {
            val first = month.atDay(1).weekStart()
            val last  = month.atEndOfMonth()
            val weeks = ((last.toEpochDay() - first.toEpochDay()) / 7 + 1).toInt()
            return MonthGrid(month, (0 until weeks * 7).map { first.plusDays(it.toLong()) })
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val tasks    : TaskRepository,
    private val holidays : HolidayRepository,
    private val actions  : TaskActions,
    initialDate          : LocalDate?,
) : ViewModel() {

    private val start = initialDate ?: today()

    private val _month = MutableStateFlow(YearMonth.from(start))
    val grid: StateFlow<MonthGrid> = _month.map { MonthGrid.of(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MonthGrid.of(YearMonth.from(start)))

    private val _selected = MutableStateFlow(start)
    val selected: StateFlow<LocalDate> = _selected.asStateFlow()

    private val _selectedTaskId = MutableStateFlow<String?>(null)

    /** Days in the visible grid that have tasks (dots). */
    val taskDays: StateFlow<Set<Long>> = grid.flatMapLatest { g ->
        tasks.observeTaskDays(g.days.first().toEpochDay(), g.days.last().toEpochDay()).map { it.toSet() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Holiday name by epochDay (name may be blank). */
    val holidayByDay: StateFlow<Map<Long, String>> = holidays.observeAll()
        .map { list -> list.associate { it.dateEpochDay to it.name.orEmpty() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val dayTasks: StateFlow<List<TaskEntity>> = _selected.flatMapLatest { tasks.observeTasksForDay(it.toEpochDay()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The task shown on the right; falls back to the first task of the day. */
    val selectedTask: StateFlow<TaskEntity?> = combine(dayTasks, _selectedTaskId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun previousMonth() { _month.value = _month.value.minusMonths(1) }
    fun nextMonth()     { _month.value = _month.value.plusMonths(1) }

    fun select(date: LocalDate) {
        _selected.value = date
        _selectedTaskId.value = null
        if (YearMonth.from(date) != _month.value) _month.value = YearMonth.from(date)
    }

    fun selectTask(id: String) { _selectedTaskId.value = id }

    fun setHoliday(date: LocalDate, name: String?) = viewModelScope.launch {
        val now = nowMillis()
        holidays.delete(date.toEpochDay())
        holidays.save(HolidayEntity(newId(), date.toEpochDay(), name?.trim()?.ifBlank { null }, now, now))
    }

    fun removeHoliday(date: LocalDate) = viewModelScope.launch { holidays.delete(date.toEpochDay()) }

    fun addTask(draft: TaskDraft) = viewModelScope.launch { actions.create(draft) }
    fun updateTask(task: TaskEntity, draft: TaskDraft) = viewModelScope.launch { actions.update(task, draft) }
    fun setProgress(taskId: String, percent: Int) = viewModelScope.launch { actions.setProgress(taskId, percent) }
    fun toggleDone(task: TaskEntity) = viewModelScope.launch { actions.toggleDone(task) }
    fun setPriority(taskId: String, priority: Priority) = viewModelScope.launch { actions.setPriority(taskId, priority) }
    fun delete(taskId: String) = viewModelScope.launch { actions.delete(taskId) }
    fun moveToTomorrow(taskId: String) = viewModelScope.launch { actions.moveToTomorrow(taskId) }
}
