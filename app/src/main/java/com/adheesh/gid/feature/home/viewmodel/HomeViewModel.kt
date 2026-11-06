package com.adheesh.gid.feature.home.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adheesh.gid.core.database.AlarmEntity
import com.adheesh.gid.core.database.Priority
import com.adheesh.gid.core.database.TaskEntity
import com.adheesh.gid.core.database.TimetableEntryEntity
import com.adheesh.gid.core.util.today
import com.adheesh.gid.core.util.weekStart
import com.adheesh.gid.feature.alarm.data.AlarmRepository
import com.adheesh.gid.feature.alarm.domain.AlarmTimeCalculator
import com.adheesh.gid.core.database.AttendanceStatus
import com.adheesh.gid.feature.appearance.data.AppearanceStore
import com.adheesh.gid.feature.attendance.data.AttendanceRepository
import com.adheesh.gid.feature.appearance.domain.Appearance
import com.adheesh.gid.feature.appearance.domain.NameColor
import com.adheesh.gid.feature.appearance.domain.NameFont
import com.adheesh.gid.feature.calendar.data.HolidayRepository
import com.adheesh.gid.feature.settings.data.ProfilePhotoStore
import com.adheesh.gid.feature.settings.data.ProfileRepository
import com.adheesh.gid.feature.tasks.data.TaskActions
import com.adheesh.gid.feature.tasks.data.TaskDraft
import com.adheesh.gid.feature.tasks.data.TaskRepository
import com.adheesh.gid.feature.timetable.data.TimetableRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

data class NextAlarm(val triggerAt: ZonedDateTime, val alarm: AlarmEntity)

/** A class from today that has started, with how it was marked (SPEC §2.12). */
data class ClassToMark(val entry: TimetableEntryEntity, val status: AttendanceStatus?)

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
    private val actions  : TaskActions,
    private val looks    : AppearanceStore,
    private val attendance : AttendanceRepository,
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

    /** Greeting font, colour and name, and the background photo (SPEC §2.2). */
    val appearance: StateFlow<Appearance> = looks.appearance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Appearance())

    private val _photoBusy   = MutableStateFlow(false)
    private val _photoFailed = MutableStateFlow(false)
    val photoBusy: StateFlow<Boolean> = _photoBusy
    val photoFailed: StateFlow<Boolean> = _photoFailed

    /** This week + next week, Monday first (the 2 × 7 strip). */
    val stripDays: StateFlow<List<LocalDate>> = todayFlow.map { d -> (0L..13L).map { d.weekStart().plusDays(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), (0L..13L).map { today().weekStart().plusDays(it) })

    val stripTaskDays: StateFlow<Set<Long>> = stripDays.flatMapLatest { days ->
        tasks.observeTaskDays(days.first().toEpochDay(), days.last().toEpochDay()).map { it.toSet() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val holidayDays: Flow<Set<Long>> = holidays.observeHolidayDays().map { it.toSet() }

    val stripHolidays: StateFlow<Set<Long>> = holidayDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val nextAlarm: StateFlow<NextAlarm?> = combine(alarms.observeAll(), minuteTicker) { list, now ->
        list.filter { it.enabled }
            .map { a -> NextAlarm(AlarmTimeCalculator.nextTrigger(a.hour, a.minute, a.repeatDaysMask, now), a) }
            .minByOrNull { it.triggerAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val upcoming: StateFlow<UpcomingClasses> = combine(timetable.observeRunning(), holidayDays, minuteTicker) { entries, holidaySet, now ->
        upcomingClasses(entries.filter { it.enabled }, holidaySet, now.toLocalDate(), now.toLocalTime())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UpcomingClasses(null, emptyList(), false))

    /** Today's classes that have started, to mark present/absent/cancelled. Empty on holidays. */
    val classesToMark: StateFlow<List<ClassToMark>> = combine(
        timetable.observeRunning(),
        holidayDays,
        minuteTicker,
        todayFlow.flatMapLatest { attendance.observeForDay(it.toEpochDay()) },
    ) { entries, holidaySet, now, marks ->
        val today = now.toLocalDate()
        if (today.toEpochDay() in holidaySet) return@combine emptyList()
        val nowMinutes = now.hour * 60 + now.minute
        entries.filter { it.enabled && it.dayOfWeek == today.dayOfWeek.value && it.startMinutes <= nowMinutes }
            .sortedBy { it.startMinutes }
            .map { e ->
                val mark = marks.firstOrNull { it.timetableEntryId == e.id && it.startMinutes == e.startMinutes }
                ClassToMark(e, AttendanceStatus.of(mark?.status))
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Whether there is a timetable at all, so the attendance card can hide. */
    val hasClasses: StateFlow<Boolean> = timetable.observeRunning().map { list -> list.any { it.enabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Tapping the current mark again clears it. */
    fun mark(item: ClassToMark, status: AttendanceStatus) = viewModelScope.launch {
        val day = today().toEpochDay()
        if (item.status == status) {
            attendance.observeForDay(day).first()
                .firstOrNull { it.timetableEntryId == item.entry.id && it.startMinutes == item.entry.startMinutes }
                ?.let { attendance.delete(it) }
        } else {
            attendance.mark(item.entry, day, status)
        }
    }

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

    fun setHomeName(name: String?) = viewModelScope.launch { looks.setHomeName(name) }
    fun setFont(font: NameFont) = viewModelScope.launch { looks.setFont(font) }
    fun setColor(color: NameColor) = viewModelScope.launch { looks.setColor(color) }
    fun setDim(dim: Float) = viewModelScope.launch { looks.setDim(dim) }
    fun removeBackground() = viewModelScope.launch { looks.clearBackground() }

    fun setBackground(uri: Uri) = viewModelScope.launch {
        _photoBusy.value = true
        _photoFailed.value = false
        // Finishes even if Home is left while the photo is being blurred.
        val ok = withContext(NonCancellable) { looks.setBackground(uri) }
        _photoFailed.value = !ok
        _photoBusy.value = false
    }

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
