package com.adheesh.gid.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

/** Decodes a picked image with its short side at most [targetShortSide] pixels, the right way up. */
fun decodeUpright(context: Context, uri: Uri, targetShortSide: Int): Bitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    // Reads only the size; decodeStream returns null here by design.
    val stream = resolver.openInputStream(uri) ?: return null
    stream.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= targetShortSide) sample *= 2
    val decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return null

    val rotation = resolver.openInputStream(uri)?.use {
        runCatching { ExifInterface(it).rotationDegrees }.getOrDefault(0)
    } ?: 0
    val shortSide = minOf(decoded.width, decoded.height)
    val scale = (targetShortSide.toFloat() / shortSide).coerceAtMost(1f)
    if (rotation == 0 && scale == 1f) return decoded
    val matrix = Matrix().apply { postScale(scale, scale); postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        .also { if (it !== decoded) decoded.recycle() }
}
