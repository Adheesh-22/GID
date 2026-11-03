package com.adheesh.gid.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ── Attendance ────────────────────────────────────────────────────────────────
// One class on one date, marked present, absent or cancelled (SPEC §2.12).
// status:       AttendanceStatus stored as a string
// startMinutes: the class's start that day, so two classes of the same entry
//               never clash and a moved class keeps its old marks.
// Marking the same class on the same date again replaces the row.
@Entity(
    tableName = "attendance",
    indices   = [
        Index(value = ["timetableEntryId", "dateEpochDay", "startMinutes"], unique = true),
        Index(value = ["subjectId"]),
    ],
)
data class AttendanceEntity(
    @PrimaryKey val id      : String,
    val subjectId           : String,
    val timetableEntryId    : String?,
    val dateEpochDay        : Long,
    val startMinutes        : Int,
    val status              : String,
    val createdAt           : Long,
    val updatedAt           : Long,
)

/** Marks per subject and status, for the percent on every card. */
data class AttendanceCount(
    val subjectId : String,
    val status    : String,
    val count     : Int,
)
