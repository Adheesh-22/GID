package com.adheesh.gid.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// ── GIDDatabase ───────────────────────────────────────────────────────────────
// Single Room database for the whole app.  Version starts at 1; bump it and add
// a Migration object whenever you change a schema in future milestones.
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
    ],
    version = 1,
    exportSchema = true,   // saves schema JSON to app/schemas/ for migration diffing
)
abstract class GIDDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun timetableDao(): TimetableDao
    abstract fun holidayDao(): HolidayDao
    abstract fun activityTypeDao(): ActivityTypeDao
    abstract fun alarmDao(): AlarmDao
    abstract fun activitySessionDao(): ActivitySessionDao
    abstract fun profileDao(): ProfileDao

    companion object {
        private const val DATABASE_NAME = "gid_database"

        fun create(context: Context, allowDestructiveMigration: Boolean = false): GIDDatabase {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                GIDDatabase::class.java,
                DATABASE_NAME,
            )
            if (allowDestructiveMigration) {
                // Only enabled in debug builds (see AppContainer).
                // Drops and recreates all tables when the schema changes without
                // providing a Migration object.  Safe during development; NEVER
                // acceptable in production where users have real data.
                builder.fallbackToDestructiveMigration(dropAllTables = true)
            }
            return builder.build()
        }
    }
}
