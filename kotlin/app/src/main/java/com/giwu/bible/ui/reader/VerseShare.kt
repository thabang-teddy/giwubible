package com.giwu.bible.ui.reader

/** Where a shared verse sends the reader. */
const val SITE_URL = "https://giwu.co.za"

/**
 * The bottom bar's line: VERSION - BOOK - CHAPTER - VERSE.
 *
 * Parts the reader has not got yet are left out rather than printed empty, so
 * the bar reads correctly before a verse is tapped and during first load.
 */
fun verseReference(
    versionName: String?,
    bookName: String?,
    chapter: Int,
    verse: Int?,
): String = listOfNotNull(
    versionName?.takeIf { it.isNotBlank() },
    bookName?.takeIf { it.isNotBlank() },
    chapter.takeIf { it > 0 }?.toString(),
    verse?.takeIf { it > 0 }?.toString(),
).joinToString(" - ") { it.uppercase() }

/**
 * The live-site link for a verse: `https://giwu.co.za/read/{book}/{chapter}?v={verse}`.
 *
 * The book travels as a readable slug ("song-of-solomon"); the server accepts
 * a book number too, which is what an unnamed book falls back to.
 */
fun verseShareUrl(
    bookName: String?,
    bookNumber: Int,
    chapter: Int,
    verse: Int?,
): String {
    val slug = bookSlug(bookName).ifEmpty { bookNumber.toString() }
    val query = verse?.takeIf { it > 0 }?.let { "?v=$it" }.orEmpty()

    return "$SITE_URL/read/$slug/$chapter$query"
}

/** "1 John" -> "1-john", "Song of Solomon" -> "song-of-solomon". */
private fun bookSlug(bookName: String?): String =
    bookName.orEmpty()
        .lowercase()
        .map { if (it.isLetterOrDigit()) it else '-' }
        .joinToString("")
        .split('-')
        .filter { it.isNotEmpty() }
        .joinToString("-")
