package com.giwu.bible.data.remote

import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/** The Giwu server the app talks to unless the reader points it elsewhere. */
const val DEFAULT_BASE_URL = "https://giwu.co.za/api/"

/** A non-2xx response, carrying the status so callers can explain it. */
class ApiException(
    val statusCode: Int,
    override val message: String,
) : IOException(message)

/**
 * The app's HTTP layer.
 *
 * Holds the base URL and bearer token centrally so a change in Settings
 * applies to every later request, and keeps two clients: the short-timeout one
 * for JSON endpoints and a patient one for whole-translation downloads, which
 * can run to several megabytes.
 */
class ApiClient(
    baseUrl: String = DEFAULT_BASE_URL,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {

    @Volatile
    var baseUrl: String = normalize(baseUrl)
        private set

    @Volatile
    private var token: String? = null

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /** Shares the connection pool with [client] but waits far longer. */
    private val downloadClient = client.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .build()

    fun setBaseUrl(url: String) {
        baseUrl = normalize(url)
    }

    /** Attaches, or with null removes, the bearer token on later requests. */
    fun setAuthToken(token: String?) {
        this.token = token
    }

    // ── Requests ───────────────────────────────────────────────────────────

    suspend fun get(path: String, query: Map<String, Any?> = emptyMap()): JsonElement =
        withContext(io) {
            client.newCall(request(path, query).build()).execute().use { response ->
                json.parseToJsonElement(response.requireBody())
            }
        }

    /**
     * Streams a response body straight into [read] instead of buffering it as
     * a string first, which keeps a whole-translation download out of a second
     * multi-megabyte copy.
     */
    suspend fun <T> getStreaming(
        path: String,
        query: Map<String, Any?> = emptyMap(),
        read: (InputStream) -> T,
    ): T = withContext(io) {
        downloadClient.newCall(request(path, query).build()).execute().use { response ->
            val body = response.body ?: throw ApiException(response.code, "Empty response")
            if (!response.isSuccessful) throw response.toApiException(null)
            read(body.byteStream())
        }
    }

    /**
     * Runs [block], retrying a dropped connection.
     *
     * A whole translation is several megabytes, and a mobile link (or an
     * emulator's NAT) drops often enough mid-transfer that one failure should
     * not send the reader back to the setup screen empty-handed. A response
     * the server actually sent is not retried: a 404 or a 500 will say the
     * same thing next time.
     */
    suspend fun <T> withRetries(
        attempts: Int = DOWNLOAD_ATTEMPTS,
        block: suspend () -> T,
    ): T {
        var last: IOException? = null
        repeat(attempts) { attempt ->
            try {
                return block()
            } catch (e: ApiException) {
                throw e
            } catch (e: IOException) {
                last = e
                if (attempt < attempts - 1) delay(RETRY_DELAY_MS * (attempt + 1))
            }
        }
        throw last ?: IOException("Request failed")
    }

    suspend fun post(path: String, body: JsonObject? = null): JsonElement =
        withContext(io) {
            val payload = (body?.toString() ?: "{}").toRequestBody(JSON_MEDIA_TYPE)
            client.newCall(request(path).post(payload).build()).execute().use { response ->
                val text = response.requireBody()
                if (text.isBlank()) JsonObject(emptyMap()) else json.parseToJsonElement(text)
            }
        }

    suspend fun delete(path: String) = withContext(io) {
        client.newCall(request(path).delete().build()).execute().use { response ->
            response.requireBody()
            Unit
        }
    }

    // ── Plumbing ───────────────────────────────────────────────────────────

    private fun request(path: String, query: Map<String, Any?> = emptyMap()): Request.Builder {
        val builder = Request.Builder()
            .url(resolve(path, query))
            .header("Accept", "application/json")
        token?.let { builder.header("Authorization", "Bearer $it") }
        return builder
    }

    private fun resolve(path: String, query: Map<String, Any?>): HttpUrl {
        val base = baseUrl.toHttpUrlOrNull()
            ?: throw ApiException(0, "Server URL is not a valid address: $baseUrl")
        val builder = base.newBuilder().addPathSegments(path.trimStart('/'))
        for ((key, value) in query) {
            if (value != null) builder.addQueryParameter(key, value.toString())
        }
        return builder.build()
    }

    /** Returns the body text, or throws [ApiException] for a non-2xx status. */
    private fun Response.requireBody(): String {
        val text = body?.string().orEmpty()
        if (!isSuccessful) throw toApiException(text)
        return text
    }

    private fun Response.toApiException(body: String?): ApiException {
        val detail = body?.let { json.messageOrNull(it) }
        return ApiException(
            statusCode = code,
            message = detail ?: "Request failed with HTTP $code",
        )
    }

    companion object {
        private const val DOWNLOAD_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1_500L

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /** Laravel sends validation and auth failures as `{"message": "..."}`. */
        private fun Json.messageOrNull(body: String): String? = runCatching {
            parseToJsonElement(body).stringOrNull("message")
        }.getOrNull()?.takeIf { it.isNotBlank() }

        /**
         * Trailing slash included: the base URL carries the `/api/` path
         * segment, and OkHttp would otherwise drop it when appending.
         */
        fun normalize(url: String): String = if (url.endsWith("/")) url else "$url/"
    }
}
