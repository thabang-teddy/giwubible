package com.giwu.bible.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * The download payload is decoded with generated serializers, so the
 * quoted-integer tolerance has to live in the serializer rather than in a
 * hand-written reader.
 */
class FlexibleIntSerializerTest {

    @Serializable
    private data class Row(
        @Serializable(with = FlexibleIntSerializer::class) val b: Int,
        val t: String,
    )

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a plain number decodes`() {
        assertEquals(1, json.decodeFromString<Row>("""{"b":1,"t":"text"}""").b)
    }

    @Test
    fun `a quoted number decodes`() {
        assertEquals(42, json.decodeFromString<Row>("""{"b":"42","t":"text"}""").b)
    }

    @Test
    fun `a float decodes to its integer part`() {
        assertEquals(7, json.decodeFromString<Row>("""{"b":7.0,"t":"text"}""").b)
    }

    @Test
    fun `whitespace around a quoted number is ignored`() {
        assertEquals(3, json.decodeFromString<Row>("""{"b":" 3 ","t":"text"}""").b)
    }

    @Test
    fun `text that is not a number is reported`() {
        assertThrows(ApiException::class.java) {
            json.decodeFromString<Row>("""{"b":"one","t":"text"}""")
        }
    }
}
