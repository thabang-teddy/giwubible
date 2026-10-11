package com.giwu.bible.repo

import android.util.Log
import com.giwu.bible.data.BibleDatabase
import com.giwu.bible.data.BibleSeed
import com.giwu.bible.data.remote.BibleApi
import com.giwu.bible.model.Bible
import com.giwu.bible.model.Book
import com.giwu.bible.model.ComparisonResult
import com.giwu.bible.model.Verse
import kotlinx.coroutines.CancellationException

/**
 * The reader's single source of bible text.
 *
 * Local first, always: a downloaded translation answers without a round-trip,
 * and asking the database before the network also covers the window where the
 * downloaded flag has not caught up with what is actually on disk (right after
 * a file sync, for instance). The network is the fallback, and the bundled
 * seed lists are the last resort so the book list and translation picker are
 * never empty — even offline on a fresh install.
 */
class BibleRepository(
    private val db: BibleDatabase,
    private val api: BibleApi,
) {

    suspend fun isSetupComplete(): Boolean = db.isSetupComplete()

    /** Translations with their downloaded flags. */
    suspend fun bibles(): List<Bible> {
        bestEffort("local bibles") { db.getBibles() }
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }
        bestEffort("remote bibles") { api.getBibles() }
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }
        return BibleSeed.versions
    }

    /** The book list, persisted on the way through when it comes from the API. */
    suspend fun books(): List<Book> {
        bestEffort("local books") { db.getBooks() }
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }

        val remote = bestEffort("remote books") { api.getBooks() }?.takeIf { it.isNotEmpty() }
        if (remote != null) {
            bestEffort("book cache write") { db.saveBooks(remote) }
            return remote
        }
        return BibleSeed.books
    }

    /** Every verse of one chapter. Throws when neither source can answer. */
    suspend fun chapter(bible: String, book: Int, chapter: Int): List<Verse> {
        bestEffort("local chapter") { db.getChapter(bible, book, chapter) }
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }
        return api.getChapter(bible, book, chapter)
    }

    /**
     * One verse in another translation, or null when nobody can supply it —
     * the panel renders that as "not available" rather than as an error.
     */
    suspend fun comparison(
        bible: String,
        book: Int,
        chapter: Int,
        verse: Int,
    ): ComparisonResult? {
        bestEffort("local verse") { db.getVerse(bible, book, chapter, verse) }
            ?.let { return it }
        return bestEffort("remote verse") { api.getVerse(bible, book, chapter, verse) }
    }

    /**
     * Downloads [table] and stores it.
     *
     * [onStage] reports which phase is running so the setup screen can say
     * "Downloading KJV…" and then "Storing KJV…".
     */
    suspend fun downloadBible(table: String, onStage: (DownloadStage) -> Unit = {}) {
        BibleDatabase.requireBibleTable(table)

        onStage(DownloadStage.Downloading)
        val payload = api.downloadBible(table)

        onStage(DownloadStage.Storing(written = 0, total = payload.verses.size))
        // Register the metadata first: marking the translation downloaded is an
        // UPDATE, and bible_version_key can still be empty when the list call
        // never happened (offline first launch).
        db.saveBiblesList(listOf(payload.bible))
        db.saveBibleVerses(table, payload.verses) { written, total ->
            onStage(DownloadStage.Storing(written, total))
        }
    }

    /** What the database knows, with no network fallback. */
    suspend fun localBibles(): List<Bible> = db.getBibles()

    /**
     * Refreshes the catalogue from the server and returns the merged list.
     *
     * Used by the setup screen, which wants to show translations added on the
     * server since the last launch. Book names come along for the ride while
     * there is connectivity. Downloaded flags are never clobbered.
     */
    suspend fun refreshCatalog(): List<Bible> {
        val remote = api.getBibles()
        if (remote.isNotEmpty()) db.saveBiblesList(remote)

        bestEffort("remote books") { api.getBooks() }
            ?.takeIf { it.isNotEmpty() }
            ?.let { db.saveBooks(it) }

        return db.getBibles()
    }

    /** Copies every translation out of an uploaded SQLite file. */
    suspend fun syncFromSqliteFile(
        path: String,
        onProgress: (table: String, current: Int, total: Int) -> Unit = { _, _, _ -> },
    ): List<String> = db.syncFromSqliteFile(path, onProgress)

    /** Drops every downloaded translation and returns the app to setup. */
    suspend fun reset() = db.reset()

    suspend fun resolveDefaultBible(): String = db.resolveDefaultBible()

    /**
     * Runs [block], turning an expected miss into null.
     *
     * A local read failing because a translation is not downloaded yet, or a
     * remote read failing because the device is offline, is the normal case
     * here — the caller falls through to the next source. Cancellation is not
     * a failure and is rethrown.
     */
    private inline fun <T> bestEffort(what: String, block: () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "$what unavailable: ${e.message}")
            null
        }

    private companion object {
        const val TAG = "BibleRepository"
    }
}

/** Where a translation download has got to. */
sealed interface DownloadStage {
    data object Downloading : DownloadStage
    data class Storing(val written: Int, val total: Int) : DownloadStage
}
