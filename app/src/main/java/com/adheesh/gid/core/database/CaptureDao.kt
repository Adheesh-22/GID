package com.adheesh.gid.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {

    @Query("SELECT * FROM captures WHERE status = 'INBOX' ORDER BY capturedAt DESC")
    fun observeInbox(): Flow<List<CaptureEntity>>

    @Query("SELECT COUNT(*) FROM captures WHERE status = 'INBOX'")
    fun observeInboxCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM captures WHERE status = 'INBOX'")
    suspend fun inboxCount(): Int

    @Query("SELECT * FROM captures WHERE subjectId = :subjectId AND status = 'SORTED' ORDER BY capturedAt DESC")
    fun observeForSubject(subjectId: String): Flow<List<CaptureEntity>>

    @Query("SELECT COUNT(*) FROM captures WHERE subjectId = :subjectId AND status = 'SORTED'")
    suspend fun countForSubject(subjectId: String): Int

    @Query("SELECT * FROM captures WHERE id = :id")
    fun observe(id: String): Flow<CaptureEntity?>

    @Query("SELECT * FROM captures WHERE id = :id")
    suspend fun get(id: String): CaptureEntity?

    @Query("SELECT * FROM captures WHERE id IN (:ids)")
    suspend fun getAll(ids: List<String>): List<CaptureEntity>

    /**
     * Full-text match on title, note and url, plus a plain match on the
     * subject or heading name. [ftsQuery] is already escaped, e.g. `"dbms"*`.
     */
    @Query(
        """
        SELECT captures.* FROM captures
        WHERE captures.status != 'ARCHIVED' AND (
            captures.rowid IN (SELECT rowid FROM captures_fts WHERE captures_fts MATCH :ftsQuery)
            OR captures.subjectId IN (SELECT id FROM subjects WHERE name LIKE '%' || :plain || '%')
            OR captures.headingId IN (SELECT id FROM headings WHERE name LIKE '%' || :plain || '%')
        )
        ORDER BY captures.capturedAt DESC
        """
    )
    fun search(ftsQuery: String, plain: String): Flow<List<CaptureEntity>>

    // @Upsert updates an existing row in place. REPLACE would delete and re-insert
    // it, and SQLite skips delete triggers for that, leaving stale search entries.
    @Upsert
    suspend fun upsert(capture: CaptureEntity)

    @Upsert
    suspend fun upsertAll(captures: List<CaptureEntity>)

    /** Items under a deleted heading become Unfiled (SPEC §2.8). */
    @Query("UPDATE captures SET headingId = NULL, updatedAt = :now WHERE headingId = :headingId")
    suspend fun clearHeading(headingId: String, now: Long)

    /** A deleted subject's items go back to the Inbox instead of being lost. */
    @Query("UPDATE captures SET status = 'INBOX', subjectId = NULL, headingId = NULL, updatedAt = :now WHERE subjectId = :subjectId")
    suspend fun returnToInbox(subjectId: String, now: Long)

    @Query("UPDATE captures SET suggestedSubjectId = NULL WHERE suggestedSubjectId = :subjectId")
    suspend fun clearSuggestion(subjectId: String)

    @Query("DELETE FROM captures WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT filePath FROM captures WHERE filePath IS NOT NULL")
    suspend fun allFilePaths(): List<String>

    @Query("DELETE FROM captures")
    suspend fun deleteAll()
}
