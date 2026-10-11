package com.giwu.bible.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BibleSeedTest {

    @Test
    fun `all 66 books are seeded in canonical order`() {
        assertEquals(66, BibleSeed.books.size)
        assertEquals((1..66).toList(), BibleSeed.books.map { it.number })
        assertEquals("Genesis", BibleSeed.books.first().name)
        assertEquals("Revelation", BibleSeed.books.last().name)
    }

    @Test
    fun `the testament split lands on Matthew`() {
        val oldTestament = BibleSeed.books.filter { it.testament == "OT" }
        val newTestament = BibleSeed.books.filter { it.testament == "NT" }

        assertEquals(39, oldTestament.size)
        assertEquals(27, newTestament.size)
        assertEquals("Matthew", newTestament.first().name)
    }

    @Test
    fun `every seeded translation has a valid table name`() {
        val pattern = Regex("^t_[a-z0-9]+$")
        assertTrue(BibleSeed.versions.all { pattern.matches(it.table) })
        assertTrue(BibleSeed.versions.none { it.downloaded })
    }

    @Test
    fun `KJV is among the seeded translations`() {
        val kjv = BibleSeed.versions.firstOrNull { it.table == BibleDatabase.FALLBACK_BIBLE }
        assertEquals("KJV", kjv?.abbreviation)
    }

    @Test
    fun `chapter counts match the known long and short books`() {
        assertEquals(50, BibleSeed.maxChaptersFor(1)) // Genesis
        assertEquals(150, BibleSeed.maxChaptersFor(19)) // Psalms
        assertEquals(1, BibleSeed.maxChaptersFor(57)) // Philemon
        assertEquals(22, BibleSeed.maxChaptersFor(66)) // Revelation
    }

    @Test
    fun `an unknown book number falls back to the longest book`() {
        assertEquals(150, BibleSeed.maxChaptersFor(0))
        assertEquals(150, BibleSeed.maxChaptersFor(67))
    }

    @Test
    fun `every book has a chapter count of its own`() {
        // A missing entry would silently fall back to 150 and offer chapters
        // that do not exist.
        val fallbacks = BibleSeed.books.filter { BibleSeed.maxChaptersFor(it.number) == 150 }
        assertEquals(listOf("Psalms"), fallbacks.map { it.name })
    }
}
