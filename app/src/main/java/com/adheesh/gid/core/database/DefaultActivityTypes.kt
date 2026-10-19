package com.adheesh.gid.core.database

import java.util.UUID

// ── Default activity types – SPEC §2.6 ───────────────────────────────────────
// These 8 types are inserted on first launch and cannot be deleted.
// Colors are the GID palette tokens expressed as hex strings.
object DefaultActivityTypes {

    fun all(): List<ActivityTypeEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            make("Sleep",               "#6E6BF2", "SLEEP",  now),   // periwinkle
            make("Nap",                 "#7FE3E6", "NAP",    now),   // cyan
            make("Study session",       "#F2675A", "FOCUS",  now),   // coral
            make("Vibecode",            "#F5C842", "FOCUS",  now),   // mustard
            make("DSA",                 "#C6DAD0", "FOCUS",  now),   // sage
            make("Learning on YouTube", "#5ED393", "FOCUS",  now),   // holidayGreen
            make("Class",               "#F5C842", "CLASS",  now),   // mustard
            make("Free time",           "#9B9BA1", "FREE",   now),   // mutedText
        )
    }

    private fun make(name: String, colorHex: String, category: String, now: Long) =
        ActivityTypeEntity(
            id        = UUID.randomUUID().toString(),
            name      = name,
            colorHex  = colorHex,
            category  = category,
            isDefault = true,
            createdAt = now,
            updatedAt = now,
        )
}
