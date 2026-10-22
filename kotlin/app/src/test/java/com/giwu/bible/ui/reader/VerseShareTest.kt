package com.giwu.bible.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The bottom bar's line and the link it shares. The link has to match the
 * server's `/read/{book}/{chapter}` route, so the slugging is asserted here
 * against the book names that break naive slugs.
 */
class VerseShareTest {

    @Test
    fun `reference reads version then book then chapter then verse`() {
        assertEquals(
            "KING JAMES VERSION - GENESIS - 1 - 3",
            verseReference("King James Version", "Genesis", 1, 3),
        )
    }

    @Test
    fun `reference drops the verse until one is selected`() {
        assertEquals(
            "KING JAMES VERSION - GENESIS - 1",
            verseReference("King James Version", "Genesis", 1, null),
        )
    }

    @Test
    fun `reference leaves out parts the reader has not loaded yet`() {
        assertEquals("GENESIS - 1", verseReference(null, "Genesis", 1, null))
        assertEquals("", verseReference(null, null, 0, null))
    }

    @Test
    fun `share url carries a slugged book and the verse as a query`() {
        assertEquals(
            "https://giwu.co.za/read/john/3?v=16",
            verseShareUrl("John", 43, 3, 16),
        )
    }

    @Test
    fun `share url slugs multi word and numbered books`() {
        assertEquals(
            "https://giwu.co.za/read/song-of-solomon/2?v=1",
            verseShareUrl("Song of Solomon", 22, 2, 1),
        )
        assertEquals(
            "https://giwu.co.za/read/1-john/4?v=8",
            verseShareUrl("1 John", 62, 4, 8),
        )
    }

    @Test
    fun `share url drops the query when no verse is selected`() {
        assertEquals("https://giwu.co.za/read/john/3", verseShareUrl("John", 43, 3, null))
    }

    @Test
    fun `share url falls back to the book number when the name is missing`() {
        assertEquals("https://giwu.co.za/read/43/3?v=16", verseShareUrl(null, 43, 3, 16))
    }
}
