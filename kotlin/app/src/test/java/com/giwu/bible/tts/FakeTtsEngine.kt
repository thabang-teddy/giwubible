package com.giwu.bible.tts

import kotlinx.coroutines.CompletableDeferred

/**
 * A [TtsEngine] that never makes a sound.
 *
 * Each utterance stays pending until the test completes it, which is what
 * lets a test sit in the middle of a verse and check what pause, stop or skip
 * does from there.
 */
class FakeTtsEngine(
    private var readiness: TtsReadiness = TtsReadiness.READY,
) : TtsEngine {

    val spoken = mutableListOf<String>()
    val rates = mutableListOf<Float>()
    var stopCount = 0
        private set
    var reloadCount = 0
        private set
    var isShutdown = false
        private set

    /** Set to have the next [speak] fail instead of waiting. */
    var failNextSpeak: Exception? = null

    private var pending: CompletableDeferred<Unit>? = null

    val hasPendingUtterance: Boolean get() = pending?.isCompleted == false

    /** Completes the utterance in flight, as a real engine's onDone would. */
    fun finishUtterance() {
        pending?.complete(Unit)
        pending = null
    }

    override suspend fun prepare(): TtsReadiness = readiness

    override suspend fun reload() {
        reloadCount++
    }

    fun setReadiness(value: TtsReadiness) {
        readiness = value
    }

    override suspend fun setRate(multiplier: Float) {
        rates += multiplier
    }

    override suspend fun speak(text: String) {
        failNextSpeak?.let {
            failNextSpeak = null
            throw it
        }
        spoken += text
        val deferred = CompletableDeferred<Unit>()
        pending = deferred
        try {
            deferred.await()
        } finally {
            if (pending === deferred) pending = null
        }
    }

    override suspend fun stop() {
        stopCount++
        pending?.complete(Unit)
        pending = null
    }

    override fun shutdown() {
        isShutdown = true
    }
}
