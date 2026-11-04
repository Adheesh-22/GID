package com.adheesh.gid.core.database

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

// ── Capture ───────────────────────────────────────────────────────────────────
// Anything saved into the Inbox: a photo, a link, some text or a PDF.
// kind:   CaptureKind   – what was saved
// type:   a CaptureCategory id – what it means to the student; null until sorted
// status: CaptureStatus – INBOX until the user sorts it
// capturedAt: when the photo was taken (or saved), used to suggest a subject.
// filePath:   absolute path inside filesDir/captures (photos and PDFs only).
//             Two captures may share one file when it is the same file.
@Entity(
    tableName = "captures",
    indices   = [
        Index(value = ["status"]), Index(value = ["subjectId"]), Index(value = ["headingId"]),
        Index(value = ["contentHash"]),
    ],
)
data class CaptureEntity(
    @PrimaryKey val id          : String,
    val kind                    : String,
    val type                    : String?  = null,
    val status                  : String   = CaptureStatus.INBOX.name,
    val title                   : String   = "",
    val note                    : String   = "",
    val url                     : String?  = null,
    val filePath                : String?  = null,
    val mimeType                : String?  = null,
    val capturedAt              : Long,
    val subjectId               : String?  = null,
    val suggestedSubjectId      : String?  = null,
    val headingId               : String?  = null,
    val dueEpochDay             : Long?    = null,
    val linkedTaskId            : String?  = null,
    val openedAt                : Long?    = null,
    /** SHA-256 of the file, so the same file captured twice shares one copy. */
    val contentHash             : String?  = null,
    val createdAt               : Long,
    val updatedAt               : Long,
)

// Full-text search index kept in sync with `captures` by Room's triggers.
// Search with `captures_fts MATCH 'word*'` and join back on rowid.
@Fts4(contentEntity = CaptureEntity::class)
@Entity(tableName = "captures_fts")
data class CaptureFts(
    val title : String,
    val note  : String,
    val url   : String?,
)
