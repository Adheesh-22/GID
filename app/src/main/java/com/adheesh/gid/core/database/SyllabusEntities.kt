package com.adheesh.gid.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ── Syllabus (SPEC §2.13) ─────────────────────────────────────────────────────
// A subject's syllabus: units in order, each with topics that get ticked off.

/** A unit (or module, chapter) of a subject. */
@Entity(
    tableName = "syllabus_units",
    indices   = [Index(value = ["subjectId"])],
)
data class SyllabusUnitEntity(
    @PrimaryKey val id : String,
    val subjectId      : String,
    val name           : String,
    val sortOrder      : Int,
    val createdAt      : Long,
    val updatedAt      : Long,
)

/** One topic inside a unit. doneAt: when it was ticked; null while to do. */
@Entity(
    tableName = "syllabus_topics",
    indices   = [Index(value = ["unitId"]), Index(value = ["subjectId"])],
)
data class SyllabusTopicEntity(
    @PrimaryKey val id : String,
    val unitId         : String,
    val subjectId      : String,
    val name           : String,
    val sortOrder      : Int,
    val doneAt         : Long?   = null,
    val createdAt      : Long,
    val updatedAt      : Long,
)

/** Which units an exam covers; none = the whole subject. */
@Entity(
    tableName   = "exam_units",
    primaryKeys = ["examId", "unitId"],
    indices     = [Index(value = ["unitId"])],
)
data class ExamUnitEntity(
    val examId : String,
    val unitId : String,
)
