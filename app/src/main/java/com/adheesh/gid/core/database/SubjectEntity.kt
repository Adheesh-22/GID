package com.adheesh.gid.core.database

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
    val createdAt      : Long,
    val updatedAt      : Long,
) {
    companion object {
        fun keyOf(name: String): String = name.trim().lowercase()
    }
}
