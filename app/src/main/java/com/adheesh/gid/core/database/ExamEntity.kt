package com.adheesh.gid.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ── Exam ──────────────────────────────────────────────────────────────────────
// An exam, test, assignment or project for one subject (SPEC §2.10).
// kind:        ExamKind stored as a string
// completedAt: done (exam sat, assignment submitted); null while upcoming
@Entity(
    tableName = "exams",
    indices   = [Index(value = ["subjectId"]), Index(value = ["dateEpochDay"])],
)
data class ExamEntity(
    @PrimaryKey val id      : String,
    val subjectId           : String,
    val title               : String,
    val kind                : String  = "EXAM",
    val dateEpochDay        : Long,
    val timeMinutes         : Int?    = null,
    val room                : String? = null,
    val notes               : String  = "",
    val completedAt         : Long?   = null,
    val sourceCaptureId     : String? = null,   // the Deadline capture it was made from
    val createdAt           : Long,
    val updatedAt           : Long,
)
