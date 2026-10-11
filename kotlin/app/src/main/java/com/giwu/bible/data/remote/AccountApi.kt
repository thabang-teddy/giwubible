package com.giwu.bible.data.remote

import com.giwu.bible.model.Bookmark
import com.giwu.bible.model.User
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** A successful sign-in: the token to send and who it belongs to. */
data class Session(
    val token: String,
    val user: User,
)

/** The token-authenticated endpoints: the account and its bookmarks. */
class AccountApi(private val client: ApiClient) {

    // ── Auth ───────────────────────────────────────────────────────────────

    suspend fun register(name: String, email: String, password: String): Session =
        client.post(
            "auth/register",
            buildJsonObject {
                put("name", name)
                put("email", email)
                put("password", password)
                put("password_confirmation", password)
            },
        ).toSession()

    suspend fun login(email: String, password: String): Session =
        client.post(
            "auth/login",
            buildJsonObject {
                put("email", email)
                put("password", password)
            },
        ).toSession()

    suspend fun logout() {
        client.post("auth/logout")
    }

    /** Confirms a restored token still works, and returns who it belongs to. */
    suspend fun me(): User = client.get("auth/me").toUser()

    // ── Bookmarks ──────────────────────────────────────────────────────────

    suspend fun listBookmarks(): List<Bookmark> {
        val list = client.get("bookmarks").listOrNull("data", "bookmarks", "items")
            ?: return emptyList()
        return list.map { it.toBookmark() }
    }

    suspend fun saveBookmark(
        bible: String,
        book: Int,
        chapter: Int,
        verse: Int,
        text: String,
    ): Bookmark = client.post(
        "bookmarks",
        buildJsonObject {
            put("bible", bible)
            put("book", book)
            put("chapter", chapter)
            put("verse", verse)
            put("text", text)
        },
    ).toBookmark()

    suspend fun deleteBookmark(id: Int) = client.delete("bookmarks/$id")

    // ── Mapping ────────────────────────────────────────────────────────────

    private fun JsonElement.toSession(): Session = Session(
        token = string("token"),
        user = (objectOrNull("user") ?: this).toUser(),
    )

    private fun JsonElement.toUser(): User {
        // /auth/me answers with the user itself; register and login nest it.
        val source = objectOrNull("user") ?: objectOrNull("data") ?: this
        return User(
            id = source.int("id"),
            name = source.string("name"),
            email = source.string("email"),
        )
    }

    private fun JsonElement.toBookmark(): Bookmark {
        val source = objectOrNull("data") ?: this
        return Bookmark(
            id = source.int("id"),
            bible = source.string("bible"),
            book = source.int("book"),
            chapter = source.int("chapter"),
            verse = source.int("verse"),
            text = source.stringOrNull("text").orEmpty(),
        )
    }
}
