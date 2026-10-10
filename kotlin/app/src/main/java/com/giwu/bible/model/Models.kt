package com.giwu.bible.model

/** One translation, as listed in `bible_version_key`. */
data class Bible(
    val table: String,
    val abbreviation: String,
    val version: String,
    val downloaded: Boolean = false,
)

/**
 * One book of the bible.
 *
 * [testament] mirrors the `t` column of `key_english`, which is null in some
 * rows of the shipped data file.
 */
data class Book(
    val number: Int,
    val name: String,
    val testament: String? = null,
)

/** One verse row of a `t_*` table. */
data class Verse(
    val book: Int,
    val chapter: Int,
    val number: Int,
    val text: String,
)

/** The same verse in another translation, for the comparison panel. */
data class ComparisonResult(
    val bible: String,
    val version: String,
    val abbreviation: String,
    val text: String?,
)

data class Bookmark(
    val id: Int,
    val bible: String,
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val text: String,
) {
    fun matches(bible: String, book: Int, chapter: Int, verse: Int): Boolean =
        this.bible == bible && this.book == book &&
            this.chapter == chapter && this.verse == verse
}

data class User(
    val id: Int,
    val name: String,
    val email: String,
)

/**
 * Loading state for data that is fetched once per screen.
 *
 * Stands in for Riverpod's `AsyncValue`, so the Compose screens can branch on
 * loading / error / data the same way the Flutter widgets did.
 */
sealed interface Async<out T> {
    data object Loading : Async<Nothing>
    data class Data<T>(val value: T) : Async<T>
    data class Error(val cause: Throwable) : Async<Nothing>

    val valueOrNull: T?
        get() = when (this) {
            is Data -> value
            else -> null
        }

    val isLoading: Boolean get() = this is Loading

    val errorOrNull: Throwable?
        get() = when (this) {
            is Error -> cause
            else -> null
        }
}

/** Runs [block], wrapping success and failure into [Async] like `AsyncValue.guard`. */
inline fun <T> asyncCatching(block: () -> T): Async<T> =
    try {
        Async.Data(block())
    } catch (e: Exception) {
        Async.Error(e)
    }
