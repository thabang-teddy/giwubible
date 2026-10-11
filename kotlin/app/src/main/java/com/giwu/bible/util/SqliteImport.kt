package com.giwu.bible.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Prepares a reader-supplied SQLite file for import.
 *
 * A document picker hands back a content URI, and SQLite needs a real file, so
 * the pick is copied into the cache first. The copy is also what makes the
 * validation below safe: an arbitrary file is opened read-only and has to
 * answer one metadata query before anything is imported from it.
 */
object SqliteImport {

    private const val CACHE_NAME = "imported_bible.db"

    /** Copies [uri] into the cache and returns the file, or null if unreadable. */
    suspend fun stage(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        val target = File(context.cacheDir, CACHE_NAME)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            target
        } catch (e: Exception) {
            target.delete()
            null
        }
    }

    /** True when [file] is a SQLite database that holds the bible metadata. */
    suspend fun looksLikeBibleDatabase(file: File): Boolean = withContext(Dispatchers.IO) {
        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(
                file.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
            db.rawQuery("SELECT 1 FROM bible_version_key LIMIT 1", null).use { it.moveToFirst() }
            true
        } catch (e: Exception) {
            false
        } finally {
            db?.close()
        }
    }

    fun discard(context: Context) {
        File(context.cacheDir, CACHE_NAME).delete()
    }
}
