package com.adheesh.gid.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adheesh.gid.core.database.ActivityTypeEntity
import com.adheesh.gid.core.database.AlarmEntity
import com.adheesh.gid.core.database.Priority
import com.adheesh.gid.core.database.TaskEntity
import com.adheesh.gid.core.database.TimetableEntryEntity
import com.adheesh.gid.core.util.today
import com.adheesh.gid.core.util.weekStart
import com.adheesh.gid.feature.alarm.data.ActivityTypeRepository
import com.adheesh.gid.feature.alarm.data.AlarmRepository
import com.adheesh.gid.feature.alarm.domain.AlarmTimeCalculator
import com.adheesh.gid.feature.calendar.data.HolidayRepository
import com.adheesh.gid.feature.settings.data.ProfilePhotoStore
import com.adheesh.gid.feature.settings.data.ProfileRepository
import com.adheesh.gid.feature.tasks.data.TaskActions
import com.adheesh.gid.feature.tasks.data.TaskDraft
import com.adheesh.gid.feature.tasks.data.TaskRepository
import com.adheesh.gid.feature.timetable.data.TimetableRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

data class NextAlarm(val triggerAt: ZonedDateTime, val alarm: AlarmEntity, val type: ActivityTypeEntity?)

data class UpcomingClasses(
    /** The day these classes are on (today, or the next day that has classes). */
    val day        : LocalDate?,
    val classes    : List<TimetableEntryEntity>,
    val isHoliday  : Boolean,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    profiles             : ProfileRepository,
    photos               : ProfilePhotoStore,
    private val tasks    : TaskRepository,
    holidays             : HolidayRepository,
    timetable            : TimetableRepository,
    alarms               : AlarmRepository,
    types                : ActivityTypeRepository,
    private val actions  : TaskActions,
) : ViewModel() {

    /** Emits once a minute so "next alarm" and "upcoming classes" move on, and at midnight. */
    private val minuteTicker: Flow<ZonedDateTime> = flow {
        while (true) {
            emit(ZonedDateTime.now())
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }

    private val todayFlow: Flow<LocalDate> = minuteTicker.map { it.toLocalDate() }.distinctUntilChanged()

    val name: StateFlow<String?> = profiles.observe().map { it?.fullName?.ifBlank { null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val photoPath: StateFlow<String?> = photos.path
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** This week + next week, Monday first (the 2 × 7 strip). */
    val stripDays: StateFlow<List<LocalDate>> = todayFlow.map { d -> (0L..13L).map { d.weekStart().plusDays(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), (0L..13L).map { today().weekStart().plusDays(it) })

    val stripTaskDays: StateFlow<Set<Long>> = stripDays.flatMapLatest { days ->
        tasks.observeTaskDays(days.first().toEpochDay(), days.last().toEpochDay()).map { it.toSet() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val holidayDays: Flow<Set<Long>> = holidays.observeHolidayDays().map { it.toSet() }

    val stripHolidays: StateFlow<Set<Long>> = holidayDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val nextAlarm: StateFlow<NextAlarm?> = combine(alarms.observeAll(), types.observeAll(), minuteTicker) { list, typeList, now ->
        list.filter { it.enabled }
            .map { a -> NextAlarm(AlarmTimeCalculator.nextTrigger(a.hour, a.minute, a.repeatDaysMask, now), a, typeList.firstOrNull { it.id == a.typeId }) }
            .minByOrNull { it.triggerAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val upcoming: StateFlow<UpcomingClasses> = combine(timetable.observeAll(), holidayDays, minuteTicker) { entries, holidaySet, now ->
        upcomingClasses(entries.filter { it.enabled }, holidaySet, now.toLocalDate(), now.toLocalTime())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UpcomingClasses(null, emptyList(), false))

    /** Today's tasks, already ordered by priority then time by the DAO. */
    val todayTasks: StateFlow<List<TaskEntity>> = todayFlow.flatMapLatest { tasks.observeTasksForDay(it.toEpochDay()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleDone(task: TaskEntity) = viewModelScope.launch { actions.toggleDone(task) }
    fun setProgress(id: String, percent: Int) = viewModelScope.launch { actions.setProgress(id, percent) }
    fun setPriority(id: String, priority: Priority) = viewModelScope.launch { actions.setPriority(id, priority) }
    fun addTask(draft: TaskDraft) = viewModelScope.launch { actions.create(draft) }
    fun updateTask(task: TaskEntity, draft: TaskDraft) = viewModelScope.launch { actions.update(task, draft) }
    fun delete(id: String) = viewModelScope.launch { actions.delete(id) }
    fun moveToTomorrow(id: String) = viewModelScope.launch { actions.moveToTomorrow(id) }

    companion object {
        /**
         * Next 3 classes still to come today; if none, the next day with
         * classes. A holiday today shows 'No classes' (SPEC §2.2).
         */
        fun upcomingClasses(
            entries  : List<TimetableEntryEntity>,
            holidays : Set<Long>,
            today    : LocalDate,
            now      : LocalTime,
        ): UpcomingClasses {
            if (today.toEpochDay() in holidays) return UpcomingClasses(today, emptyList(), isHoliday = true)
            val nowMinutes = now.hour * 60 + now.minute
            val rest = entries.filter { it.dayOfWeek == today.dayOfWeek.value && it.endMinutes > nowMinutes }
                .sortedBy { it.startMinutes }
            if (rest.isNotEmpty()) return UpcomingClasses(today, rest.take(3), isHoliday = false)
            for (offset in 1L..7L) {
                val day = today.plusDays(offset)
                if (day.toEpochDay() in holidays) continue
                val list = entries.filter { it.dayOfWeek == day.dayOfWeek.value }.sortedBy { it.startMinutes }
                if (list.isNotEmpty()) return UpcomingClasses(day, list.take(3), isHoliday = false)
            }
            return UpcomingClasses(null, emptyList(), isHoliday = false)
        }
    }
}
