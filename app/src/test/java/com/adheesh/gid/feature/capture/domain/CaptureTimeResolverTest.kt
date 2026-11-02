package com.adheesh.gid.feature.capture.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class CaptureTimeResolverTest {

    private val now      = 1_762_250_000_000L   // Nov 2025
    private val hourAgo  = now - 3_600_000L
    private val dayAgo   = now - 86_400_000L

    @Test fun `exif time wins`() {
        assertEquals(hourAgo, CaptureTimeResolver.resolve(hourAgo, dayAgo, now))
    }

    @Test fun `falls back to gallery date when exif is missing`() {
        assertEquals(dayAgo, CaptureTimeResolver.resolve(null, dayAgo, now))
    }

    @Test fun `falls back to now when nothing is known`() {
        assertEquals(now, CaptureTimeResolver.resolve(null, null, now))
    }

    @Test fun `zero and future times are ignored`() {
        assertEquals(dayAgo, CaptureTimeResolver.resolve(0L, dayAgo, now))
        assertEquals(now, CaptureTimeResolver.resolve(now + 60_000L, 0L, now))
    }

    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val elevenTwenty = ZonedDateTime.of(2025, 11, 4, 11, 20, 5, 0, kolkata).toInstant().toEpochMilli()

    @Test fun `exif time without offset is read in the phone's zone`() {
        assertEquals(elevenTwenty, CaptureTimeResolver.parseExif("2025:11:04 11:20:05", null, kolkata))
    }

    @Test fun `exif offset beats the phone's zone`() {
        assertEquals(elevenTwenty, CaptureTimeResolver.parseExif("2025:11:04 11:20:05", "+05:30", ZoneId.of("UTC")))
    }

    @Test fun `bad exif text gives null`() {
        assertNull(CaptureTimeResolver.parseExif("0000:00:00 00:00:00", null, kolkata))
        assertNull(CaptureTimeResolver.parseExif(null, null, kolkata))
    }
}
