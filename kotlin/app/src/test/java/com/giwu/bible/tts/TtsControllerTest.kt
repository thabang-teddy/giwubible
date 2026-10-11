package com.giwu.bible.tts

import com.giwu.bible.model.Verse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TtsControllerTest {

    private fun verses(count: Int): List<Verse> = (1..count).map {
        Verse(book = 1, chapter = 1, number = it, text = "Verse $it text.")
    }

    @Test
    fun `start speaks the first verse and reports it`() = runTest {
        val engine = FakeTtsEngine()
        val controller = TtsController(engine, this)

        controller.start(verses(3))
        advanceUntilIdle()

        assertEquals(TtsStatus.SPEAKING, controller.playback.value.status)
        assertEquals(1, controller.playback.value.verse)
        assertEquals(listOf("Verse 1 text."), engine.spoken)

        controller.stop()
        advanceUntilIdle()
    }

    @Test
    fun `playback steps through the chapter as utterances finish`() = runTest {
        val engine = FakeTtsEngine()
        val controller = TtsController(engine, this)

        controller.start(verses(3))
        advanceUntilIdle()
        engine.finishUtterance()
        advanceUntilIdle()

        assertEquals(2, controller.playback.value.verse)

        engine.finishUtterance()
        advanceUntilIdle()
        assertEquals(3, controller.playback.value.verse)

        engine.finishUtterance()
        advanceUntilIdle()

        // The chapter is done: idle, with the one-shot end signal set.
        assertEquals(TtsStatus.IDLE, controller.playback.value.status)
        assertTrue(controller.playback.value.reachedEnd)
        assertEquals(3, engine.spoken.size)
    }

    @Test
    fun `start can begin at a given verse`() = runTest {
        val engine = FakeTtsEngine()
        val controller = TtsController(engine, this)

        controller.start(verses(5), fromVerse = 4)
        advanceUntilIdle()

        assertEquals(4, controller.playback.value.verse)
        assertEquals(listOf("Verse 4 text."), engine.spoken)

        controller.stop()
        advanceUntilIdle()
    }

    @Test
    fun `an unknown starting verse falls back to the first one`() = runTest {
        val engine = FakeTtsEngine()
        val controller = TtsController(engine, this)

        controller.start(verses(3), fromVerse = 99)
        advanceUntilIdle()

        assertEquals(1, controller.playback.value.verse)

        controller.stop()
        advanceUntilIdle()
    }

    @Test
    fun `pause holds the verse and resume re-reads it`() = runTest {
        val engine = FakeTtsEngine()
        val controller = TtsController(engine, this)

        controller.start(verses(3))
        advanceUntilIdle()
        controller.pause()
        advanceUntilIdle()

        assertEquals(TtsStatus.PAUSED, controller.playback.value.status)
        assertEquals(1, controller.playback.value.verse)

        controller.resume()
        advanceUntilIdle()

        assertEquals(TtsStatus.SPEAKING, controller.playback.value.status)
        assertEquals(1, controller.playback.value.verse)
        assertEquals(listOf("Verse 1 text.", "Verse 1 text."), engine.spoken)

        controller.stop()
        advanceUntilIdle()
    }

    @Test
    fun `stop clears the state and silences the engine`() = runTest {
        val engine = FakeTtsEngine()
        val controller = TtsController(engine, this)

        controller.start(verses(3))
        advanceUntilIdle()
        controller.stop()
        advanceUntilIdle()

        assertEquals(TtsStatus.IDLE, controller.playback.value.status)
        assertNull(controller.playback.value.verse)
        assertFalse(engine.hasPendingUtterance)
    }

    @Test
    fun `the loop a stop interrupted does not advance afterwards`() = runTest {
        val engine = FakeTtsEngine()
        val controller = TtsController(engine, this)

        controller.start(verses(3))
        advanceUntilIdle()
        controller.stop()
        advanceUntilIdle()

        // Only the first verse was ever issued, and nothing followed it.
        assertEquals(listOf("Verse 1 text."), engine.spoken)
        assertEquals(TtsStatus.IDLE, controller.playback.value.status)
    }

    @Test
    fun `next skips forward`() = runTest {
        val engine = FakeTtsEngine()
        val controller = TtsController(engine, this)

        controller.start(verses(3))
        advanceUntilIdle()
        controller.next()
        advanceUntilIdle()

        assertEquals(2, controller.playback.value.verse)

        controller.stop()
        advanceUntilIdle()
    }

    @Test
    fun `next on the last verse stops playback`() = runTest {
        val engine = FakeTtsEngine()
        val controller = TtsController(engine, this)

        controller.start(verses(2), fromVerse = 2)
        advanceUntilIdle()
        controller.next()
        advanceUntilIdle()

        assertEquals(TtsStatus.IDLE, controller.playback.value.status)
        assertFalse(controller.playback.value.reachedEnd)
    }

    @Test
    fun `previous steps back and stays put at the start`() = runTest {
        val engine = FakeTtsEngine()
        val controller = TtsController(engine, this)

        controller.start(verses(3), fromVerse = 2)
        advanceUntilIdle()
        controller.previous()
        advanceUntilIdle()
        assertEquals(1, controller.playback.value.verse)

        controller.previous()
        advanceUntilIdle()
        assertEquals(1, controller.playback.value.verse)

        controller.stop()
        advanceUntilIdle()
    }

    @Test
    fun `transport controls do nothing while idle`() = runTest {
        val engine = FakeTtsEngine()
        val controller = TtsController(engine, this)

        controller.next()
        controller.previous()
        controller.resume()
        advanceUntilIdle()

        assertEquals(TtsStatus.IDLE, controller.playback.value.status)
        assertTrue(engine.spoken.isEmpty())
    }

    @Test
    fun `announcing verse numbers prefixes the utterance`() = runTest {
        val engine = FakeTtsEngine()
        val controller = TtsController(engine, this, announceNumbers = true)

        controller.start(verses(1))
        advanceUntilIdle()

        assertEquals(listOf("Verse 1. Verse 1 text."), engine.spoken)

        controller.stop()
        advanceUntilIdle()
    }

    @Test
    fun `the rate reaches the engine and re-issues the current verse`() = runTest {
        val engine = FakeTtsEngine()
        val controller = TtsController(engine, this, rate = 1f)

        controller.start(verses(2))
        advanceUntilIdle()
        controller.setRate(1.5f)
        advanceUntilIdle()

        assertEquals(listOf(1f, 1.5f), engine.rates)
        assertEquals(1, controller.playback.value.verse)
        assertEquals(listOf("Verse 1 text.", "Verse 1 text."), engine.spoken)

        controller.stop()
        advanceUntilIdle()
    }

    @Test
    fun `a failing utterance stops playback instead of hanging`() = runTest {
        val engine = FakeTtsEngine()
        engine.failNextSpeak = IllegalStateException("engine died")
        val controller = TtsController(engine, this)

        controller.start(verses(3))
        advanceUntilIdle()

        assertEquals(TtsStatus.IDLE, controller.playback.value.status)
    }

    @Test
    fun `an empty chapter is ignored`() = runTest {
        val engine = FakeTtsEngine()
        val controller = TtsController(engine, this)

        controller.start(emptyList())
        advanceUntilIdle()

        assertEquals(TtsStatus.IDLE, controller.playback.value.status)
        assertTrue(engine.spoken.isEmpty())
    }

    @Test
    fun `clearReachedEnd fires the end signal only once`() = runTest {
        val engine = FakeTtsEngine()
        val controller = TtsController(engine, this)

        controller.start(verses(1))
        advanceUntilIdle()
        engine.finishUtterance()
        advanceUntilIdle()

        assertTrue(controller.playback.value.reachedEnd)
        controller.clearReachedEnd()
        assertFalse(controller.playback.value.reachedEnd)
    }

    @Test
    fun `a verse with no speakable text is skipped`() = runTest {
        val engine = FakeTtsEngine()
        val controller = TtsController(engine, this)

        controller.start(
            listOf(
                Verse(book = 1, chapter = 1, number = 1, text = "   "),
                Verse(book = 1, chapter = 1, number = 2, text = "Real text."),
            ),
        )
        advanceUntilIdle()

        assertEquals(2, controller.playback.value.verse)
        assertEquals(listOf("Real text."), engine.spoken)

        controller.stop()
        advanceUntilIdle()
    }

    @Test
    fun `dispose stops playback and shuts the engine down`() = runTest {
        val engine = FakeTtsEngine()
        val controller = TtsController(engine, this)

        controller.start(verses(2))
        advanceUntilIdle()
        controller.dispose()
        advanceUntilIdle()

        assertTrue(engine.isShutdown)
    }
}
