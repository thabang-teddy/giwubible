package com.giwu.bible.tts

import org.junit.Assert.assertEquals
import org.junit.Test

class VerseTextTest {

    @Test
    fun `plain text is unchanged`() {
        assertEquals(
            "In the beginning God created the heaven and the earth.",
            prepareVerseText("In the beginning God created the heaven and the earth."),
        )
    }

    @Test
    fun `square brackets go but the words they wrap stay`() {
        assertEquals(
            "And God said, Let there be light: and there was light.",
            prepareVerseText("And God said, Let [there] be light: and there was light."),
        )
    }

    @Test
    fun `curly brackets are stripped too`() {
        assertEquals("the sons of God", prepareVerseText("the {sons} of God"))
    }

    @Test
    fun `runs of whitespace collapse to one space`() {
        assertEquals("one two three", prepareVerseText("one   two\n\tthree"))
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        assertEquals("Amen.", prepareVerseText("  Amen.  "))
    }

    @Test
    fun `a verse of only markup becomes empty`() {
        assertEquals("", prepareVerseText(" [ ] { } "))
    }

    @Test
    fun `an empty verse stays empty`() {
        assertEquals("", prepareVerseText(""))
    }
}
