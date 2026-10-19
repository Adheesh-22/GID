package com.adheesh.gid.feature.alarm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adheesh.gid.core.database.ActivityCategory
import com.adheesh.gid.core.database.ActivitySessionEntity
import com.adheesh.gid.core.database.ActivityTypeEntity
import com.adheesh.gid.core.database.AlarmEntity
import com.adheesh.gid.core.database.SessionSource
import com.adheesh.gid.core.scheduling.AlarmScheduler
import com.adheesh.gid.core.util.newId
import com.adheesh.gid.core.util.nowMillis
import com.adheesh.gid.feature.alarm.data.ActivityTypeRepository
import com.adheesh.gid.feature.alarm.data.AlarmRepository
import com.adheesh.gid.feature.sessions.data.SessionManager
import com.adheesh.gid.feature.sessions.data.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlarmDraft(
    val label          : String,
    val typeId         : String,
    val hour           : Int,
    val minute         : Int,
    val repeatDaysMask : Int,
    val enabled        : Boolean,
    /** SLEEP/NAP only: begin a sleep session right away (SPEC §2.6). */
    val startSleepNow  : Boolean,
)

class AlarmViewModel(
    private val alarms    : AlarmRepository,
    private val types     : ActivityTypeRepository,
    private val scheduler : AlarmScheduler,
    private val sessions  : SessionManager,
    sessionRepository     : SessionRepository,
) : ViewModel() {

    val alarmList: StateFlow<List<AlarmEntity>> = alarms.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val typeList: StateFlow<List<ActivityTypeEntity>> = types.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Timers counting down right now. */
    val runningTimers: StateFlow<List<ActivitySessionEntity>> = sessionRepository.observeAllRunning()
        .map { list -> list.filter { it.source == SessionSource.TIMER.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Alarms ───────────────────────────────────────────────────────────────

    fun saveAlarm(existing: AlarmEntity?, draft: AlarmDraft) = viewModelScope.launch {
        val now = nowMillis()
        val alarm = AlarmEntity(
            id             = existing?.id ?: newId(),
            label          = draft.label.trim(),
            typeId         = draft.typeId,
            hour           = draft.hour,
            minute         = draft.minute,
            repeatDaysMask = draft.repeatDaysMask,
            enabled        = draft.enabled,
            createdAt      = existing?.createdAt ?: now,
            updatedAt      = now,
        )
        alarms.save(alarm)
        scheduler.schedule(alarm)   // any edit re-arms the alarm (SPEC §7)

        val type = types.getById(draft.typeId)
        val category = type?.let { ActivityCategory.of(it.category) }
        if (draft.startSleepNow && type != null && (category == ActivityCategory.SLEEP || category == ActivityCategory.NAP)) {
            sessions.startSleepNow(type)
        }
    }

    fun setEnabled(alarm: AlarmEntity, enabled: Boolean) = viewModelScope.launch {
        val updated = alarm.copy(enabled = enabled, updatedAt = nowMillis())
        alarms.save(updated)
        scheduler.schedule(updated)
    }

    fun deleteAlarm(alarm: AlarmEntity) = viewModelScope.launch {
        scheduler.cancel(alarm.id)
        alarms.delete(alarm.id)
    }

    // ── Timers ───────────────────────────────────────────────────────────────

    fun startTimer(typeId: String, minutes: Int) = viewModelScope.launch {
        val type = types.getById(typeId) ?: return@launch
        sessions.startTimer(type, minutes)
    }

    fun stopTimer(sessionId: String) = viewModelScope.launch { sessions.stop(sessionId) }

    // ── Activity types ───────────────────────────────────────────────────────

    fun saveType(existing: ActivityTypeEntity?, name: String, colorHex: String, category: ActivityCategory) =
        viewModelScope.launch {
            val now = nowMillis()
            types.save(
                ActivityTypeEntity(
                    id        = existing?.id ?: newId(),
                    name      = name.trim(),
                    colorHex  = colorHex,
                    // A default keeps its category so the stats it feeds stay correct.
                    category  = if (existing?.isDefault == true) existing.category else category.name,
                    isDefault = existing?.isDefault ?: false,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                )
            )
        }

    /** Defaults can be renamed but never deleted (the repository enforces it). */
    fun deleteType(type: ActivityTypeEntity) = viewModelScope.launch {
        if (!type.isDefault) types.delete(type.id)
    }
}
