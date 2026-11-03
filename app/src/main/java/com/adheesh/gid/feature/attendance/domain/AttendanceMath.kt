package com.adheesh.gid.feature.attendance.domain

// ── Attendance maths (SPEC §2.12) ────────────────────────────────────────────
// Cancelled classes never count. 'Before GID' counts are added in so a
// student who starts mid-semester sees their real percentage.

/** Where the percent sits against the target, for the bar colour. */
enum class AttendanceLevel { NONE, BELOW, CLOSE, SAFE }

data class AttendanceSummary(
    val present    : Int,
    val total      : Int,
    val target     : Int,
    /** Null while no class has been counted. */
    val percent    : Int?,
    /** More absences in a row that keep the percent at or above the target. */
    val canMiss    : Int,
    /** Presents in a row needed to get back to the target; null if it can't be reached. */
    val mustAttend : Int?,
    val level      : AttendanceLevel,
)

object AttendanceMath {

    /** Points above the target that still count as 'close'. */
    const val CLOSE_MARGIN = 5

    fun summary(
        present      : Int,
        absent       : Int,
        target       : Int,
        priorPresent : Int = 0,
        priorTotal   : Int = 0,
    ): AttendanceSummary {
        val p = present + priorPresent.coerceAtLeast(0)
        val t = present + absent + priorTotal.coerceAtLeast(priorPresent).coerceAtLeast(0)
        val goal = target.coerceIn(0, 100)
        if (t == 0) return AttendanceSummary(p, 0, goal, null, 0, 0, AttendanceLevel.NONE)

        val percent = p * 100 / t
        // Missing k more keeps it if p * 100 >= goal * (t + k).
        val canMiss = when {
            goal == 0          -> Int.MAX_VALUE
            p * 100 < goal * t -> 0
            else               -> (p * 100 / goal) - t
        }
        // Attending n more reaches it if (p + n) * 100 >= goal * (t + n).
        val shortfall = goal * t - p * 100
        val mustAttend = when {
            shortfall <= 0 -> 0
            goal == 100    -> null
            else           -> (shortfall + (100 - goal) - 1) / (100 - goal)
        }
        val exact = p * 100.0 / t
        val level = when {
            exact < goal                -> AttendanceLevel.BELOW
            exact < goal + CLOSE_MARGIN -> AttendanceLevel.CLOSE
            else                        -> AttendanceLevel.SAFE
        }
        return AttendanceSummary(p, t, goal, percent, canMiss, mustAttend, level)
    }
}
