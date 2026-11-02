package com.adheesh.gid.feature.settings.data

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.adheesh.gid.BuildConfig
import com.adheesh.gid.MainActivity
import com.adheesh.gid.core.database.GIDDatabase
import com.adheesh.gid.core.database.GIDPreferences
import com.adheesh.gid.core.util.nowMillis
import com.adheesh.gid.feature.capture.data.CaptureFileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.system.exitProcess

/** Why an import was refused; null means it worked. */
enum class BackupError { UNREADABLE, NOT_A_BACKUP, NEWER_VERSION }

// ── Backup and restore (SPEC §2.9) ───────────────────────────────────────────
// Everything lives on the phone, so losing the phone would lose it all. Export
// writes one zip the user keeps wherever they like; import puts it back.
//
// Zip layout:  manifest.json · database/<db files> · captures/<files> · photo/<profile photo>
class BackupManager(
    private val context     : Context,
    private val database    : GIDDatabase,
    private val preferences : GIDPreferences,
    private val captures    : CaptureFileStore,
) {

    /** Writes the backup to [target] (a document the user picked). Returns false on failure. */
    suspend fun export(target: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            // Room writes to a WAL file first; a checkpoint folds it into the main file
            // so the copy is complete and consistent.
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
            val dbFile = context.getDatabasePath(GIDDatabase.DATABASE_NAME)
            val photo = preferences.profilePhotoPath.first()?.let { File(it) }?.takeIf { it.exists() }

            context.contentResolver.openOutputStream(target)?.use { out ->
                ZipOutputStream(out.buffered()).use { zip ->
                    zip.putEntry(MANIFEST, manifest().toString(2).byteInputStream())
                    listOf(dbFile, File(dbFile.path + "-wal"), File(dbFile.path + "-shm"))
                        .filter { it.exists() }
                        .forEach { f -> f.inputStream().use { zip.putEntry("$DB_DIR/${f.name}", it) } }
                    captures.folder.listFiles().orEmpty().filter { it.isFile }.forEach { f ->
                        f.inputStream().use { zip.putEntry("$CAPTURES_DIR/${f.name}", it) }
                    }
                    photo?.inputStream()?.use { zip.putEntry("$PHOTO_DIR/${photo.name}", it) }
                }
            } ?: error("Can't open the chosen file")
            true
        }.getOrDefault(false)
    }

    /**
     * Checks [source] and, if it's a GID backup this version can read, replaces
     * all local data with it. On success the app restarts and this never returns.
     */
    suspend fun import(source: Uri): BackupError? = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        try {
            val opened = context.contentResolver.openInputStream(source) ?: return@withContext BackupError.UNREADABLE
            opened.use { unzip(it, staging) } ?: return@withContext BackupError.UNREADABLE

            val manifest = File(staging, MANIFEST).takeIf { it.exists() }
                ?.let { runCatching { JSONObject(it.readText()) }.getOrNull() }
                ?.takeIf { it.optString("app") == APP_TAG }
                ?: return@withContext BackupError.NOT_A_BACKUP
            // Room can migrate an older database forward, but never a newer one back.
            if (manifest.optInt("dbVersion", Int.MAX_VALUE) > DB_VERSION) return@withContext BackupError.NEWER_VERSION
            val dbFiles = File(staging, DB_DIR).listFiles().orEmpty()
            if (dbFiles.none { it.name == GIDDatabase.DATABASE_NAME }) return@withContext BackupError.NOT_A_BACKUP

            // Point of no return: swap the files in while nothing has the database open.
            database.close()
            val dbFile = context.getDatabasePath(GIDDatabase.DATABASE_NAME)
            listOf("", "-wal", "-shm", "-journal").forEach { File(dbFile.path + it).delete() }
            dbFiles.forEach { it.copyTo(File(dbFile.parentFile, it.name), overwrite = true) }

            captures.folder.listFiles().orEmpty().forEach { it.delete() }
            File(staging, CAPTURES_DIR).listFiles().orEmpty().forEach { it.copyTo(File(captures.folder, it.name), overwrite = true) }
            relinkCaptureFiles(dbFile)

            val photo = File(staging, PHOTO_DIR).listFiles().orEmpty().firstOrNull()
            preferences.setProfilePhotoPath(photo?.copyTo(File(context.filesDir, photo.name), overwrite = true)?.absolutePath)

            restart()
        } finally {
            staging.deleteRecursively()
        }
    }

    // Capture rows store absolute paths; on another phone (or user profile) the
    // files folder can differ, so point every row at today's folder by file name.
    private fun relinkCaptureFiles(dbFile: File) {
        runCatching {
            SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                val rows = db.rawQuery("SELECT id, filePath FROM captures WHERE filePath IS NOT NULL", null).use { c ->
                    buildList { while (c.moveToNext()) add(c.getString(0) to c.getString(1)) }
                }
                rows.forEach { (id, path) ->
                    db.execSQL("UPDATE captures SET filePath = ? WHERE id = ?", arrayOf(File(captures.folder, File(path).name).absolutePath, id))
                }
            }
        }   // A version-1 backup has no captures table: nothing to relink.
    }

    // Reopening Room in this process would keep the closed instance around in
    // every repository, so start the app fresh instead.
    private fun restart(): Nothing {
        context.startActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        exitProcess(0)
    }

    private fun manifest() = JSONObject()
        .put("app", APP_TAG)
        .put("versionCode", BuildConfig.VERSION_CODE)
        .put("dbVersion", DB_VERSION)
        .put("createdAt", nowMillis())

    /** Unzips into [into], refusing entries that would escape it ("zip slip"). Null on a bad zip. */
    private fun unzip(input: InputStream, into: File): Unit? = runCatching {
        val root = into.canonicalPath + File.separator
        ZipInputStream(input.buffered()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                val out = File(into, entry.name)
                require(out.canonicalPath.startsWith(root)) { "Bad entry ${entry.name}" }
                if (entry.isDirectory) out.mkdirs()
                else { out.parentFile?.mkdirs(); out.outputStream().use { zip.copyTo(it) } }
            }
        }
    }.getOrNull()

    private fun ZipOutputStream.putEntry(name: String, data: InputStream) {
        putNextEntry(ZipEntry(name))
        data.copyTo(this)
        closeEntry()
    }

    companion object {
        private const val APP_TAG      = "GID"
        private const val MANIFEST     = "manifest.json"
        private const val DB_DIR       = "database"
        private const val CAPTURES_DIR = "captures"
        private const val PHOTO_DIR    = "photo"
        /** Keep in step with GIDDatabase's version. */
        const val DB_VERSION = 2
    }
}
