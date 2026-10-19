package com.adheesh.gid.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// ── ActivityType ──────────────────────────────────────────────────────────────
// category: "SLEEP" | "NAP" | "FOCUS" | "CLASS" | "FREE" | "OTHER"
// isDefault: default types can be renamed but not deleted
@Entity(tableName = "activity_types")
data class ActivityTypeEntity(
    @PrimaryKey val id   : String,
    val name             : String,
    val colorHex         : String,       // e.g. "#6E6BF2"
    val category         : String,       // ActivityCategory enum as string
    val isDefault        : Boolean = false,
    val createdAt        : Long,
    val updatedAt        : Long,
)
