package com.giwu.bible.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * The server's JSON is not always shaped the same way, so these cover the
 * variations seen in practice: a bare array, a wrapped array, quoted
 * integers, and nulls where a string is expected.
 */
class JsonSupportTest {

    private val json = Json

    private fun parse(text: String) = json.parseToJsonElement(text)

    @Test
    fun `a bare array is returned as the list`() {
        val list = parse("""[{"b":1},{"b":2}]""").listOrNull("data")
        assertEquals(2, list?.size)
    }

    @Test
    fun `a wrapped array is found under any known key`() {
        assertEquals(1, parse("""{"data":[{"b":1}]}""").listOrNull("data")?.size)
        assertEquals(1, parse("""{"verses":[{"b":1}]}""").listOrNull("data", "verses")?.size)
        assertEquals(1, parse("""{"items":[{"b":1}]}""").listOrNull("data", "items")?.size)
    }

    @Test
    fun `an unrecognised shape returns null rather than throwing`() {
        assertNull(parse("""{"unexpected":{"b":1}}""").listOrNull("data", "verses"))
        assertNull(parse("""42""").listOrNull("data"))
    }

    @Test
    fun `integers are read whether quoted or not`() {
        val element = parse("""{"b":1,"c":"2","v":3.0}""")
        assertEquals(1, element.int("b"))
        assertEquals(2, element.int("c"))
        assertEquals(3, element.int("v"))
    }

    @Test
    fun `a missing field reports which one it was`() {
        val error = assertThrows(ApiException::class.java) {
            parse("""{"b":1}""").string("t")
        }
        assertEquals(true, error.message.contains("\"t\""))
    }

    @Test
    fun `a JSON null reads as a Kotlin null, not as the literal text`() {
        assertNull(parse("""{"t":null}""").stringOrNull("t"))
    }

    @Test
    fun `the downloaded flag accepts numbers, strings and booleans`() {
        assertEquals(true, parse("""{"downloaded":1}""").boolOrNull("downloaded"))
        assertEquals(true, parse("""{"downloaded":"true"}""").boolOrNull("downloaded"))
        assertEquals(false, parse("""{"downloaded":0}""").boolOrNull("downloaded"))
        assertEquals(false, parse("""{"downloaded":false}""").boolOrNull("downloaded"))
        assertNull(parse("""{"downloaded":"maybe"}""").boolOrNull("downloaded"))
        assertNull(parse("""{}""").boolOrNull("downloaded"))
    }

    @Test
    fun `base URLs always keep a trailing slash`() {
        assertEquals("https://giwu.co.za/api/", ApiClient.normalize("https://giwu.co.za/api"))
        assertEquals("https://giwu.co.za/api/", ApiClient.normalize("https://giwu.co.za/api/"))
    }
}
