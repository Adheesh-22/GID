package com.adheesh.gid.feature.appearance.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.adheesh.gid.core.database.GIDPreferences
import com.adheesh.gid.feature.appearance.domain.Appearance
import com.adheesh.gid.feature.appearance.domain.BoxBlur
import com.adheesh.gid.feature.appearance.domain.NameColor
import com.adheesh.gid.feature.appearance.domain.NameFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

// ── Appearance: stored on the device only (SPEC §2.2) ────────────────────────
// The background photo is shrunk, blurred once and saved in the app's private
// files; the original is never kept. Nothing here is synced or backed up.
class AppearanceStore(
    private val context     : Context,
    private val preferences : GIDPreferences,
) {
    val appearance: Flow<Appearance> = combine(
        preferences.homeName,
        preferences.homeNameFont,
        preferences.homeNameColor,
        preferences.backgroundPath,
        preferences.backgroundDim,
    ) { name, font, color, path, dim ->
        Appearance(name, NameFont.of(font), NameColor.of(color), path, dim)
    }

    suspend fun setHomeName(name: String?) = preferences.setHomeName(name)
    suspend fun setFont(font: NameFont) = preferences.setHomeNameFont(font.name)
    suspend fun setColor(color: NameColor) = preferences.setHomeNameColor(color.name)

    suspend fun setDim(dim: Float) =
        preferences.setBackgroundDim(dim.coerceIn(0f, Appearance.MAX_DIM))

    /** Blurs the picked photo and makes it the background. Returns false if it couldn't be read. */
    suspend fun setBackground(uri: Uri): Boolean = withContext(Dispatchers.Default) {
        val blurred = runCatching { decodeSmall(uri)?.let(::blur) }.getOrNull() ?: return@withContext false
        // A new file name each time so the screen reloads instead of showing the cached old one.
        val target = File(context.filesDir, "background_${System.currentTimeMillis()}.jpg")
        val saved = withContext(Dispatchers.IO) {
            runCatching { target.outputStream().use { blurred.compress(Bitmap.CompressFormat.JPEG, 90, it) } }
                .getOrDefault(false)
        }
        blurred.recycle()
        if (!saved) {
            target.delete()
            return@withContext false
        }
        val old = preferences.backgroundPath.first()
        preferences.setBackgroundPath(target.absolutePath)
        old?.let { File(it).delete() }
        true
    }

    suspend fun clearBackground() = withContext(Dispatchers.IO) {
        preferences.backgroundPath.first()?.let { File(it).delete() }
        preferences.setBackgroundPath(null)
    }

    /** Decodes about [TARGET_WIDTH] pixels wide, the right way up. */
    private fun decodeSmall(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // Reads only the size; decodeStream returns null here by design.
        val stream = resolver.openInputStream(uri) ?: return null
        stream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= TARGET_WIDTH) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val rotation = resolver.openInputStream(uri)?.use {
            runCatching { ExifInterface(it).rotationDegrees }.getOrDefault(0)
        } ?: 0
        val shortSide = minOf(decoded.width, decoded.height)
        val scale = (TARGET_WIDTH.toFloat() / shortSide).coerceAtMost(1f)
        if (rotation == 0 && scale == 1f) return decoded
        val matrix = Matrix().apply { postScale(scale, scale); postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            .also { if (it !== decoded) decoded.recycle() }
    }

    private fun blur(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        source.recycle()
        BoxBlur.blur(pixels, w, h, BLUR_RADIUS)
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    private companion object {
        /** Short side of the saved photo; it is blurred, so more pixels add nothing. */
        const val TARGET_WIDTH = 480
        const val BLUR_RADIUS  = 12
    }
}
