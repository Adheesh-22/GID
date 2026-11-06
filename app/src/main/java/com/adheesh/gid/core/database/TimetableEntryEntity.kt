package com.adheesh.gid.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// ── TimetableEntry ────────────────────────────────────────────────────────────
// dayOfWeek: 1=Monday … 7=Sunday (ISO-8601, same as java.time.DayOfWeek.value)
// startMinutes / endMinutes: minutes from midnight
// subjectId: the Subject with the same name; set by TimetableRepository on save.
@Entity(tableName = "timetable_entries")
data class TimetableEntryEntity(
    @PrimaryKey val id   : String,
    val subject          : String,
    val subjectId        : String?    = null,
    val dayOfWeek        : Int,       // 1–7
    val startMinutes     : Int,
    val endMinutes       : Int,
    val room             : String?    = null,
    val reminderMinutes  : Int        = 10,
    val enabled          : Boolean    = true,
    /** Null for classes saved before semesters: they count as part of any semester. */
    val semesterId       : String?    = null,
    val createdAt        : Long,
    val updatedAt        : Long,
)
