package com.adheesh.gid.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// ── LocalProfile ──────────────────────────────────────────────────────────────
// Local copy of the user's profile (mirrors the Supabase profiles table).
// dobEpochDay: date-of-birth stored as LocalDate.toEpochDay()
// needsSync:   true when local edits haven't been pushed to Supabase yet
// There is always at most one row (id is always the Supabase user UUID).
@Entity(tableName = "profile")
data class LocalProfileEntity(
    @PrimaryKey val id   : String,       // Supabase auth.users UUID
    val fullName         : String?  = null,
    val dobEpochDay      : Long?    = null,
    val phone            : String?  = null,
    val institution      : String?  = null,
    val needsSync        : Boolean  = false,
    val createdAt        : Long,
    val updatedAt        : Long,
)
