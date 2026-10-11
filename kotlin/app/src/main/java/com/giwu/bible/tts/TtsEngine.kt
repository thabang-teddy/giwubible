package com.giwu.bible.tts

/** Whether the reader can speak right now. */
enum class TtsReadiness {
    /** A usable voice is installed; reading works with no network at all. */
    READY,

    /**
     * The engine is there but has no voice data for the language yet, which
     * the system can install on request.
     */
    NEEDS_VOICE_DATA,

    /** No usable speech engine on this device. */
    UNSUPPORTED,
}

/**
 * The slice of speech synthesis this app needs.
 *
 * Kept as an interface so the playback state machine can be unit-tested
 * without a real engine, and so a different synthesizer could be dropped in
 * without touching [TtsController].
 */
interface TtsEngine {

    /** Starts the engine and reports whether it can speak. Caches its result. */
    suspend fun prepare(): TtsReadiness

    /** Drops the cached probe so newly installed voice data is picked up. */
    suspend fun reload()

    /** [multiplier] is user-facing: 1.0 is the voice's natural pace. */
    suspend fun setRate(multiplier: Float)

    /** Returns once the utterance has finished playing, or was stopped. */
    suspend fun speak(text: String)

    suspend fun stop()

    fun shutdown()
}

private val BRACKETS = Regex("[\\[\\]{}]")
private val WHITESPACE = Regex("\\s+")

/**
 * Strips editorial markup so the engine reads a clean sentence.
 *
 * Public-domain bible texts wrap translator-supplied words in brackets (KJV
 * uses `[...]`, some exports `{...}`). Those words are part of the sentence,
 * so only the delimiters go — dropping the contents would leave
 * ungrammatical speech.
 */
fun prepareVerseText(raw: String): String =
    raw.replace(BRACKETS, "").replace(WHITESPACE, " ").trim()
