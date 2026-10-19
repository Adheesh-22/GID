package com.adheesh.gid.feature.settings.data

import android.content.Context
import android.net.Uri
import com.adheesh.gid.core.database.GIDPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

// ── Profile photo: stored on the device only, never uploaded (SPEC §2.5) ─────
// The photo picker gives us a temporary content:// Uri, so we copy the image
// into the app's private files folder and remember the file path in DataStore.
class ProfilePhotoStore(
    private val context     : Context,
    private val preferences : GIDPreferences,
) {
    val path: Flow<String?> = preferences.profilePhotoPath

    /** Copies the picked image into app storage. Returns false if it couldn't be read. */
    suspend fun save(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        // A new file name each time makes the avatar reload instead of showing the cached old one.
        val target = File(context.filesDir, "profile_photo_${System.currentTimeMillis()}.jpg")
        val copied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } != null
        }.getOrDefault(false)
        if (!copied) {
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
}
