package com.giwu.bible.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.giwu.bible.model.Bible
import com.giwu.bible.model.Book
import com.giwu.bible.model.ComparisonResult
import com.giwu.bible.model.Verse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The on-device bible store.
 *
 * Mirrors the schema of the server's `bible-sqlite.db` so a downloaded
 * translation can be read with the same queries the API uses: one `t_*` table
 * per translation plus the `bible_version_key` and `key_english` metadata
 * tables. `app_settings` is local only and holds the server URL.
 *
 * Every call suspends onto [io]; nothing here touches the main thread.
 */
class BibleDatabase internal constructor(
    private val helper: SQLiteOpenHelper,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {

    // ── Setup status ───────────────────────────────────────────────────────

    /** True once at least one translation has been downloaded. */
    suspend fun isSetupComplete(): Boolean = read {
        it.rawQuery(
            "SELECT COUNT(*) FROM bible_version_key WHERE downloaded = 1",
            null,
        ).firstInt() > 0
    }

    suspend fun isBibleDownloaded(table: String): Boolean = read {
        it.rawQuery(
            "SELECT downloaded FROM bible_version_key WHERE \"table\" = ? LIMIT 1",
            arrayOf(table),
        ).firstInt() == 1
    }

    // ── Bibles ─────────────────────────────────────────────────────────────

    suspend fun getBibles(): List<Bible> = read { db ->
        db.rawQuery(
            "SELECT \"table\", abbreviation, version, downloaded FROM bible_version_key " +
                "ORDER BY abbreviation ASC",
            null,
        ).map { it.toBible() }
    }

    /**
     * Registers [bibles] without touching rows that already exist, so a list
     * refresh from the API never clears a downloaded flag.
     */
    suspend fun saveBiblesList(bibles: List<Bible>) = write { db ->
        db.transaction {
            val stmt = db.compileStatement(
                "INSERT OR IGNORE INTO bible_version_key " +
                    "(\"table\", abbreviation, version, downloaded) VALUES (?, ?, ?, 0)",
            )
            stmt.use {
                for (bible in bibles) {
                    it.clearBindings()
                    it.bindString(1, bible.table)
                    it.bindString(2, bible.abbreviation)
                    it.bindString(3, bible.version)
                    it.executeInsert()
                }
            }
        }
    }

    suspend fun markBibleDownloaded(table: String, downloaded: Boolean) = write { db ->
        db.execSQL(
            "UPDATE bible_version_key SET downloaded = ? WHERE \"table\" = ?",
            arrayOf<Any>(if (downloaded) 1 else 0, table),
        )
    }

    // ── Books ──────────────────────────────────────────────────────────────

    suspend fun getBooks(): List<Book> = read { db ->
        db.rawQuery("SELECT b, n, t FROM key_english ORDER BY b ASC", null)
            .map { it.toBook() }
    }

    suspend fun saveBooks(books: List<Book>) = write { db ->
        db.transaction {
            val stmt = db.compileStatement(
                "INSERT OR REPLACE INTO key_english (b, n, t) VALUES (?, ?, ?)",
            )
            stmt.use {
                for (book in books) {
                    it.clearBindings()
                    it.bindLong(1, book.number.toLong())
                    it.bindString(2, book.name)
                    book.testament?.let { t -> it.bindString(3, t) }
                    it.executeInsert()
                }
            }
        }
    }

    // ── Verses ─────────────────────────────────────────────────────────────

    suspend fun getChapter(bibleTable: String, book: Int, chapter: Int): List<Verse> {
        requireBibleTable(bibleTable)
        return read { db ->
            db.rawQuery(
                "SELECT b, c, v, t FROM $bibleTable WHERE b = ? AND c = ? ORDER BY v ASC",
                arrayOf(book.toString(), chapter.toString()),
            ).map { it.toVerse() }
        }
    }

    suspend fun getVerse(
        bibleTable: String,
        book: Int,
        chapter: Int,
        verse: Int,
    ): ComparisonResult? {
        requireBibleTable(bibleTable)
        return read { db ->
            val text = db.rawQuery(
                "SELECT t FROM $bibleTable WHERE b = ? AND c = ? AND v = ? LIMIT 1",
                arrayOf(book.toString(), chapter.toString(), verse.toString()),
            ).firstOrNull { it.getString(0) } ?: return@read null

            db.rawQuery(
                "SELECT \"table\", abbreviation, version FROM bible_version_key " +
                    "WHERE \"table\" = ? LIMIT 1",
                arrayOf(bibleTable),
            ).firstOrNull { row ->
                ComparisonResult(
                    bible = row.getString(0),
                    version = row.getString(2),
                    abbreviation = row.getString(1),
                    text = text,
                )
            }
        }
    }

    /**
     * Creates the verse table for [bibleTable], replaces its rows with
     * [verses], and marks the translation as downloaded.
     *
     * [onProgress] reports rows written so a long insert can show movement.
     */
    suspend fun saveBibleVerses(
        bibleTable: String,
        verses: List<Verse>,
        onProgress: ((written: Int, total: Int) -> Unit)? = null,
    ) {
        requireBibleTable(bibleTable)
        write { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS $bibleTable (" +
                    "b INTEGER NOT NULL, c INTEGER NOT NULL, v INTEGER NOT NULL, " +
                    "t TEXT NOT NULL, PRIMARY KEY (b, c, v))",
            )
            db.transaction {
                val stmt = db.compileStatement(
                    "INSERT OR REPLACE INTO $bibleTable (b, c, v, t) VALUES (?, ?, ?, ?)",
                )
                stmt.use {
                    verses.forEachIndexed { index, verse ->
                        it.clearBindings()
                        it.bindLong(1, verse.book.toLong())
                        it.bindLong(2, verse.chapter.toLong())
                        it.bindLong(3, verse.number.toLong())
                        it.bindString(4, verse.text)
                        it.executeInsert()
                        if (onProgress != null && (index + 1) % PROGRESS_STEP == 0) {
                            onProgress(index + 1, verses.size)
                        }
                    }
                }
            }
        }
        markBibleDownloaded(bibleTable, downloaded = true)
        onProgress?.invoke(verses.size, verses.size)
    }

    // ── App settings ───────────────────────────────────────────────────────

    /** The stored server URL, or null when the reader has never changed it. */
    suspend fun getServerUrl(): String? = appSetting(KEY_SERVER_URL)

    suspend fun saveServerUrl(url: String) = write { db ->
        db.execSQL(
            "INSERT OR REPLACE INTO app_settings (key, value) VALUES (?, ?)",
            arrayOf(KEY_SERVER_URL, url),
        )
    }

    /**
     * Which translation to read after a reset.
     *
     * Priority: an explicit `default_bible` setting, then KJV if downloaded,
     * then the first downloaded translation, then KJV as a hard fallback (the
     * reader shows an empty chapter until something is downloaded).
     */
    suspend fun resolveDefaultBible(): String {
        appSetting(KEY_DEFAULT_BIBLE)?.let { return it }
        if (isBibleDownloaded(FALLBACK_BIBLE)) return FALLBACK_BIBLE
        return read { db ->
            db.rawQuery(
                "SELECT \"table\" FROM bible_version_key WHERE downloaded = 1 " +
                    "ORDER BY abbreviation ASC LIMIT 1",
                null,
            ).firstOrNull { it.getString(0) }
        } ?: FALLBACK_BIBLE
    }

    private suspend fun appSetting(key: String): String? = read { db ->
        db.rawQuery(
            "SELECT value FROM app_settings WHERE key = ? LIMIT 1",
            arrayOf(key),
        ).firstOrNull { it.getString(0) }
    }

    // ── Reset ──────────────────────────────────────────────────────────────

    /**
     * Drops every downloaded verse table and clears metadata, then re-seeds
     * the bundled lists so the setup screen works offline straight away.
     * After this call [isSetupComplete] is false.
     */
    suspend fun reset() = write { db ->
        val tables = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name LIKE 't\\_%' ESCAPE '\\'",
            null,
        ).map { it.getString(0) }

        for (name in tables) {
            if (BIBLE_TABLE.matches(name)) db.execSQL("DROP TABLE IF EXISTS $name")
        }
        db.execSQL("DELETE FROM bible_version_key")
        db.execSQL("DELETE FROM key_english")
        seed(db)
    }

    // ── Sync from an uploaded SQLite file ──────────────────────────────────

    /**
     * Copies every bible table out of the SQLite file at [path] into this
     * database. Returns the table names that were synced.
     */
    suspend fun syncFromSqliteFile(
        path: String,
        onProgress: ((bibleTable: String, current: Int, total: Int) -> Unit)? = null,
    ): List<String> {
        val source = withContext(io) {
            SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
        }
        val synced = mutableListOf<String>()
        try {
            val sourceBibles = withContext(io) { source.readBiblesList() }
            saveBiblesList(sourceBibles)

            val sourceBooks = withContext(io) { source.readBooks() }
            if (sourceBooks.isNotEmpty()) saveBooks(sourceBooks)

            sourceBibles.forEachIndexed { index, bible ->
                if (!withContext(io) { source.hasTable(bible.table) }) return@forEachIndexed
                onProgress?.invoke(bible.table, index + 1, sourceBibles.size)

                requireBibleTable(bible.table)
                val verses = withContext(io) {
                    source.rawQuery(
                        "SELECT b, c, v, t FROM ${bible.table} ORDER BY b, c, v",
                        null,
                    ).map { it.toVerse() }
                }
                saveBibleVerses(bible.table, verses)
                synced += bible.table
            }
        } finally {
            withContext(io) { source.close() }
        }
        return synced
    }

    private fun SQLiteDatabase.readBiblesList(): List<Bible> = runCatching {
        rawQuery(
            "SELECT \"table\", abbreviation, version FROM bible_version_key",
            null,
        ).map { Bible(it.getString(0), it.getString(1), it.getString(2)) }
    }.getOrDefault(emptyList())

    private fun SQLiteDatabase.readBooks(): List<Book> = runCatching {
        rawQuery("SELECT b, n, t FROM key_english ORDER BY b ASC", null)
            .map { it.toBook() }
    }.getOrDefault(emptyList())

    private fun SQLiteDatabase.hasTable(name: String): Boolean =
        rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(name),
        ).use { it.moveToFirst() }

    // ── Plumbing ───────────────────────────────────────────────────────────

    private suspend fun <T> read(block: (SQLiteDatabase) -> T): T =
        withContext(io) { block(helper.readableDatabase) }

    private suspend fun <T> write(block: (SQLiteDatabase) -> T): T =
        withContext(io) { block(helper.writableDatabase) }

    companion object {
        const val FALLBACK_BIBLE = "t_kjv"

        private const val DB_NAME = "giwu_bible.db"
        private const val DB_VERSION = 1
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_DEFAULT_BIBLE = "default_bible"
        private const val PROGRESS_STEP = 2_000

        /** Verse tables are interpolated into SQL, so the name is allowlisted. */
        private val BIBLE_TABLE = Regex("^t_[a-z0-9]+$")

        fun open(context: Context, io: CoroutineDispatcher = Dispatchers.IO): BibleDatabase =
            BibleDatabase(Helper(context.applicationContext), io)

        internal fun requireBibleTable(table: String) {
            require(BIBLE_TABLE.matches(table)) { "Invalid bible table name: $table" }
        }

        /**
         * Inserts the bundled metadata. `INSERT OR IGNORE` keeps existing rows
         * (and their downloaded flags) intact, so this is safe to run on every
         * open and also backfills databases created before a seed changed.
         */
        private fun seed(db: SQLiteDatabase) {
            db.transaction {
                db.compileStatement(
                    "INSERT OR IGNORE INTO bible_version_key " +
                        "(\"table\", abbreviation, version, downloaded) VALUES (?, ?, ?, 0)",
                ).use { stmt ->
                    for (bible in BibleSeed.versions) {
                        stmt.clearBindings()
                        stmt.bindString(1, bible.table)
                        stmt.bindString(2, bible.abbreviation)
                        stmt.bindString(3, bible.version)
                        stmt.executeInsert()
                    }
                }
                db.compileStatement(
                    "INSERT OR IGNORE INTO key_english (b, n, t) VALUES (?, ?, ?)",
                ).use { stmt ->
                    for (book in BibleSeed.books) {
                        stmt.clearBindings()
                        stmt.bindLong(1, book.number.toLong())
                        stmt.bindString(2, book.name)
                        book.testament?.let { stmt.bindString(3, it) }
                        stmt.executeInsert()
                    }
                }
            }
        }

        private class Helper(context: Context) :
            SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS bible_version_key (" +
                        "\"table\" TEXT PRIMARY KEY, abbreviation TEXT NOT NULL, " +
                        "version TEXT NOT NULL, downloaded INTEGER NOT NULL DEFAULT 0)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS key_english (" +
                        "b INTEGER PRIMARY KEY, n TEXT NOT NULL, t TEXT)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS app_settings (" +
                        "key TEXT PRIMARY KEY, value TEXT NOT NULL)",
                )
                seed(db)
            }

            override fun onOpen(db: SQLiteDatabase) {
                // onCreate only fires on a fresh file; running these here
                // backfills databases made before a table or seed was added.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS app_settings (" +
                        "key TEXT PRIMARY KEY, value TEXT NOT NULL)",
                )
                seed(db)
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                // Schema v1 is the first shipped version; nothing to migrate yet.
            }
        }
    }
}

// ── Cursor and transaction helpers ─────────────────────────────────────────

private inline fun SQLiteDatabase.transaction(block: () -> Unit) {
    beginTransaction()
    try {
        block()
        setTransactionSuccessful()
    } finally {
        endTransaction()
    }
}

private inline fun <T> Cursor.map(row: (Cursor) -> T): List<T> = use {
    val out = ArrayList<T>(count.coerceAtLeast(0))
    while (moveToNext()) out += row(this)
    out
}

private inline fun <T> Cursor.firstOrNull(row: (Cursor) -> T): T? = use {
    if (moveToFirst()) row(this) else null
}

private fun Cursor.firstInt(): Int = use { if (moveToFirst()) getInt(0) else 0 }

private fun Cursor.toBible() = Bible(
    table = getString(0),
    abbreviation = getString(1),
    version = getString(2),
    downloaded = getInt(3) == 1,
)

private fun Cursor.toBook() = Book(
    number = getInt(0),
    name = getString(1),
    testament = if (isNull(2)) null else getString(2),
)

private fun Cursor.toVerse() = Verse(
    book = getInt(0),
    chapter = getInt(1),
    number = getInt(2),
    text = getString(3),
)
