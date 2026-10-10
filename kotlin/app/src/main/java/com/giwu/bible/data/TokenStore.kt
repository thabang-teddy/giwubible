// Jetpack Security is deprecated upstream; see create() for why it is still
// used here and what happens when it is unavailable.
@file:Suppress("DEPRECATION")

package com.giwu.bible.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Holds the Sanctum personal-access token.
 *
 * The token is a bearer credential for the reader's account, so it goes into
 * [EncryptedSharedPreferences] rather than the ordinary preference file.
 * Keystore-backed encryption needs API 23 and can fail on devices with a
 * broken keystore, so this falls back to a private preference file and says so
 * in the log instead of leaving the reader unable to sign in.
 */
class TokenStore private constructor(private val prefs: SharedPreferences) {

    fun read(): String? = runCatching { prefs.getString(KEY_TOKEN, null) }
        .onFailure { Log.w(TAG, "Could not read the stored token", it) }
        .getOrNull()

    fun write(token: String) {
        prefs.edit { putString(KEY_TOKEN, token) }
    }

    fun clear() {
        prefs.edit { remove(KEY_TOKEN) }
    }

    companion object {
        private const val TAG = "TokenStore"
        private const val KEY_TOKEN = "giwu_auth_token"
        private const val SECURE_FILE = "giwu_secure"
        private const val PLAIN_FILE = "giwu_secure_fallback"

        fun create(context: Context): TokenStore {
            val app = context.applicationContext
            val encrypted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // App-private storage is already encrypted at rest on modern
                // Android, but a bearer token is worth the extra keystore
                // layer while this library still works; the fallback below
                // covers both old devices and it going away.
                runCatching {
                    val key = MasterKey.Builder(app)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()
                    EncryptedSharedPreferences.create(
                        app,
                        SECURE_FILE,
                        key,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                    )
                }.onFailure {
                    Log.w(TAG, "Encrypted storage unavailable; using private prefs", it)
                }.getOrNull()
            } else {
                null
            }

            return TokenStore(
                encrypted ?: app.getSharedPreferences(PLAIN_FILE, Context.MODE_PRIVATE),
            )
        }
    }
}
