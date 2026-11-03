package com.adheesh.gid.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ── Subject ───────────────────────────────────────────────────────────────────
// The hub that classes, captures and (later) exams hang off.
// nameKey = name lowercased and trimmed, so "DBMS" and " dbms " are one subject.
@Entity(
    tableName = "subjects",
    indices   = [Index(value = ["nameKey"], unique = true)],
)
data class SubjectEntity(
    @PrimaryKey val id : String,
    val name           : String,
    val nameKey        : String,
    val colorHex       : String?  = null,
    val archived       : Boolean  = false,
    /** Attendance target in percent; null = the one in Settings (SPEC §2.12). */
    val attendanceTarget : Int?   = null,
    /** Classes attended / held before the student started marking in GID. */
    @ColumnInfo(defaultValue = "0") val priorPresent : Int = 0,
    @ColumnInfo(defaultValue = "0") val priorTotal   : Int = 0,
    val createdAt      : Long,
    val updatedAt      : Long,
) {
    companion object {
        fun keyOf(name: String): String = name.trim().lowercase()
    }
}
