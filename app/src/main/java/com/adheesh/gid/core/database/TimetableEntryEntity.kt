package com.adheesh.gid.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// ── TimetableEntry ────────────────────────────────────────────────────────────
// dayOfWeek: 1=Monday … 7=Sunday (ISO-8601, same as java.time.DayOfWeek.value)
// startMinutes / endMinutes: minutes from midnight
@Entity(tableName = "timetable_entries")
data class TimetableEntryEntity(
    @PrimaryKey val id   : String,
    val subject          : String,
    val dayOfWeek        : Int,       // 1–7
    val startMinutes     : Int,
    val endMinutes       : Int,
    val room             : String?    = null,
    val reminderMinutes  : Int        = 10,
    val enabled          : Boolean    = true,
    val createdAt        : Long,
    val updatedAt        : Long,
)
