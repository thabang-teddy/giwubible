package com.giwu.bible.repo

import com.giwu.bible.data.BibleDatabase
import com.giwu.bible.data.remote.ApiClient
import com.giwu.bible.data.remote.DEFAULT_BASE_URL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The address of the Giwu server.
 *
 * Stored in the database rather than in preferences because it is app data the
 * reset flow owns, and loaded before the first request so every call starts
 * out pointing at the right host.
 */
class ServerSettings(
    private val db: BibleDatabase,
    private val client: ApiClient,
    initialUrl: String,
) {

    private val _url = MutableStateFlow(initialUrl)
    val url: StateFlow<String> = _url.asStateFlow()

    /** Persists [url] and points the HTTP client at it from the next call on. */
    suspend fun save(url: String) {
        val normalized = ApiClient.normalize(url.trim())
        db.saveServerUrl(normalized)
        client.setBaseUrl(normalized)
        _url.value = normalized
    }

    /** Returns to the address the app shipped with. */
    suspend fun reset() = save(DEFAULT_BASE_URL)

    companion object {
        /** Reads the stored URL, falling back to the compiled-in default. */
        suspend fun load(db: BibleDatabase, client: ApiClient): ServerSettings {
            val stored = runCatching { db.getServerUrl() }.getOrNull()
            val url = ApiClient.normalize(stored ?: DEFAULT_BASE_URL)
            client.setBaseUrl(url)
            return ServerSettings(db, client, url)
        }
    }
}
