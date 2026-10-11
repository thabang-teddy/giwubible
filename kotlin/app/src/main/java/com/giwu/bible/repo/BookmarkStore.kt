package com.giwu.bible.repo

import com.giwu.bible.data.remote.AccountApi
import com.giwu.bible.model.Async
import com.giwu.bible.model.Bookmark
import com.giwu.bible.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The signed-in reader's bookmarks, app-wide.
 *
 * Bookmarks live on the server, so the list follows the session: it reloads
 * when someone signs in and empties when they sign out.
 */
class BookmarkStore(
    private val api: AccountApi,
    auth: AuthStore,
    private val scope: CoroutineScope,
) {

    private val _bookmarks = MutableStateFlow<Async<List<Bookmark>>>(Async.Data(emptyList()))
    val bookmarks: StateFlow<Async<List<Bookmark>>> = _bookmarks.asStateFlow()

    init {
        scope.launch {
            auth.user.collect { user -> onSessionChanged(user) }
        }
    }

    private suspend fun onSessionChanged(user: User?) {
        if (user == null) {
            _bookmarks.value = Async.Data(emptyList())
            return
        }
        reload()
    }

    suspend fun reload() {
        _bookmarks.value = Async.Loading
        _bookmarks.value = try {
            Async.Data(api.listBookmarks())
        } catch (e: Exception) {
            Async.Error(e)
        }
    }

    fun find(bible: String, book: Int, chapter: Int, verse: Int): Bookmark? =
        _bookmarks.value.valueOrNull?.firstOrNull { it.matches(bible, book, chapter, verse) }

    fun isBookmarked(bible: String, book: Int, chapter: Int, verse: Int): Boolean =
        find(bible, book, chapter, verse) != null

    /**
     * Adds or removes the bookmark for one verse.
     *
     * A removal is reflected locally before the request goes out — the row is
     * already gone from the reader's point of view — and restored if the
     * server refuses.
     */
    suspend fun toggle(
        bible: String,
        book: Int,
        chapter: Int,
        verse: Int,
        text: String,
    ) {
        val existing = find(bible, book, chapter, verse)
        val current = _bookmarks.value.valueOrNull ?: emptyList()

        if (existing != null) {
            _bookmarks.value = Async.Data(current.filter { it.id != existing.id })
            try {
                api.deleteBookmark(existing.id)
            } catch (e: Exception) {
                _bookmarks.value = Async.Data(current)
                throw e
            }
            return
        }

        val created = api.saveBookmark(bible, book, chapter, verse, text)
        _bookmarks.value = Async.Data(current + created)
    }

    /** Removes a bookmark by id, used by the bookmarks list itself. */
    suspend fun remove(id: Int) {
        val current = _bookmarks.value.valueOrNull ?: emptyList()
        _bookmarks.value = Async.Data(current.filter { it.id != id })
        try {
            api.deleteBookmark(id)
        } catch (e: Exception) {
            _bookmarks.value = Async.Data(current)
            throw e
        }
    }
}
