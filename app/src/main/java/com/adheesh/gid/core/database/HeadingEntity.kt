package com.adheesh.gid.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ── Heading ───────────────────────────────────────────────────────────────────
// A section inside a subject, e.g. "Important questions", "Unit 3", "PYQs".
@Entity(
    tableName = "headings",
    indices   = [Index(value = ["subjectId"])],
)
data class HeadingEntity(
    @PrimaryKey val id : String,
    val subjectId      : String,
    val name           : String,
    val sortOrder      : Int,
    val createdAt      : Long,
    val updatedAt      : Long,
)
