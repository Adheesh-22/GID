package com.adheesh.gid.core.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

// ── GIDDatabase ───────────────────────────────────────────────────────────────
// Single Room database for the whole app.  Bump the version and add a
// migration whenever a schema changes; users keep their data across updates.
//
// Note: no TypeConverters needed – all fields that Room can't handle natively
// (enums, dates) are stored as primitive types (String / Long / Int).
@Database(
    entities = [
        TaskEntity::class,
        TimetableEntryEntity::class,
        HolidayEntity::class,
        ActivityTypeEntity::class,
        AlarmEntity::class,
        ActivitySessionEntity::class,
        LocalProfileEntity::class,
        SubjectEntity::class,
        HeadingEntity::class,
        CaptureEntity::class,
        CaptureFts::class,
        ExamEntity::class,
        AttendanceEntity::class,
        SyllabusUnitEntity::class,
        SyllabusTopicEntity::class,
        ExamUnitEntity::class,
        CaptureCategoryEntity::class,
    ],
    version = 6,
    exportSchema = true,   // saves schema JSON to app/schemas/ for migration diffing
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = GIDDatabase.SubjectsFromTimetable::class),
        AutoMigration(from = 2, to = 3),   // adds the exams table only
        AutoMigration(from = 3, to = 4),   // attendance table, subject targets, capture hashes
        AutoMigration(from = 4, to = 5),   // syllabus units, topics and exam-unit links
        AutoMigration(from = 5, to = 6, spec = GIDDatabase.DefaultCategories::class),   // categories, alarm options
    ],
)
abstract class GIDDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun timetableDao(): TimetableDao
    abstract fun holidayDao(): HolidayDao
    abstract fun activityTypeDao(): ActivityTypeDao
    abstract fun alarmDao(): AlarmDao
    abstract fun activitySessionDao(): ActivitySessionDao
    abstract fun profileDao(): ProfileDao
    abstract fun subjectDao(): SubjectDao
    abstract fun headingDao(): HeadingDao
    abstract fun captureDao(): CaptureDao
    abstract fun examDao(): ExamDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun syllabusDao(): SyllabusDao
    abstract fun captureCategoryDao(): CaptureCategoryDao

    // ── Migration 1 → 2 ──────────────────────────────────────────────────────
    // Room diffs schemas/1.json against 2.json and writes the CREATE TABLE and
    // ALTER TABLE statements itself. Room can't guess data changes, so this hook
    // turns every distinct class name in the timetable into a Subject and links
    // the classes to it. SQLite has no UUID function; 32 random hex characters
    // are just as unique for an id.
    class SubjectsFromTimetable : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            db.execSQL(
                """
                INSERT INTO subjects (id, name, nameKey, colorHex, archived, createdAt, updatedAt)
                SELECT lower(hex(randomblob(16))), trim(MIN(subject)), lower(trim(subject)), NULL, 0, $now, $now
                FROM timetable_entries
                WHERE trim(subject) != ''
                GROUP BY lower(trim(subject))
                """.trimIndent()
            )
            db.execSQL(
                """
                UPDATE timetable_entries
                SET subjectId = (SELECT id FROM subjects WHERE subjects.nameKey = lower(trim(timetable_entries.subject)))
                """.trimIndent()
            )
        }
    }

    // ── Migration 5 → 6 ──────────────────────────────────────────────────────
    // The old fixed types become rows, so captures keep their type unchanged.
    class DefaultCategories : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) = seedCategories(db)
    }

    companion object {
        const val DATABASE_NAME = "gid_database"

        /** Inserts the default categories; used on upgrade and on a fresh install. */
        private fun seedCategories(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            CaptureType.entries.forEachIndexed { index, type ->
                db.execSQL(
                    "INSERT OR IGNORE INTO capture_categories (id, name, sortOrder, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?)",
                    arrayOf<Any>(type.name, type.defaultName, index, now, now),
                )
            }
        }

        /**
         * There is deliberately no destructive fallback, even in debug builds: those
         * hold real data too. A missing migration should fail loudly, never wipe.
         */
        fun create(context: Context): GIDDatabase {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                GIDDatabase::class.java,
                DATABASE_NAME,
            )
            builder.addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) = seedCategories(db)
            })
            return builder.build()
        }
    }
}
