package com.giwu.bible.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.giwu.bible.AppContainer
import com.giwu.bible.AppGraph
import com.giwu.bible.data.AppPrefs
import com.giwu.bible.data.BibleSeed
import com.giwu.bible.model.Async
import com.giwu.bible.model.Bible
import com.giwu.bible.model.Book
import com.giwu.bible.model.ComparisonResult
import com.giwu.bible.model.Verse
import com.giwu.bible.repo.BibleRepository
import com.giwu.bible.repo.BookmarkStore
import com.giwu.bible.tts.TtsController
import com.giwu.bible.tts.TtsEngine
import com.giwu.bible.tts.TtsReadiness
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class ChapterKey(
    val bible: String,
    val book: Int,
    val chapter: Int,
    val refresh: Int = 0,
)

/**
 * The reading screen's state.
 *
 * Owns three independent streams — the translation list, the book list and the
 * open chapter — because each has its own fallback chain and failure mode, and
 * the reader stays usable when any one of them is empty.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModel(
    private val repository: BibleRepository,
    private val prefs: AppPrefs,
    private val bookmarks: BookmarkStore,
    private val tts: TtsController,
    private val ttsEngine: TtsEngine,
) : ViewModel() {

    private val _bibles = MutableStateFlow<List<Bible>>(BibleSeed.versions)
    val bibles: StateFlow<List<Bible>> = _bibles.asStateFlow()

    private val _books = MutableStateFlow<Async<List<Book>>>(Async.Loading)
    val books: StateFlow<Async<List<Book>>> = _books.asStateFlow()

    private val _readiness = MutableStateFlow<TtsReadiness?>(null)
    val readiness: StateFlow<TtsReadiness?> = _readiness.asStateFlow()

    private val refresh = MutableStateFlow(0)

    /**
     * True while the reader is deliberately rolling into the next chapter, so
     * the "selection changed" guard does not stop the playback it just caused.
     */
    private var autoAdvancing = false

    private val chapterKey: StateFlow<ChapterKey> = combine(
        prefs.primaryBible,
        prefs.book,
        prefs.chapter,
        refresh,
    ) { bible, book, chapter, refresh ->
        ChapterKey(bible, book, chapter, refresh)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ChapterKey(
            bible = prefs.primaryBible.value,
            book = prefs.book.value,
            chapter = prefs.chapter.value,
        ),
    )

    val chapter: StateFlow<Async<List<Verse>>> = chapterKey
        .flatMapLatest { key ->
            flow {
                emit(Async.Loading)
                emit(
                    try {
                        Async.Data(repository.chapter(key.bible, key.book, key.chapter))
                    } catch (e: Exception) {
                        Async.Error(e)
                    },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Async.Loading)

    init {
        viewModelScope.launch { _bibles.value = repository.bibles() }
        loadBooks()
        probeReadiness()
        observeSelection()
        observePlayback()
    }

    // ── Loading ────────────────────────────────────────────────────────────

    fun loadBooks() {
        _books.value = Async.Loading
        viewModelScope.launch {
            _books.value = try {
                Async.Data(repository.books())
            } catch (e: Exception) {
                Async.Error(e)
            }
        }
    }

    fun reloadBibles() {
        viewModelScope.launch { _bibles.value = repository.bibles() }
    }

    fun retryChapter() {
        refresh.value += 1
    }

    /** Re-probes the speech engine, e.g. after voice data was installed. */
    fun probeReadiness(forceReload: Boolean = false) {
        viewModelScope.launch {
            if (forceReload) ttsEngine.reload()
            _readiness.value = ttsEngine.prepare()
        }
    }

    // ── Selection ──────────────────────────────────────────────────────────

    private fun observeSelection() {
        viewModelScope.launch {
            chapterKey.collect {
                if (!autoAdvancing) tts.stop()
            }
        }
    }

    fun selectBible(table: String) {
        prefs.setPrimaryBible(table)
        prefs.setBook(1)
        prefs.setChapter(1)
        prefs.setActiveVerse(null)
    }

    fun selectBook(book: Int) {
        prefs.setBook(book)
        prefs.setChapter(1)
        prefs.setActiveVerse(null)
    }

    fun selectChapter(chapter: Int) {
        prefs.setChapter(chapter)
        prefs.setActiveVerse(null)
    }

    /** Tapping the open verse again clears the selection. */
    fun toggleVerse(verse: Int) {
        prefs.setActiveVerse(if (prefs.activeVerse.value == verse) null else verse)
    }

    fun clearVerse() = prefs.setActiveVerse(null)

    /** Back to the default translation at Genesis 1. */
    fun reset() {
        viewModelScope.launch {
            prefs.setPrimaryBible(repository.resolveDefaultBible())
            prefs.setBook(1)
            prefs.setChapter(1)
            prefs.setActiveVerse(null)
        }
    }

    // ── Read aloud ─────────────────────────────────────────────────────────

    /** Starts at the verse the reader last tapped, if any. */
    fun startReading() {
        val verses = chapter.value.valueOrNull ?: return
        tts.start(verses, fromVerse = prefs.activeVerse.value)
    }

    private fun observePlayback() {
        viewModelScope.launch {
            tts.playback.collect { state ->
                if (state.reachedEnd) onChapterFinished()
            }
        }
    }

    private suspend fun onChapterFinished() {
        tts.clearReachedEnd()
        if (!prefs.continueToNextChapter.value) return

        val book = prefs.book.value
        val chapter = prefs.chapter.value
        if (chapter >= BibleSeed.maxChaptersFor(book)) return

        autoAdvancing = true
        try {
            prefs.setChapter(chapter + 1)
            prefs.setActiveVerse(null)
            val verses = repository.chapter(prefs.primaryBible.value, book, chapter + 1)
            tts.start(verses)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The next chapter is not available offline — stop rather than
            // stall with no audio and no explanation.
            tts.stop()
        } finally {
            autoAdvancing = false
        }
    }

    // ── Bookmarks ──────────────────────────────────────────────────────────

    fun isBookmarked(bible: String, book: Int, chapter: Int, verse: Int): Boolean =
        bookmarks.isBookmarked(bible, book, chapter, verse)

    /**
     * Adds or removes the bookmark for [verse], taking its text from the
     * chapter already on screen. Returns a message when it failed.
     */
    suspend fun toggleBookmark(verse: Int): String? {
        val key = chapterKey.value
        val text = chapter.value.valueOrNull
            ?.firstOrNull { it.number == verse }
            ?.text
            .orEmpty()
        return try {
            bookmarks.toggle(key.bible, key.book, key.chapter, verse, text)
            null
        } catch (e: Exception) {
            e.message ?: "Could not save the bookmark."
        }
    }

    // ── Comparison panel ───────────────────────────────────────────────────

    suspend fun comparison(
        bible: String,
        book: Int,
        chapter: Int,
        verse: Int,
    ): ComparisonResult? = repository.comparison(bible, book, chapter, verse)

    companion object {
        fun factory(container: AppContainer, graph: AppGraph): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T = ReaderViewModel(
                    repository = container.repository,
                    prefs = container.prefs,
                    bookmarks = graph.bookmarks,
                    tts = container.tts,
                    ttsEngine = container.ttsEngine,
                ) as T
            }
    }
}
