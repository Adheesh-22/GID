package com.adheesh.gid.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// ── ActivitySession ───────────────────────────────────────────────────────────
// One row per activity session (sleep, nap, focus, class, free-time, etc.)
// source:  "ALARM" | "TIMER" | "MANUAL"
// status:  "RUNNING" | "COMPLETED" | "CANCELLED"
// flagged: true if the session ran for > 16 h and was auto-closed (SPEC §3)
@Entity(tableName = "activity_sessions")
data class ActivitySessionEntity(
    @PrimaryKey val id   : String,
    val typeId           : String,       // FK → ActivityTypeEntity.id
    val category         : String,       // denormalised for fast queries
    val startAt          : Long,         // epoch ms
    val endAt            : Long?  = null, // null while RUNNING
    val plannedMinutes   : Int?   = null,
    val source           : String,       // SessionSource enum
    val status           : String,       // SessionStatus enum
    val flagged          : Boolean = false,
    val createdAt        : Long,
    val updatedAt        : Long,
)
