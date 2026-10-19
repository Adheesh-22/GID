package com.adheesh.gid.core.database

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.random.Random

// ── SampleDataSeeder ──────────────────────────────────────────────────────────
// Creates two weeks of realistic data for UI development.
// Called from the Debug screen's 'Seed sample data' button.
// All data is generated deterministically from the current date.
class SampleDataSeeder(private val db: GIDDatabase) {

    suspend fun seed() {
        val today     = LocalDate.now()
        val monday    = today.with(DayOfWeek.MONDAY)   // start of current week
        val now       = System.currentTimeMillis()

        // ── 1. Activity types ─────────────────────────────────────────────────
        // Reuse the default types seeded on first launch so we never create
        // duplicates; only insert them if they are missing (e.g. after Clear).
        val existing = db.activityTypeDao().getAll()
        val allTypes = existing.ifEmpty { DefaultActivityTypes.all().also { db.activityTypeDao().upsertAll(it) } }

        val sleepType  = allTypes.first { it.category == "SLEEP" }
        val napType    = allTypes.first { it.category == "NAP" }
        val focusTypes = allTypes.filter { it.category == "FOCUS" }

        // ── 2. Timetable – Mon/Tue/Wed/Thu/Fri ───────────────────────────────
        val subjects = listOf(
            Triple("Data Structures",    9 * 60,  10 * 60 + 30),
            Triple("Operating Systems",  11 * 60, 12 * 60 + 30),
            Triple("Database Systems",   14 * 60, 15 * 60 + 30),
            Triple("Computer Networks",  9 * 60,  10 * 60 + 30),
            Triple("Software Eng.",      11 * 60, 12 * 60 + 30),
        )
        val ttEntries = (1..5).flatMapIndexed { idx, dow ->
            val (subj, start, end) = subjects[idx]
            listOf(
                TimetableEntryEntity(
                    id             = uuid(),
                    subject        = subj,
                    dayOfWeek      = dow,
                    startMinutes   = start,
                    endMinutes     = end,
                    room           = "Room ${100 + idx * 10}",
                    reminderMinutes = 10,
                    enabled        = true,
                    createdAt      = now,
                    updatedAt      = now,
                )
            )
        }
        db.timetableDao().upsertAll(ttEntries)

        // ── 3. Holidays ───────────────────────────────────────────────────────
        db.holidayDao().upsertAll(listOf(
            holiday(monday.plusDays(5), "Weekend", now),   // Saturday of this week
            holiday(monday.plusDays(6), "Weekend", now),   // Sunday
            holiday(monday.plusDays(12), "Weekend", now),  // Saturday next week
            holiday(monday.plusDays(13), "Weekend", now),  // Sunday next week
            holiday(monday.plusDays(3),  "Public Holiday", now),  // Thu this week
        ))

        // ── 4. Tasks – 14 days ────────────────────────────────────────────────
        val taskTitles = listOf(
            "Implement linked list in Java",
            "Revise chapter 5 – OS scheduling",
            "Submit DB assignment",
            "Practice 3 LeetCode problems",
            "Read networks textbook ch.7",
            "Complete lab report",
            "Prepare presentation slides",
            "Review pull request from team",
            "Study sorting algorithms",
            "Write unit tests for parser",
        )
        val priorities = listOf("HIGH", "HIGH", "MEDIUM", "MEDIUM", "MEDIUM", "LOW", "LOW")
        val tasks = (0..13).flatMap { dayOffset ->
            val epochDay = monday.plusDays(dayOffset.toLong()).toEpochDay()
            val taskCount = Random.nextInt(2, 5)
            (0 until taskCount).map { i ->
                val title    = taskTitles[(dayOffset * taskCount + i) % taskTitles.size]
                val prio     = priorities[(dayOffset + i) % priorities.size]
                val progress = when {
                    dayOffset < 7 -> listOf(0, 25, 50, 75, 100).random()  // past week mixed
                    else          -> listOf(0, 0, 25, 50).random()          // next week mostly 0
                }
                val status = when {
                    progress == 100 -> "DONE"
                    progress > 0    -> "IN_PROGRESS"
                    else            -> "PENDING"
                }
                TaskEntity(
                    id              = uuid(),
                    title           = title,
                    description     = "Auto-generated sample task for $title",
                    dateEpochDay    = epochDay,
                    timeMinutes     = if (i % 2 == 0) 9 * 60 + i * 15 else null,
                    priority        = prio,
                    estimatedMinutes = listOf(25, 45, 60, 90).random(),
                    progressPercent = progress,
                    status          = status,
                    completedAt     = if (status == "DONE") now - dayOffset * 86_400_000L else null,
                    createdAt       = now - dayOffset * 86_400_000L,
                    updatedAt       = now,
                )
            }
        }
        db.taskDao().upsertAll(tasks)

        // ── 5. Sessions – 7 past days ─────────────────────────────────────────
        val sessions = mutableListOf<ActivitySessionEntity>()
        for (dayOffset in 0 until 7) {
            val date   = monday.plusDays(dayOffset.toLong())
            val baseMs = epochDayToMs(date)

            // Sleep: starts ~23:00 previous day, ends 6–7 AM this day
            val sleepStartMs = baseMs - 60 * 60_000L + Random.nextLong(-30 * 60_000L, 30 * 60_000L)
            val sleepEndMs   = baseMs + (6 * 60 + Random.nextInt(-30, 60)) * 60_000L
            sessions += session(sleepType.id, "SLEEP", sleepStartMs, sleepEndMs, "ALARM", now)

            // Nap: optional (50% chance)
            if (Random.nextBoolean()) {
                val napStart = baseMs + 13 * 3_600_000L
                sessions += session(napType.id, "NAP", napStart, napStart + 30 * 60_000L, "MANUAL", now)
            }

            // 1-3 focus sessions per day
            val focusCount = Random.nextInt(1, 4)
            var focusStart = baseMs + 9 * 3_600_000L
            repeat(focusCount) {
                val fType    = focusTypes.random()
                val duration = listOf(25, 45, 60).random() * 60_000L
                sessions += session(fType.id, "FOCUS", focusStart, focusStart + duration, "TIMER", now)
                focusStart += duration + 15 * 60_000L  // 15-min break
            }
        }
        db.activitySessionDao().upsertAll(sessions)
    }

    suspend fun clearAll() {
        db.taskDao().deleteAll()
        db.timetableDao().deleteAll()
        db.holidayDao().deleteAll()
        db.alarmDao().deleteAll()
        db.activitySessionDao().deleteAll()
        // Custom activity types are removed; the defaults are put back because
        // alarms and timers always need them (SPEC §2.6).
        db.activityTypeDao().deleteAll()
        db.activityTypeDao().upsertAll(DefaultActivityTypes.all())
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun uuid() = UUID.randomUUID().toString()

    private fun holiday(date: LocalDate, name: String, now: Long) = HolidayEntity(
        id          = uuid(),
        dateEpochDay = date.toEpochDay(),
        name        = name,
        createdAt   = now,
        updatedAt   = now,
    )

    private fun session(
        typeId   : String,
        category : String,
        startMs  : Long,
        endMs    : Long,
        source   : String,
        now      : Long,
    ) = ActivitySessionEntity(
        id        = uuid(),
        typeId    = typeId,
        category  = category,
        startAt   = startMs,
        endAt     = endMs,
        source    = source,
        status    = "COMPLETED",
        createdAt = now,
        updatedAt = now,
    )

    /** Local midnight at the start of [date], in the phone's time zone. */
    private fun epochDayToMs(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
