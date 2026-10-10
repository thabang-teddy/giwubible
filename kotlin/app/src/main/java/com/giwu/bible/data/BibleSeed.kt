package com.giwu.bible.data

import com.giwu.bible.model.Bible
import com.giwu.bible.model.Book

/**
 * Canonical bible metadata that ships with the app.
 *
 * Seeds `bible_version_key` and `key_english` on first launch and after a reset,
 * and doubles as the offline fallback so the setup screen and the book list
 * always have something to show, even with no network and an empty database.
 */
object BibleSeed {

    /** The seven public-domain translations the Giwu server serves. */
    val versions: List<Bible> = listOf(
        Bible(table = "t_asv", abbreviation = "ASV", version = "American Standard-ASV1901"),
        Bible(table = "t_bbe", abbreviation = "BBE", version = "Bible in Basic English"),
        Bible(table = "t_dby", abbreviation = "DARBY", version = "Darby English Bible"),
        Bible(table = "t_kjv", abbreviation = "KJV", version = "King James Version"),
        Bible(table = "t_wbt", abbreviation = "WBT", version = "Webster's Bible"),
        Bible(table = "t_web", abbreviation = "WEB", version = "World English Bible"),
        Bible(table = "t_ylt", abbreviation = "YLT", version = "Young's Literal Translation"),
    )

    /** All 66 books, in canonical order. */
    val books: List<Book> = listOf(
        // Old Testament
        Book(number = 1, name = "Genesis", testament = "OT"),
        Book(number = 2, name = "Exodus", testament = "OT"),
        Book(number = 3, name = "Leviticus", testament = "OT"),
        Book(number = 4, name = "Numbers", testament = "OT"),
        Book(number = 5, name = "Deuteronomy", testament = "OT"),
        Book(number = 6, name = "Joshua", testament = "OT"),
        Book(number = 7, name = "Judges", testament = "OT"),
        Book(number = 8, name = "Ruth", testament = "OT"),
        Book(number = 9, name = "1 Samuel", testament = "OT"),
        Book(number = 10, name = "2 Samuel", testament = "OT"),
        Book(number = 11, name = "1 Kings", testament = "OT"),
        Book(number = 12, name = "2 Kings", testament = "OT"),
        Book(number = 13, name = "1 Chronicles", testament = "OT"),
        Book(number = 14, name = "2 Chronicles", testament = "OT"),
        Book(number = 15, name = "Ezra", testament = "OT"),
        Book(number = 16, name = "Nehemiah", testament = "OT"),
        Book(number = 17, name = "Esther", testament = "OT"),
        Book(number = 18, name = "Job", testament = "OT"),
        Book(number = 19, name = "Psalms", testament = "OT"),
        Book(number = 20, name = "Proverbs", testament = "OT"),
        Book(number = 21, name = "Ecclesiastes", testament = "OT"),
        Book(number = 22, name = "Song of Solomon", testament = "OT"),
        Book(number = 23, name = "Isaiah", testament = "OT"),
        Book(number = 24, name = "Jeremiah", testament = "OT"),
        Book(number = 25, name = "Lamentations", testament = "OT"),
        Book(number = 26, name = "Ezekiel", testament = "OT"),
        Book(number = 27, name = "Daniel", testament = "OT"),
        Book(number = 28, name = "Hosea", testament = "OT"),
        Book(number = 29, name = "Joel", testament = "OT"),
        Book(number = 30, name = "Amos", testament = "OT"),
        Book(number = 31, name = "Obadiah", testament = "OT"),
        Book(number = 32, name = "Jonah", testament = "OT"),
        Book(number = 33, name = "Micah", testament = "OT"),
        Book(number = 34, name = "Nahum", testament = "OT"),
        Book(number = 35, name = "Habakkuk", testament = "OT"),
        Book(number = 36, name = "Zephaniah", testament = "OT"),
        Book(number = 37, name = "Haggai", testament = "OT"),
        Book(number = 38, name = "Zechariah", testament = "OT"),
        Book(number = 39, name = "Malachi", testament = "OT"),
        // New Testament
        Book(number = 40, name = "Matthew", testament = "NT"),
        Book(number = 41, name = "Mark", testament = "NT"),
        Book(number = 42, name = "Luke", testament = "NT"),
        Book(number = 43, name = "John", testament = "NT"),
        Book(number = 44, name = "Acts", testament = "NT"),
        Book(number = 45, name = "Romans", testament = "NT"),
        Book(number = 46, name = "1 Corinthians", testament = "NT"),
        Book(number = 47, name = "2 Corinthians", testament = "NT"),
        Book(number = 48, name = "Galatians", testament = "NT"),
        Book(number = 49, name = "Ephesians", testament = "NT"),
        Book(number = 50, name = "Philippians", testament = "NT"),
        Book(number = 51, name = "Colossians", testament = "NT"),
        Book(number = 52, name = "1 Thessalonians", testament = "NT"),
        Book(number = 53, name = "2 Thessalonians", testament = "NT"),
        Book(number = 54, name = "1 Timothy", testament = "NT"),
        Book(number = 55, name = "2 Timothy", testament = "NT"),
        Book(number = 56, name = "Titus", testament = "NT"),
        Book(number = 57, name = "Philemon", testament = "NT"),
        Book(number = 58, name = "Hebrews", testament = "NT"),
        Book(number = 59, name = "James", testament = "NT"),
        Book(number = 60, name = "1 Peter", testament = "NT"),
        Book(number = 61, name = "2 Peter", testament = "NT"),
        Book(number = 62, name = "1 John", testament = "NT"),
        Book(number = 63, name = "2 John", testament = "NT"),
        Book(number = 64, name = "3 John", testament = "NT"),
        Book(number = 65, name = "Jude", testament = "NT"),
        Book(number = 66, name = "Revelation", testament = "NT"),
    )

    /**
     * Chapters per book, so the chapter picker and the
     * continue-to-next-chapter check work without touching the database.
     */
    private val chapterCounts: Map<Int, Int> = mapOf(
        1 to 50, 2 to 40, 3 to 27, 4 to 36, 5 to 34, 6 to 24, 7 to 21, 8 to 4, 9 to 31, 10 to 24,
        11 to 22, 12 to 25, 13 to 29, 14 to 36, 15 to 10, 16 to 13, 17 to 10, 18 to 42, 19 to 150, 20 to 31,
        21 to 12, 22 to 8, 23 to 66, 24 to 52, 25 to 5, 26 to 48, 27 to 12, 28 to 14, 29 to 3, 30 to 9,
        31 to 1, 32 to 4, 33 to 7, 34 to 3, 35 to 3, 36 to 3, 37 to 2, 38 to 14, 39 to 4, 40 to 28,
        41 to 16, 42 to 24, 43 to 21, 44 to 28, 45 to 16, 46 to 16, 47 to 13, 48 to 6, 49 to 6, 50 to 4,
        51 to 4, 52 to 5, 53 to 3, 54 to 6, 55 to 4, 56 to 3, 57 to 1, 58 to 13, 59 to 5, 60 to 5,
        61 to 3, 62 to 5, 63 to 1, 64 to 1, 65 to 1, 66 to 22,
    )

    /** Falls back to 150 (the longest book) for an unknown book number. */
    fun maxChaptersFor(book: Int): Int = chapterCounts[book] ?: 150
}
