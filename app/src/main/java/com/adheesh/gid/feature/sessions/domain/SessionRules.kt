package com.adheesh.gid.feature.sessions.domain

import com.adheesh.gid.core.database.ActivitySessionEntity
import com.adheesh.gid.core.database.SessionSource
import com.adheesh.gid.core.database.SessionStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

// ── Session rules – SPEC §3 ──────────────────────────────────────────────────
// One ActivitySession table feeds both sleep and focus stats. These functions
// create and close sessions; screens and receivers only call them.
object SessionRules {

    /** A RUNNING session left open longer than this is closed and flagged. */
    const val MAX_RUNNING_MILLIS = 16L * 60 * 60 * 1000

    fun start(
        typeId         : String,
        category       : String,
        source         : SessionSource,
        now            : Long,
        plannedMinutes : Int? = null,
        id             : String = UUID.randomUUID().toString(),
    ) = ActivitySessionEntity(
        id             = id,
        typeId         = typeId,
        category       = category,
        startAt        = now,
        endAt          = null,
        plannedMinutes = plannedMinutes,
        source         = source.name,
        status         = SessionStatus.RUNNING.name,
        createdAt      = now,
        updatedAt      = now,
    )

    /** When a timer session is due to finish (start + planned duration). */
    fun endsAt(session: ActivitySessionEntity): Long? =
        session.plannedMinutes?.let { session.startAt + it * 60_000L }

    /** Closes a running session at [now] with its real duration. */
    fun close(session: ActivitySessionEntity, now: Long): ActivitySessionEntity =
        session.copy(
            endAt     = now.coerceAtLeast(session.startAt),
            status    = SessionStatus.COMPLETED.name,
            updatedAt = now,
        )

    /**
     * If the session has been RUNNING for over 16 hours, close it at the
     * 16-hour mark and flag it; otherwise return null (nothing to do).
     */
    fun autoCloseIfStale(session: ActivitySessionEntity, now: Long): ActivitySessionEntity? {
        if (session.status != SessionStatus.RUNNING.name) return null
        if (now - session.startAt <= MAX_RUNNING_MILLIS) return null
        return session.copy(
            endAt     = session.startAt + MAX_RUNNING_MILLIS,
            status    = SessionStatus.COMPLETED.name,
            flagged   = true,
            updatedAt = now,
        )
    }

    /** The local day a session counts on: the day it ENDS (midnight rule). */
    fun countsOnDay(session: ActivitySessionEntity, zone: ZoneId): LocalDate? =
        session.endAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
}
