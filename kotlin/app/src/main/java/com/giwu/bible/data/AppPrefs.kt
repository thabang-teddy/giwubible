package com.giwu.bible.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reader preferences, persisted the moment they change.
 *
 * Each setting is exposed as a [StateFlow] so Compose recomposes on write and
 * the reader comes back exactly where it left off. Keys match the Flutter
 * build's so the two apps stay easy to compare.
 */
class AppPrefs(private val prefs: SharedPreferences) {

    // ── Appearance ─────────────────────────────────────────────────────────

    private val _darkMode = MutableStateFlow(prefs.getBoolean(KEY_DARK, false))
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    fun toggleDarkMode() = setDarkMode(!_darkMode.value)

    fun setDarkMode(value: Boolean) {
        _darkMode.value = value
        prefs.edit { putBoolean(KEY_DARK, value) }
    }

    // ── Reading position ───────────────────────────────────────────────────

    private val _primaryBible =
        MutableStateFlow(prefs.getString(KEY_BIBLE, null) ?: BibleDatabase.FALLBACK_BIBLE)
    val primaryBible: StateFlow<String> = _primaryBible.asStateFlow()

    fun setPrimaryBible(table: String) {
        _primaryBible.value = table
        prefs.edit { putString(KEY_BIBLE, table) }
    }

    private val _book = MutableStateFlow(prefs.getInt(KEY_BOOK, 1))
    val book: StateFlow<Int> = _book.asStateFlow()

    fun setBook(book: Int) {
        _book.value = book
        prefs.edit { putInt(KEY_BOOK, book) }
    }

    private val _chapter = MutableStateFlow(prefs.getInt(KEY_CHAPTER, 1))
    val chapter: StateFlow<Int> = _chapter.asStateFlow()

    fun setChapter(chapter: Int) {
        _chapter.value = chapter
        prefs.edit { putInt(KEY_CHAPTER, chapter) }
    }

    /**
     * The verse the reader tapped, which drives the comparison panel.
     * Deliberately not persisted — a new session starts with nothing selected.
     */
    private val _activeVerse = MutableStateFlow<Int?>(null)
    val activeVerse: StateFlow<Int?> = _activeVerse.asStateFlow()

    fun setActiveVerse(verse: Int?) {
        _activeVerse.value = verse
    }

    // ── Read aloud ─────────────────────────────────────────────────────────

    private val _speechRate = MutableStateFlow(prefs.getFloat(KEY_RATE, 1f))
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    fun setSpeechRate(multiplier: Float) {
        _speechRate.value = multiplier
        prefs.edit { putFloat(KEY_RATE, multiplier) }
    }

    /** Steps to the next preset, wrapping back to the slowest. */
    fun cycleSpeechRate() {
        val index = SPEECH_RATE_PRESETS.indexOf(_speechRate.value)
        setSpeechRate(SPEECH_RATE_PRESETS[(index + 1) % SPEECH_RATE_PRESETS.size])
    }

    private val _announceVerseNumbers =
        MutableStateFlow(prefs.getBoolean(KEY_ANNOUNCE, false))
    val announceVerseNumbers: StateFlow<Boolean> = _announceVerseNumbers.asStateFlow()

    fun setAnnounceVerseNumbers(value: Boolean) {
        _announceVerseNumbers.value = value
        prefs.edit { putBoolean(KEY_ANNOUNCE, value) }
    }

    private val _continueToNextChapter =
        MutableStateFlow(prefs.getBoolean(KEY_CONTINUE, false))
    val continueToNextChapter: StateFlow<Boolean> = _continueToNextChapter.asStateFlow()

    fun setContinueToNextChapter(value: Boolean) {
        _continueToNextChapter.value = value
        prefs.edit { putBoolean(KEY_CONTINUE, value) }
    }

    // ── Parallel translations ──────────────────────────────────────────────

    /**
     * Translations ticked in the comparison panel.
     *
     * Empty means "all of them", which keeps the panel useful when a new
     * translation is downloaded without the reader having to tick it.
     */
    private val _parallelBibles = MutableStateFlow(
        prefs.getStringSet(KEY_PARALLEL, emptySet())?.toList() ?: emptyList(),
    )
    val parallelBibles: StateFlow<List<String>> = _parallelBibles.asStateFlow()

    fun toggleParallelBible(table: String, allComparable: List<String>) {
        val next = nextParallelSelection(_parallelBibles.value, table, allComparable)
        _parallelBibles.value = next
        prefs.edit { putStringSet(KEY_PARALLEL, next.toSet()) }
    }

    companion object {
        /** User-facing speed multipliers; 1.0 is the voice's natural pace. */
        val SPEECH_RATE_PRESETS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

        /**
         * Works out the selection after [table] is ticked or unticked.
         *
         * An empty list is the "all of them" shorthand, which is why a
         * selection that happens to cover everything collapses back to empty
         * — otherwise a translation downloaded later would arrive unticked.
         */
        fun nextParallelSelection(
            current: List<String>,
            table: String,
            allComparable: List<String>,
        ): List<String> = when {
            current.isEmpty() -> allComparable.filter { it != table }

            current.contains(table) -> current.filter { it != table }

            else -> (current + table)
                .let { if (it.size == allComparable.size) emptyList() else it }
        }

        private const val KEY_DARK = "giwu_dark"
        private const val KEY_BIBLE = "giwu_bible"
        private const val KEY_BOOK = "giwu_book"
        private const val KEY_CHAPTER = "giwu_chapter"
        private const val KEY_RATE = "giwu_tts_rate"
        private const val KEY_ANNOUNCE = "giwu_tts_numbers"
        private const val KEY_CONTINUE = "giwu_tts_continue"
        private const val KEY_PARALLEL = "giwu_parallel_bibles"

        fun create(context: Context): AppPrefs = AppPrefs(
            context.applicationContext
                .getSharedPreferences("giwu_prefs", Context.MODE_PRIVATE),
        )
    }
}
