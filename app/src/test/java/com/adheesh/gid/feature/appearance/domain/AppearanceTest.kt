package com.adheesh.gid.feature.appearance.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceTest {

    @Test fun greetingUsesTypedNameFirst() {
        assertEquals("Addy", Appearance(homeName = "Addy").greetingName("Adheesh Jain"))
    }

    @Test fun greetingFallsBackToProfileFirstName() {
        assertEquals("Adheesh", Appearance().greetingName("Adheesh Jain"))
        assertEquals("Adheesh", Appearance(homeName = "  ").greetingName(" Adheesh Jain"))
        assertNull(Appearance().greetingName(null))
    }

    @Test fun unknownStoredOptionsUseDefaults() {
        assertEquals(NameFont.GID, NameFont.of("FANCY"))
        assertEquals(NameColor.TEXT, NameColor.of(null))
        assertEquals(NameColor.CORAL, NameColor.of("CORAL"))
    }

    @Test fun blurKeepsAFlatImageTheSame() {
        val grey = 0xFF808080.toInt()
        val pixels = IntArray(12 * 8) { grey }
        BoxBlur.blur(pixels, 12, 8, radius = 3)
        assertTrue(pixels.all { it == grey })
    }

    @Test fun blurSpreadsABrightDotAndKeepsItsCentreBrightest() {
        val w = 15; val h = 15
        val pixels = IntArray(w * h) { 0xFF000000.toInt() }
        pixels[7 * w + 7] = 0xFFFFFFFF.toInt()
        BoxBlur.blur(pixels, w, h, radius = 2, passes = 1)
        val centre = pixels[7 * w + 7] and 0xFF
        val near   = pixels[7 * w + 8] and 0xFF
        val far    = pixels[0] and 0xFF
        assertTrue(centre in 1 until 255)
        assertEquals(centre, near)          // inside the same 5 × 5 box
        assertEquals(0, far)
        assertEquals(0xFF, pixels[0] ushr 24) // stays opaque
    }

    @Test fun radiusZeroChangesNothing() {
        val pixels = intArrayOf(1, 2, 3, 4)
        BoxBlur.blur(pixels, 2, 2, radius = 0)
        assertEquals(listOf(1, 2, 3, 4), pixels.toList())
    }

    @Test fun darkTextOnALightPhotoLightTextOnADarkOne() {
        assertTrue(BackdropContrast.prefersDarkText(240, 240, 235, dim = 0f))
        assertTrue(!BackdropContrast.prefersDarkText(20, 20, 30, dim = 0f))
    }

    @Test fun dimmingALightPhotoSwitchesBackToLightText() {
        assertTrue(BackdropContrast.prefersDarkText(250, 250, 250, dim = 0.1f))
        assertTrue(!BackdropContrast.prefersDarkText(250, 250, 250, dim = 0.85f))
    }
}
