package com.giwu.bible.data.remote

import com.giwu.bible.model.Bible
import com.giwu.bible.model.Book
import com.giwu.bible.model.ComparisonResult
import com.giwu.bible.model.Verse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream

/** A whole translation as the download endpoint sends it. */
data class BibleDownload(
    val bible: Bible,
    val verses: List<Verse>,
)

/** The public bible endpoints of the Giwu JSON API. */
class BibleApi(private val client: ApiClient) {

    /** `GET /bibles` — every translation the server can serve. */
    suspend fun getBibles(): List<Bible> {
        val list = client.get("bibles").listOrNull("data", "bibles", "items")
            ?: return emptyList()
        return list.map {
            Bible(
                table = it.string("table"),
                abbreviation = it.string("abbreviation"),
                version = it.string("version"),
                downloaded = it.boolOrNull("downloaded") ?: false,
            )
        }
    }

    /** `GET /books` — the 66-book list. */
    suspend fun getBooks(): List<Book> {
        val list = client.get("books").listOrNull("data", "books", "items")
            ?: return emptyList()
        return list.map {
            Book(
                number = it.int("b"),
                name = it.string("n"),
                testament = it.stringOrNull("t"),
            )
        }
    }

    /** `GET /chapter` — every verse of one chapter in one translation. */
    suspend fun getChapter(bible: String, book: Int, chapter: Int): List<Verse> {
        val response = client.get(
            "chapter",
            mapOf("bible" to bible, "book" to book, "chapter" to chapter),
        )
        val list = response.listOrNull("data", "verses", "items") ?: return emptyList()
        return list.map {
            Verse(
                book = it.int("b"),
                chapter = it.int("c"),
                number = it.int("v"),
                text = it.string("t"),
            )
        }
    }

    /** `GET /verse` — one verse in the translation being compared against. */
    suspend fun getVerse(
        bible: String,
        book: Int,
        chapter: Int,
        verse: Int,
    ): ComparisonResult? {
        val response = client.get(
            "verse",
            mapOf("bible" to bible, "book" to book, "chapter" to chapter, "verse" to verse),
        )
        val data = response.objectOrNull("data") ?: return null
        return ComparisonResult(
            bible = data.string("bible"),
            version = data.string("version"),
            abbreviation = data.string("abbreviation"),
            text = data.stringOrNull("text"),
        )
    }

    /**
     * `GET /bibles/{table}/download` — the whole translation in one payload,
     * decoded straight off the socket.
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun downloadBible(table: String): BibleDownload = client.withRetries {
        client.getStreaming("bibles/$table/download") { stream ->
            val payload = client.json.decodeFromStream<DownloadEnvelope>(stream).data
            BibleDownload(
                bible = Bible(
                    table = payload.table,
                    abbreviation = payload.abbreviation,
                    version = payload.version,
                    downloaded = true,
                ),
                verses = payload.verses.map {
                    Verse(book = it.b, chapter = it.c, number = it.v, text = it.t)
                },
            )
        }
    }

    @Serializable
    private data class DownloadEnvelope(val data: DownloadPayload)

    @Serializable
    private data class DownloadPayload(
        @SerialName("table") val table: String,
        @SerialName("abbreviation") val abbreviation: String,
        @SerialName("version") val version: String,
        val verses: List<VerseDto> = emptyList(),
    )

    @Serializable
    private data class VerseDto(
        @Serializable(with = FlexibleIntSerializer::class) val b: Int,
        @Serializable(with = FlexibleIntSerializer::class) val c: Int,
        @Serializable(with = FlexibleIntSerializer::class) val v: Int,
        val t: String,
    )
}
