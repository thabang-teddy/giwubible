package com.giwu.bible.tts

import android.util.Log
import com.giwu.bible.model.Verse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TtsStatus { IDLE, SPEAKING, PAUSED }

data class TtsPlayback(
    val status: TtsStatus = TtsStatus.IDLE,

    /** The verse being spoken, or the one playback is paused on. */
    val verse: Int? = null,

    /**
     * Set for a single transition when the last verse of a chapter finished,
     * so a listener can decide whether to roll into the next chapter.
     */
    val reachedEnd: Boolean = false,
) {
    val isActive: Boolean get() = status != TtsStatus.IDLE
}

/**
 * Speaks a chapter one verse at a time.
 *
 * Each utterance is awaited on its own so the reader can highlight and scroll
 * to the verse being read, and so playback can be paused or skipped at verse
 * granularity. Pausing stops the engine and remembers the index — resuming
 * re-reads the current verse from its start, which every engine supports.
 *
 * Every transition runs inside a job that first cancels and joins the previous
 * one, so a stop, pause or skip can never be overtaken by the loop it
 * interrupted.
 */
class TtsController(
    private val engine: TtsEngine,
    private val scope: CoroutineScope,
    rate: Float = 1f,
    announceNumbers: Boolean = false,
) {

    private val _playback = MutableStateFlow(TtsPlayback())
    val playback: StateFlow<TtsPlayback> = _playback.asStateFlow()

    private var verses: List<Verse> = emptyList()
    private var index = 0
    private var job: Job? = null

    private var rate: Float = rate
    var announceNumbers: Boolean = announceNumbers

    /** Starts reading [verses], optionally from a specific verse number. */
    fun start(verses: List<Verse>, fromVerse: Int? = null) {
        if (verses.isEmpty()) return
        this.verses = verses
        val found = fromVerse?.let { number -> verses.indexOfFirst { it.number == number } } ?: -1
        index = if (found < 0) 0 else found
        playFromIndex()
    }

    fun pause() {
        if (_playback.value.status != TtsStatus.SPEAKING) return
        replaceJob {
            engine.stop()
            _playback.value = _playback.value.copy(status = TtsStatus.PAUSED)
        }
    }

    fun resume() {
        if (_playback.value.status != TtsStatus.PAUSED) return
        playFromIndex()
    }

    fun stop() {
        replaceJob {
            engine.stop()
            _playback.value = TtsPlayback()
        }
    }

    /** Skips forward; stops when already on the last verse. */
    fun next() {
        if (!_playback.value.isActive) return
        if (index >= verses.size - 1) {
            stop()
            return
        }
        index++
        playFromIndex()
    }

    /** Skips back; restarts the first verse when already at the start. */
    fun previous() {
        if (!_playback.value.isActive) return
        if (index > 0) index--
        playFromIndex()
    }

    fun setRate(multiplier: Float) {
        rate = multiplier
        // Re-issue the current verse so the new rate takes effect right away.
        if (_playback.value.status == TtsStatus.SPEAKING) playFromIndex()
    }

    /** Acknowledges the end-of-chapter signal so it fires only once. */
    fun clearReachedEnd() {
        val current = _playback.value
        if (!current.reachedEnd) return
        _playback.value = current.copy(reachedEnd = false)
    }

    fun dispose() {
        stop()
        engine.shutdown()
    }

    // ── Plumbing ───────────────────────────────────────────────────────────

    private fun playFromIndex() = replaceJob {
        engine.stop()
        engine.setRate(rate)
        speakLoop()
    }

    private suspend fun speakLoop() {
        while (index < verses.size) {
            val verse = verses[index]
            _playback.value = TtsPlayback(status = TtsStatus.SPEAKING, verse = verse.number)

            val text = utteranceFor(verse)
            if (text.isNotEmpty()) {
                try {
                    engine.speak(text)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Reading verse ${verse.number} failed", e)
                    _playback.value = TtsPlayback()
                    return
                }
            }
            index++
        }
        _playback.value = TtsPlayback(reachedEnd = true)
    }

    private fun utteranceFor(verse: Verse): String {
        val text = prepareVerseText(verse.text)
        if (text.isEmpty()) return ""
        return if (announceNumbers) "Verse ${verse.number}. $text" else text
    }

    /**
     * Replaces the running job with one that waits for it to finish
     * unwinding first — that ordering is what makes stop, pause and skip
     * race-free against the loop they interrupt.
     */
    private fun replaceJob(block: suspend () -> Unit) {
        val previous = job
        job = scope.launch {
            previous?.cancelAndJoin()
            block()
        }
    }

    private companion object {
        const val TAG = "TtsController"
    }
}
