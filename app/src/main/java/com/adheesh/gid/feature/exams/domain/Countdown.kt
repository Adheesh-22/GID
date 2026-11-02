package com.adheesh.gid.feature.exams.domain

/** How soon something is due: drives the countdown chip's colour. */
enum class Urgency { PAST, NOW, SOON, LATER }

/**
 * 'Today', 'Tomorrow', 'in N days' (up to 13), then 'in N weeks'.
 * [value] is the N for DAYS and WEEKS; the UI turns this into words.
 */
data class Countdown(val unit: Unit, val value: Long, val urgency: Urgency) {
    enum class Unit { PAST, TODAY, TOMORROW, DAYS, WEEKS }

    companion object {
        const val SOON_DAYS = 7L
        const val MAX_DAYS_SHOWN = 13L

        fun of(dateEpochDay: Long, todayEpochDay: Long): Countdown {
            val days = dateEpochDay - todayEpochDay
            return when {
                days < 0               -> Countdown(Unit.PAST, -days, Urgency.PAST)
                days == 0L             -> Countdown(Unit.TODAY, 0, Urgency.NOW)
                days == 1L             -> Countdown(Unit.TOMORROW, 1, Urgency.NOW)
                days <= MAX_DAYS_SHOWN -> Countdown(Unit.DAYS, days, if (days <= SOON_DAYS) Urgency.SOON else Urgency.LATER)
                else                   -> Countdown(Unit.WEEKS, days / 7, Urgency.LATER)
            }
        }
    }
}
