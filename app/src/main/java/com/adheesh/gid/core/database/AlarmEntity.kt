package com.adheesh.gid.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// ── Alarm ─────────────────────────────────────────────────────────────────────
// repeatDaysMask: bitmask where bit 0 = Monday … bit 6 = Sunday
//   e.g. 0b0111110 = Mon–Fri
// typeId: FK to ActivityTypeEntity.id (which type of activity this alarm starts)
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey val id   : String,
    val label            : String,
    val typeId           : String,
    val hour             : Int,          // 0-23
    val minute           : Int,          // 0-59
    val repeatDaysMask   : Int = 0,      // 0 = one-off, bitmask for repeat days
    val enabled          : Boolean = true,
    val createdAt        : Long,
    val updatedAt        : Long,
)
