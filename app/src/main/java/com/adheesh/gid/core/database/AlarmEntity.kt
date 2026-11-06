package com.adheesh.gid.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// ── Alarm ─────────────────────────────────────────────────────────────────────
// repeatDaysMask: bitmask where bit 0 = Monday … bit 6 = Sunday
//   e.g. 0b0111110 = Mon–Fri
// typeId: unused since alarms became plain alarms; activity types belong to timers.
//         The column stays so old databases need no change.
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey val id   : String,
    val label            : String,
    val typeId           : String = "",
    val hour             : Int,          // 0-23
    val minute           : Int,          // 0-59
    val repeatDaysMask   : Int = 0,      // 0 = one-off, bitmask for repeat days
    val enabled          : Boolean = true,
    /** Null = the phone's default alarm sound. */
    val ringtoneUri      : String? = null,
    @ColumnInfo(defaultValue = "5") val snoozeMinutes  : Int = DEFAULT_SNOOZE_MINUTES,
    /** Stopping needs the random pattern drawn, not just a tap (SPEC §2.6). */
    @ColumnInfo(defaultValue = "1") val requirePattern : Boolean = true,
    val createdAt        : Long,
    val updatedAt        : Long,
) {
    companion object {
        const val DEFAULT_SNOOZE_MINUTES = 5
    }
}
