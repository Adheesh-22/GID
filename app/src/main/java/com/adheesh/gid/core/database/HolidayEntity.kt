package com.adheesh.gid.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// ── Holiday ───────────────────────────────────────────────────────────────────
@Entity(tableName = "holidays")
data class HolidayEntity(
    @PrimaryKey val id   : String,
    val dateEpochDay     : Long,
    val name             : String? = null,
    val createdAt        : Long,
    val updatedAt        : Long,
)
