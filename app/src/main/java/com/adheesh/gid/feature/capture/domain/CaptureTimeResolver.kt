package com.adheesh.gid.feature.capture.domain

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// ── When was this captured? (SPEC §6 capture rules) ──────────────────────────
// A photo shared from the gallery hours later should still be matched to the
// class it was taken in, so the photo's own time beats "now". Apps such as
// WhatsApp strip that time, so we fall back step by step.
object CaptureTimeResolver {

    /**
     * Picks the first usable time: [exifMillis], then [mediaStoreMillis], then [now].
     * A time is usable when it is after 2000-01-01 and not in the future
     * (a phone with a wrong clock, or a corrupt tag, would otherwise mislead).
     */
    fun resolve(exifMillis: Long?, mediaStoreMillis: Long?, now: Long): Long =
        listOf(exifMillis, mediaStoreMillis).firstOrNull { it != null && it in EARLIEST..now } ?: now

    /**
     * EXIF stores "2025:11:04 11:20:05" in the camera's local time, and newer
     * phones add an offset tag like "+05:30". Without the offset we assume the
     * photo was taken in [zone], the phone's current zone.
     */
    fun parseExif(dateTime: String?, offset: String?, zone: ZoneId): Long? {
        val local = runCatching { LocalDateTime.parse(dateTime?.trim() ?: return null, exifFormat) }.getOrNull()
            ?: return null
        val zoneOffset = offset?.let { runCatching { ZoneOffset.of(it.trim()) }.getOrNull() }
        return (if (zoneOffset != null) local.atOffset(zoneOffset).toInstant() else local.atZone(zone).toInstant())
            .toEpochMilli()
    }

    private val exifFormat = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

    private const val EARLIEST = 946_684_800_000L   // 2000-01-01T00:00Z
}
