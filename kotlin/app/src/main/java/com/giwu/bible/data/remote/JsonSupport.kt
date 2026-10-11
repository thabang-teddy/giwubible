package com.giwu.bible.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Tolerant readers for the Giwu API.
 *
 * The same endpoint can answer with a bare array or wrap it in `data`,
 * `verses`, `books` or `items`, and some PHP/PDO/SQLite combinations send
 * integers as quoted strings. Rather than fail a response that is perfectly
 * readable, these helpers accept every shape the server is known to send —
 * matching what the Flutter client does.
 */

/** The list in [this], whether it is the whole payload or under one of [keys]. */
fun JsonElement.listOrNull(vararg keys: String): List<JsonElement>? = when (this) {
    is JsonArray -> this
    is JsonObject -> keys.firstNotNullOfOrNull { this[it] as? JsonArray }
    else -> null
}

fun JsonElement.objectOrNull(key: String): JsonObject? =
    (this as? JsonObject)?.get(key) as? JsonObject

fun JsonElement.stringOrNull(key: String): String? {
    val value = (this as? JsonObject)?.get(key) ?: return null
    if (value is JsonNull) return null
    return (value as? JsonPrimitive)?.content
}

fun JsonElement.string(key: String): String =
    stringOrNull(key) ?: throw ApiException(0, "Missing \"$key\" in the server response")

fun JsonElement.intOrNull(key: String): Int? =
    stringOrNull(key)?.trim()?.toDoubleOrNull()?.toInt()

fun JsonElement.int(key: String): Int =
    intOrNull(key) ?: throw ApiException(0, "Missing \"$key\" in the server response")

fun JsonElement.boolOrNull(key: String): Boolean? {
    val raw = stringOrNull(key) ?: return null
    return when (raw.lowercase()) {
        "1", "true" -> true
        "0", "false" -> false
        else -> null
    }
}

/**
 * Reads an integer that may arrive as a number, a quoted number, or a float.
 *
 * Used on the download payload, which is decoded straight off the socket with
 * generated serializers rather than through the helpers above.
 */
object FlexibleIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.giwu.bible.FlexibleInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeInt()
        val raw = jsonDecoder.decodeJsonElement().jsonPrimitive.content.trim()
        return raw.toIntOrNull()
            ?: raw.toDoubleOrNull()?.toInt()
            ?: throw ApiException(0, "Expected a number in the server response, got \"$raw\"")
    }

    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
}
