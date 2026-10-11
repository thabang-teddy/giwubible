package com.giwu.bible.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * [TtsEngine] backed by the device's own speech engine.
 *
 * Everything runs on-device: after the system has voice data for the language,
 * reading a chapter never touches the network. One utterance is in flight at a
 * time, which is what lets the reader highlight and scroll to the verse being
 * spoken and resume at verse granularity.
 */
class AndroidTtsEngine(context: Context) : TtsEngine {

    private val appContext = context.applicationContext

    private val lock = Mutex()
    private var tts: TextToSpeech? = null
    private var readiness: TtsReadiness? = null

    /** Completed by the progress listener when the current clip ends. */
    @Volatile
    private var utterance: CompletableDeferred<Unit>? = null

    private var utteranceCount = 0

    override suspend fun prepare(): TtsReadiness = lock.withLock {
        readiness?.let { return@withLock it }
        val result = load()
        readiness = result
        result
    }

    override suspend fun reload() = lock.withLock {
        shutdownLocked()
        readiness = null
    }

    private suspend fun load(): TtsReadiness {
        val engine = createEngine() ?: return TtsReadiness.UNSUPPORTED

        // The bible texts the app ships are English, so US English is tried
        // first and the device's own locale second (en_ZA, en_GB and friends
        // read these texts just as well).
        val candidates = listOf(Locale.US, Locale.getDefault())
        val best = candidates.maxByOrNull { engine.isLanguageAvailable(it).languageRank() }
        val status = best
            ?.let { engine.isLanguageAvailable(it) }
            ?: TextToSpeech.LANG_NOT_SUPPORTED

        return when {
            status >= TextToSpeech.LANG_AVAILABLE && best != null -> {
                engine.language = best
                engine.setOnUtteranceProgressListener(listener)
                tts = engine
                TtsReadiness.READY
            }

            status == TextToSpeech.LANG_MISSING_DATA -> {
                engine.shutdown()
                TtsReadiness.NEEDS_VOICE_DATA
            }

            else -> {
                engine.shutdown()
                TtsReadiness.UNSUPPORTED
            }
        }
    }

    /** Waits for the engine's own init callback before using it. */
    private suspend fun createEngine(): TextToSpeech? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            var engine: TextToSpeech? = null
            engine = TextToSpeech(appContext) { status ->
                if (!continuation.isActive) {
                    engine?.shutdown()
                    return@TextToSpeech
                }
                if (status == TextToSpeech.SUCCESS) {
                    continuation.resume(engine)
                } else {
                    Log.w(TAG, "Speech engine failed to initialise: status $status")
                    engine?.shutdown()
                    continuation.resume(null)
                }
            }
            continuation.invokeOnCancellation { engine?.shutdown() }
        }
    }

    override suspend fun setRate(multiplier: Float) {
        // The platform rate is a direct multiplier, so it maps one to one.
        tts?.setSpeechRate(multiplier)
    }

    override suspend fun speak(text: String) {
        val engine = checkNotNull(tts) { "Speech engine is not prepared" }

        val pending = CompletableDeferred<Unit>()
        utterance = pending
        val id = "giwu-${utteranceCount++}"

        val queued = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        if (queued != TextToSpeech.SUCCESS) {
            utterance = null
            error("Speech engine refused the utterance (code $queued)")
        }

        try {
            pending.await()
        } finally {
            // A cancelled read must not leave audio playing behind it.
            if (!pending.isCompleted) engine.stop()
            if (utterance === pending) utterance = null
        }
    }

    override suspend fun stop() {
        tts?.stop()
        utterance?.complete(Unit)
        utterance = null
    }

    override fun shutdown() {
        utterance?.complete(Unit)
        utterance = null
        tts?.shutdown()
        tts = null
        readiness = null
    }

    private fun shutdownLocked() {
        utterance?.complete(Unit)
        utterance = null
        tts?.shutdown()
        tts = null
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) {
            utterance?.complete(Unit)
        }

        // Superseded by onError(String, Int), but still abstract, so it has
        // to be implemented for older engines that call it.
        @Suppress("OVERRIDE_DEPRECATION")
        override fun onError(utteranceId: String?) {
            Log.w(TAG, "Utterance $utteranceId failed")
            utterance?.complete(Unit)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            Log.w(TAG, "Utterance $utteranceId failed with code $errorCode")
            utterance?.complete(Unit)
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            utterance?.complete(Unit)
        }
    }

    /**
     * Orders the language results so the best match across the tried locales
     * wins: an exact match beats a country or variant fallback, and either
     * beats missing data.
     */
    private fun Int.languageRank(): Int = when (this) {
        TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> 4
        TextToSpeech.LANG_COUNTRY_AVAILABLE -> 3
        TextToSpeech.LANG_AVAILABLE -> 2
        TextToSpeech.LANG_MISSING_DATA -> 1
        else -> 0
    }

    private companion object {
        const val TAG = "AndroidTtsEngine"
    }
}
