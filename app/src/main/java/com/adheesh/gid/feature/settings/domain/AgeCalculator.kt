package com.adheesh.gid.feature.settings.domain

import java.time.LocalDate

// ── Age calculator ────────────────────────────────────────────────────────────
// SPEC §2.5: age is computed from date of birth (read-only field in Settings).
// Using java.time.LocalDate which is available from minSdk 26 without desugaring
// (desugaring is still enabled in build.gradle to cover edge cases on some OEMs).

object AgeCalculator {

    /**
     * Returns the person's age in whole completed years as of [today].
     * Returns null when [dobEpochDay] is null (profile not yet filled in).
     */
    fun ageYears(dobEpochDay: Long?, today: LocalDate = LocalDate.now()): Int? {
        dobEpochDay ?: return null
        val dob = LocalDate.ofEpochDay(dobEpochDay)
        // Subtract one year if the birthday hasn't occurred yet this year
        var age = today.year - dob.year
        if (today < dob.withYear(today.year)) age--
        return age.coerceAtLeast(0)
    }
}
