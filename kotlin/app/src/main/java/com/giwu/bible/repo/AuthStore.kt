package com.giwu.bible.repo

import android.util.Log
import com.giwu.bible.data.TokenStore
import com.giwu.bible.data.remote.AccountApi
import com.giwu.bible.data.remote.ApiClient
import com.giwu.bible.model.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Who is signed in, app-wide.
 *
 * Reading the bible needs no account; signing in only adds bookmarks, so a
 * failed session restore clears the token quietly and leaves the reader
 * anonymous instead of interrupting them.
 */
class AuthStore(
    private val api: AccountApi,
    private val client: ApiClient,
    private val tokens: TokenStore,
    scope: CoroutineScope,
) {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    init {
        scope.launch { restoreSession() }
    }

    private suspend fun restoreSession() {
        val token = tokens.read() ?: return
        client.setAuthToken(token)
        try {
            _user.value = api.me()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "Stored session is no longer valid: ${e.message}")
            clearToken()
        }
    }

    suspend fun login(email: String, password: String): User {
        val session = api.login(email, password)
        storeToken(session.token)
        _user.value = session.user
        return session.user
    }

    suspend fun register(name: String, email: String, password: String): User {
        val session = api.register(name, email, password)
        storeToken(session.token)
        _user.value = session.user
        return session.user
    }

    suspend fun logout() {
        try {
            api.logout()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Best effort: the local token is dropped either way.
            Log.d(TAG, "Server-side logout failed: ${e.message}")
        }
        clearToken()
        _user.value = null
    }

    private fun storeToken(token: String) {
        tokens.write(token)
        client.setAuthToken(token)
    }

    private fun clearToken() {
        tokens.clear()
        client.setAuthToken(null)
    }

    private companion object {
        const val TAG = "AuthStore"
    }
}
