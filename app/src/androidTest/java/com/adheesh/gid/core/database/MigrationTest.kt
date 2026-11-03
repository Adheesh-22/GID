package com.adheesh.gid.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Runs on a phone or emulator: ./gradlew connectedDebugAndroidTest
// Builds a version-1 database from app/schemas/…/1.json, upgrades it to 2, and
// checks the migration turned timetable class names into linked subjects.
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GIDDatabase::class.java,
    )

    @Test
    fun timetableNamesBecomeOneSubjectEach() {
        helper.createDatabase(dbName, 1).use { db ->
            // Same subject typed two ways, plus one other subject and one blank name.
            listOf("t1" to "DBMS", "t2" to " dbms ", "t3" to "Operating Systems", "t4" to "  ").forEach { (id, subject) ->
                db.execSQL(
                    "INSERT INTO timetable_entries (id, subject, dayOfWeek, startMinutes, endMinutes, room, reminderMinutes, enabled, createdAt, updatedAt) " +
                        "VALUES (?, ?, 1, 540, 600, NULL, 10, 1, 0, 0)",
                    arrayOf(id, subject),
                )
            }
        }

        val db = helper.runMigrationsAndValidate(dbName, 2, true)

        val subjects = db.query("SELECT id, name, nameKey FROM subjects ORDER BY nameKey").use { c ->
            buildList { while (c.moveToNext()) add(Triple(c.getString(0), c.getString(1), c.getString(2))) }
        }
        assertEquals(listOf("dbms", "operating systems"), subjects.map { it.third })

        fun subjectOf(entryId: String): String? =
            db.query("SELECT subjectId FROM timetable_entries WHERE id = ?", arrayOf(entryId)).use { c ->
                c.moveToFirst(); if (c.isNull(0)) null else c.getString(0)
            }
        val dbmsId = subjects.first { it.third == "dbms" }.first
        assertEquals(dbmsId, subjectOf("t1"))
        assertEquals(dbmsId, subjectOf("t2"))
        assertNotNull(subjectOf("t3"))
        assertNull(subjectOf("t4"))   // blank names don't become subjects
        db.close()
    }

    @Test
    fun version3AddsExamsAndKeepsLibrary() {
        helper.createDatabase(dbName, 2).use { db ->
            db.execSQL("INSERT INTO subjects (id, name, nameKey, colorHex, archived, createdAt, updatedAt) VALUES ('s1', 'DBMS', 'dbms', NULL, 0, 0, 0)")
            db.execSQL(
                "INSERT INTO captures (id, kind, type, status, title, note, url, filePath, mimeType, capturedAt, subjectId, suggestedSubjectId, headingId, dueEpochDay, linkedTaskId, openedAt, createdAt, updatedAt) " +
                    "VALUES ('c1', 'TEXT', 'NOTE', 'SORTED', 'Normal forms', '', NULL, NULL, NULL, 0, 's1', NULL, NULL, NULL, NULL, NULL, 0, 0)"
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 3, true)

        db.query("SELECT COUNT(*) FROM subjects").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        db.query("SELECT subjectId FROM captures WHERE id = 'c1'").use { c -> c.moveToFirst(); assertEquals("s1", c.getString(0)) }
        // The new table exists and accepts an exam.
        db.execSQL(
            "INSERT INTO exams (id, subjectId, title, kind, dateEpochDay, timeMinutes, room, notes, completedAt, sourceCaptureId, createdAt, updatedAt) " +
                "VALUES ('x1', 's1', 'Mid-sem', 'EXAM', 20000, 600, NULL, '', NULL, NULL, 0, 0)"
        )
        db.query("SELECT COUNT(*) FROM exams").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        db.close()
    }

    @Test
    fun version4AddsAttendanceAndKeepsSubjects() {
        helper.createDatabase(dbName, 3).use { db ->
            db.execSQL("INSERT INTO subjects (id, name, nameKey, colorHex, archived, createdAt, updatedAt) VALUES ('s1', 'DBMS', 'dbms', NULL, 0, 0, 0)")
            db.execSQL(
                "INSERT INTO captures (id, kind, type, status, title, note, url, filePath, mimeType, capturedAt, subjectId, suggestedSubjectId, headingId, dueEpochDay, linkedTaskId, openedAt, createdAt, updatedAt) " +
                    "VALUES ('c1', 'PDF', NULL, 'INBOX', 'Unit 3', '', NULL, '/x/c1.pdf', 'application/pdf', 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0)"
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 4, true)

        // New subject columns start empty; captures have no fingerprint yet.
        db.query("SELECT attendanceTarget, priorPresent, priorTotal FROM subjects WHERE id = 's1'").use { c ->
            c.moveToFirst()
            assertEquals(true, c.isNull(0))
            assertEquals(0, c.getInt(1))
            assertEquals(0, c.getInt(2))
        }
        db.query("SELECT contentHash, filePath FROM captures WHERE id = 'c1'").use { c ->
            c.moveToFirst()
            assertEquals(true, c.isNull(0))
            assertEquals("/x/c1.pdf", c.getString(1))
        }
        db.execSQL(
            "INSERT INTO attendance (id, subjectId, timetableEntryId, dateEpochDay, startMinutes, status, createdAt, updatedAt) " +
                "VALUES ('a1', 's1', 't1', 20000, 540, 'PRESENT', 0, 0)"
        )
        db.query("SELECT COUNT(*) FROM attendance").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        db.close()
    }

    @Test
    fun version5AddsSyllabusAndKeepsExams() {
        helper.createDatabase(dbName, 4).use { db ->
            db.execSQL("INSERT INTO subjects (id, name, nameKey, colorHex, archived, attendanceTarget, priorPresent, priorTotal, createdAt, updatedAt) VALUES ('s1', 'DBMS', 'dbms', NULL, 0, NULL, 0, 0, 0, 0)")
            db.execSQL(
                "INSERT INTO exams (id, subjectId, title, kind, dateEpochDay, timeMinutes, room, notes, completedAt, sourceCaptureId, createdAt, updatedAt) " +
                    "VALUES ('x1', 's1', 'Mid-sem', 'EXAM', 20000, 600, NULL, '', NULL, NULL, 0, 0)"
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 5, true)

        db.query("SELECT COUNT(*) FROM exams").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        db.execSQL("INSERT INTO syllabus_units (id, subjectId, name, sortOrder, createdAt, updatedAt) VALUES ('u1', 's1', 'Unit 1', 0, 0, 0)")
        db.execSQL("INSERT INTO syllabus_topics (id, unitId, subjectId, name, sortOrder, doneAt, createdAt, updatedAt) VALUES ('t1', 'u1', 's1', 'Keys', 0, NULL, 0, 0)")
        db.execSQL("INSERT INTO exam_units (examId, unitId) VALUES ('x1', 'u1')")
        db.query("SELECT COUNT(*) FROM exam_units").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        db.close()
    }
}
