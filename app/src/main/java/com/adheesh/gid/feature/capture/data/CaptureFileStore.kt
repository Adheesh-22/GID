package com.adheesh.gid.feature.capture.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.adheesh.gid.core.util.zone
import com.adheesh.gid.feature.capture.domain.CaptureTimeResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** A file copied into app storage, with whatever the source said about when it was taken. */
data class StoredFile(
    val path             : String,
    val mimeType         : String,
    val exifMillis       : Long?,
    val mediaStoreMillis : Long?,
    /** The file's name in the app it came from, e.g. "Unit 3 notes.pdf". */
    val displayName      : String?,
)

// ── Capture files: stored on the device only (SPEC §2.8, RULES §6) ───────────
// Shared Uris are temporary (the other app can revoke them), so every photo or
// PDF is copied into filesDir/captures. Deleting it from the gallery later
// doesn't break GID, and Backup can zip the whole folder.
class CaptureFileStore(private val context: Context) {

    val folder: File get() = File(context.filesDir, FOLDER).apply { mkdirs() }

    /** Copies [uri] into app storage. Returns null when it couldn't be read. */
    suspend fun copyIn(uri: Uri, id: String, mimeHint: String?): StoredFile? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = mimeHint?.takeIf { it != "*/*" } ?: resolver.getType(uri) ?: "application/octet-stream"
        val target = File(folder, "$id.${extensionFor(mime)}")
        val copied = runCatching {
            resolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } } != null
        }.getOrDefault(false)
        if (!copied) {
            target.delete()
            return@withContext null
        }
        StoredFile(
            path             = target.absolutePath,
            mimeType         = mime,
            exifMillis       = if (mime.startsWith("image/")) exifTime(target) else null,
            mediaStoreMillis = dateTaken(uri),
            displayName      = displayName(uri),
        )
    }

    /** A file the camera app writes into, plus the content Uri it is handed. */
    fun newCameraFile(id: String): Pair<File, Uri> {
        val file = File(folder, "$id.jpg")
        return file to FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    fun exifTime(file: File): Long? = runCatching {
        val exif = ExifInterface(file)
        CaptureTimeResolver.parseExif(
            exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: exif.getAttribute(ExifInterface.TAG_DATETIME),
            exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL),
            zone(),
        )
    }.getOrNull()

    suspend fun delete(path: String?) {
        withContext(Dispatchers.IO) {
            // Only ever delete inside our own folder, whatever the database says.
            path?.let { File(it) }?.takeIf { it.parentFile == folder }?.delete()
        }
    }

    // Gallery Uris know when the photo was taken; other apps' Uris usually don't.
    private fun dateTaken(uri: Uri): Long? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATE_TAKEN), null, null, null)?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
            }
        }.getOrNull()
    }

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun extensionFor(mime: String): String =
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: if (mime.startsWith("image/")) "jpg" else "bin"

    companion object {
        const val FOLDER = "captures"
    }
}
