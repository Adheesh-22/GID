package com.adheesh.gid.feature.sessions.data

import android.content.Context
import com.adheesh.gid.core.database.ActivityCategory
import com.adheesh.gid.core.database.ActivitySessionEntity
import com.adheesh.gid.core.database.ActivityTypeEntity
import com.adheesh.gid.core.database.SessionSource
import com.adheesh.gid.core.database.SessionStatus
import com.adheesh.gid.core.notifications.Notifications
import com.adheesh.gid.core.scheduling.TimerScheduler
import com.adheesh.gid.core.util.nowMillis
import com.adheesh.gid.feature.alarm.data.ActivityTypeRepository
import com.adheesh.gid.feature.sessions.domain.SessionRules

// ── SessionManager ───────────────────────────────────────────────────────────
// Applies the session rules of SPEC §3 and keeps the matching notification and
// timer alarm in step with the database row. Screens, receivers and the
// ringing service all go through here so the rules live in one place.
class SessionManager(
    private val context  : Context,
    private val sessions : SessionRepository,
    private val types    : ActivityTypeRepository,
    private val timers   : TimerScheduler,
) {

    /** Timer: a RUNNING session whose end time is persisted (start + minutes). */
    suspend fun startTimer(type: ActivityTypeEntity, minutes: Int): ActivitySessionEntity {
        val session = SessionRules.start(type.id, type.category, SessionSource.TIMER, nowMillis(), plannedMinutes = minutes)
        sessions.save(session)
        val endsAt = SessionRules.endsAt(session)!!
        timers.schedule(session.id, endsAt)
        Notifications.showTimerRunning(context, session.id, type.name, endsAt)
        return session
    }

    /** Timer reached its end: close it at the planned end. The ring is the alarm service's job. */
    suspend fun finishTimer(sessionId: String, notify: Boolean = true) {
        val session = sessions.getById(sessionId) ?: return
        if (session.status != SessionStatus.RUNNING.name) return
        val end = SessionRules.endsAt(session)?.coerceAtMost(nowMillis()) ?: nowMillis()
        sessions.save(SessionRules.close(session, end))
        Notifications.cancel(context, sessionId.hashCode())
        if (notify) Notifications.showTimerDone(context, sessionId, typeName(session.typeId))
    }

    /** Stop early, or 'I'm awake': close with the real duration. */
    suspend fun stop(sessionId: String) {
        val session = sessions.getById(sessionId) ?: return
        if (session.status != SessionStatus.RUNNING.name) return
        sessions.save(SessionRules.close(session, nowMillis()))
        timers.cancel(sessionId)
        Notifications.cancel(context, sessionId.hashCode())
    }

    /** Dismissing an alarm ends any sleep still being counted. */
    suspend fun closeSleepSessions() {
        sessions.getAllRunning()
            .filter { val c = ActivityCategory.of(it.category); c == ActivityCategory.SLEEP || c == ActivityCategory.NAP }
            .forEach { stop(it.id) }
    }

    /**
     * Run on launch, boot and daily: close sessions left RUNNING for over 16
     * hours (flagged), finish timers whose end passed while the phone was off,
     * and re-arm the alarms of timers that are still counting down.
     */
    suspend fun repairRunning() {
        val now = nowMillis()
        for (session in sessions.getAllRunning()) {
            val stale = SessionRules.autoCloseIfStale(session, now)
            val endsAt = SessionRules.endsAt(session)
            when {
                stale != null -> {
                    sessions.save(stale)
                    timers.cancel(session.id)
                    Notifications.cancel(context, session.id.hashCode())
                }
                endsAt != null && endsAt <= now -> finishTimer(session.id)
                endsAt != null -> {
                    timers.schedule(session.id, endsAt)
                    Notifications.showTimerRunning(context, session.id, typeName(session.typeId), endsAt)
                }
                else -> Notifications.showSleeping(context, session.id, typeName(session.typeId), session.startAt)
            }
        }
    }

    private suspend fun typeName(typeId: String): String = types.getById(typeId)?.name.orEmpty()
}
