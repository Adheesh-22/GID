package com.adheesh.gid.core.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID

// ── Small time helpers shared by every feature ───────────────────────────────
// RULES §5: dates are stored as epochDay, clock times as minutes from midnight,
// instants as epoch millis. These helpers convert between those forms using
// the phone's current time zone, so "today" always means the user's local day.

fun newId(): String = UUID.randomUUID().toString()

fun nowMillis(): Long = System.currentTimeMillis()

fun zone(): ZoneId = ZoneId.systemDefault()

fun today(): LocalDate = LocalDate.now(zone())

/** Local calendar day that an instant falls on. */
fun Long.toLocalDate(z: ZoneId = zone()): LocalDate =
    Instant.ofEpochMilli(this).atZone(z).toLocalDate()

fun Long.toLocalDateTime(z: ZoneId = zone()): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(z).toLocalDateTime()

/** Epoch millis of local midnight at the start of [this] day. */
fun LocalDate.startMillis(z: ZoneId = zone()): Long =
    atStartOfDay(z).toInstant().toEpochMilli()

/** Epoch millis of a clock time (minutes from midnight) on [this] day. */
fun LocalDate.atMinutes(minutes: Int, z: ZoneId = zone()): Long =
    atTime(LocalTime.of(minutes / 60, minutes % 60)).atZone(z).toInstant().toEpochMilli()

/** Monday of the week containing [this] date (weeks run Mon–Sun). */
fun LocalDate.weekStart(): LocalDate = with(DayOfWeek.MONDAY)

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

/** "09:30" or "9:30 AM" depending on the phone's locale. */
fun formatMinutesOfDay(minutes: Int): String =
    LocalTime.of((minutes / 60) % 24, minutes % 60).format(timeFormatter)

fun formatTimeOfMillis(millis: Long): String =
    millis.toLocalDateTime().toLocalTime().format(timeFormatter)

/** "1h 20m", "45m" – compact duration used on cards and in History. */
fun formatDuration(totalMinutes: Long): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h == 0L -> "${m}m"
        m == 0L -> "${h}h"
        else    -> "${h}h ${m}m"
    }
}

// Built per call (cheap) so a language change while the app runs is picked up.

/** "Mon 3 Nov" */
fun LocalDate.formatShort(): String = format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))

/** "3 Nov 2026" in the phone's locale */
fun LocalDate.formatMedium(): String = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
