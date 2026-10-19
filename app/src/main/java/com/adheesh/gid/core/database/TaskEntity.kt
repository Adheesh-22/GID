package com.adheesh.gid.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// ── Task ──────────────────────────────────────────────────────────────────────
// SPEC §6: every record has a UUID string id + createdAt + updatedAt (epoch ms)
// dateEpochDay: LocalDate stored as LocalDate.toEpochDay() (Long)
// timeMinutes:  optional time-of-day as minutes from midnight
// priority:     "LOW" | "MEDIUM" | "HIGH"
// status:       "PENDING" | "IN_PROGRESS" | "DONE"
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id           : String,
    val title                    : String,
    val description              : String        = "",
    val dateEpochDay             : Long,
    val timeMinutes              : Int?           = null,
    val priority                 : String         = "MEDIUM",  // Priority enum stored as string
    val estimatedMinutes         : Int?           = null,
    val category                 : String?        = null,
    val progressPercent          : Int            = 0,         // 0-100
    val status                   : String         = "PENDING", // TaskStatus enum
    val completedAt              : Long?          = null,      // epoch ms
    val carriedFromId            : String?        = null,      // FK to original task
    val createdAt                : Long,
    val updatedAt                : Long,
)
