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
}
