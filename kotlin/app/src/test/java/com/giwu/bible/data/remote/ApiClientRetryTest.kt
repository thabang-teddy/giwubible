package com.giwu.bible.data.remote

import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * A whole translation is several megabytes, so a dropped connection partway
 * through is a normal event rather than an exceptional one.
 */
class ApiClientRetryTest {

    private val client = ApiClient()

    @Test
    fun `a dropped connection is retried until it succeeds`() = runTest {
        var attempts = 0
        val result = client.withRetries(attempts = 3) {
            attempts++
            if (attempts < 3) throw IOException("Software caused connection abort")
            "payload"
        }

        assertEquals("payload", result)
        assertEquals(3, attempts)
    }

    @Test
    fun `the last failure surfaces once the attempts run out`() = runTest {
        var attempts = 0
        val error = assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking {
                client.withRetries(attempts = 3) {
                    attempts++
                    throw IOException("attempt $attempts")
                }
            }
        }

        assertEquals(3, attempts)
        assertEquals("attempt 3", error.message)
    }

    @Test
    fun `a response the server sent is not retried`() = runTest {
        var attempts = 0
        val error = assertThrows(ApiException::class.java) {
            kotlinx.coroutines.runBlocking {
                client.withRetries(attempts = 3) {
                    attempts++
                    throw ApiException(404, "Not found")
                }
            }
        }

        // Asking again would get the same 404; only transport failures retry.
        assertEquals(1, attempts)
        assertEquals(404, error.statusCode)
    }

    @Test
    fun `a single attempt does not retry`() = runTest {
        var attempts = 0
        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking {
                client.withRetries(attempts = 1) {
                    attempts++
                    throw IOException("boom")
                }
            }
        }
        assertEquals(1, attempts)
    }
}
