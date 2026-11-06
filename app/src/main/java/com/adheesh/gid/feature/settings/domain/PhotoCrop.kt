package com.adheesh.gid.feature.settings.domain

import kotlin.math.min
import kotlin.math.roundToInt

/** A square of the original image, in its pixels. */
data class CropSquare(val left: Int, val top: Int, val side: Int)

// ── Cropping the profile photo (SPEC §2.5) ───────────────────────────────────
// The image sits behind a square window. At zoom 1 its short side just fills the
// window; the offset is how far its centre is from the window's centre.
object PhotoCrop {

    const val MAX_ZOOM = 5f

    /** Window pixels per image pixel. */
    fun scale(imageWidth: Int, imageHeight: Int, window: Float, zoom: Float): Float =
        window / min(imageWidth, imageHeight) * zoom

    /** Keeps the window fully covered by the image along one axis. */
    fun clampOffset(offset: Float, imageSide: Int, scale: Float, window: Float): Float {
        val room = ((imageSide * scale - window) / 2).coerceAtLeast(0f)
        return offset.coerceIn(-room, room)
    }

    /** The part of the image showing in the window. */
    fun square(imageWidth: Int, imageHeight: Int, window: Float, zoom: Float, offsetX: Float, offsetY: Float): CropSquare {
        val scale = scale(imageWidth, imageHeight, window, zoom)
        val side = (window / scale).roundToInt().coerceIn(1, min(imageWidth, imageHeight))
        val left = (imageWidth / 2f - (window / 2 + offsetX) / scale).roundToInt().coerceIn(0, imageWidth - side)
        val top = (imageHeight / 2f - (window / 2 + offsetY) / scale).roundToInt().coerceIn(0, imageHeight - side)
        return CropSquare(left, top, side)
    }
}
