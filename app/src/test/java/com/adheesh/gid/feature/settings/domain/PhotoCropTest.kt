package com.adheesh.gid.feature.settings.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoCropTest {

    private val window = 300f

    @Test fun `no zoom takes the centred square of a wide photo`() {
        assertEquals(CropSquare(left = 100, top = 0, side = 600), PhotoCrop.square(800, 600, window, 1f, 0f, 0f))
    }

    @Test fun `no zoom takes the centred square of a tall photo`() {
        assertEquals(CropSquare(left = 0, top = 100, side = 600), PhotoCrop.square(600, 800, window, 1f, 0f, 0f))
    }

    @Test fun `zooming in takes a smaller square around the centre`() {
        assertEquals(CropSquare(left = 250, top = 150, side = 300), PhotoCrop.square(800, 600, window, 2f, 0f, 0f))
    }

    @Test fun `dragging the photo right shows more of its left side`() {
        // Scale 0.5: 50 window pixels are 100 image pixels.
        assertEquals(CropSquare(left = 0, top = 0, side = 600), PhotoCrop.square(800, 600, window, 1f, 50f, 0f))
    }

    @Test fun `the offset stops where the photo's edge meets the window`() {
        val scale = PhotoCrop.scale(800, 600, window, 1f)
        assertEquals(50f, PhotoCrop.clampOffset(999f, 800, scale, window), 0.001f)
        assertEquals(-50f, PhotoCrop.clampOffset(-999f, 800, scale, window), 0.001f)
        // The short side exactly fills the window, so it can't move at all.
        assertEquals(0f, PhotoCrop.clampOffset(40f, 600, scale, window), 0.001f)
    }

    @Test fun `the square never leaves the photo`() {
        val square = PhotoCrop.square(801, 601, window, 1f, 9999f, -9999f)
        assertEquals(0, square.left)
        assertEquals(601 - square.side, square.top)
    }
}
