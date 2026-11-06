package com.adheesh.gid.feature.settings.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.graphics.scale
import com.adheesh.gid.core.database.GIDPreferences
import com.adheesh.gid.core.util.decodeUpright
import com.adheesh.gid.feature.settings.domain.CropSquare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

// ── Profile photo: stored on the device only, never uploaded (SPEC §2.5) ─────
// The photo picker gives us a temporary content:// Uri. After the crop screen, the
// chosen square goes into the app's private files folder, with its path in DataStore.
class ProfilePhotoStore(
    private val context     : Context,
    private val preferences : GIDPreferences,
) {
    val path: Flow<String?> = preferences.profilePhotoPath

    /** The picked image for the crop screen; nothing is saved yet. */
    suspend fun loadForCrop(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        runCatching { decodeUpright(context, uri, CROP_SHORT_SIDE) }.getOrNull()
    }

    /** Saves the chosen square. Returns false if it couldn't be written. */
    suspend fun saveCropped(source: Bitmap, crop: CropSquare): Boolean = withContext(Dispatchers.IO) {
        // A new file name each time makes the avatar reload instead of showing the cached old one.
        val target = File(context.filesDir, "profile_photo_${System.currentTimeMillis()}.jpg")
        val saved = runCatching {
            val square = Bitmap.createBitmap(source, crop.left, crop.top, crop.side, crop.side)
            val finished = if (crop.side > SAVED_SIDE) square.scale(SAVED_SIDE, SAVED_SIDE) else square
            target.outputStream().use { finished.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        }.getOrDefault(false)
        if (!saved) {
            target.delete()
            return@withContext false
        }
        val old = preferences.profilePhotoPath.first()
        preferences.setProfilePhotoPath(target.absolutePath)
        old?.let { File(it).delete() }
        true
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        preferences.profilePhotoPath.first()?.let { File(it).delete() }
        preferences.setProfilePhotoPath(null)
    }

    private companion object {
        /** Enough detail to zoom in on while cropping, without holding a huge photo in memory. */
        const val CROP_SHORT_SIDE = 1600
        const val SAVED_SIDE = 512
        const val JPEG_QUALITY = 90
    }
}
