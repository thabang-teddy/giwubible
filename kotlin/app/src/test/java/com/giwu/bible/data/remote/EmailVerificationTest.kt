package com.giwu.bible.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How the app decides an address is unconfirmed.
 *
 * Two independent signals, both of which have to keep working: the 403 the
 * bookmark endpoints answer with, and the `email_verified_at` field on the
 * account payload.
 */
class EmailVerificationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `a 403 is read as an unconfirmed address`() {
        val error = ApiException(403, "Your email address is not verified.")

        assertTrue(error.isEmailUnverified)
    }

    @Test
    fun `other failures are not mistaken for it`() {
        assertFalse(ApiException(401, "Unauthenticated.").isEmailUnverified)
        assertFalse(ApiException(404, "Not found").isEmailUnverified)
        assertFalse(ApiException(422, "The given data was invalid.").isEmailUnverified)
        assertFalse(ApiException(500, "Server error").isEmailUnverified)
        assertFalse(ApiException(0, "Server URL is not a valid address").isEmailUnverified)
    }

    @Test
    fun `an account with a null timestamp maps to an unconfirmed user`() {
        val user = json.parseToJsonElement(
            """{"id":1,"name":"Reader","email":"a@example.com","email_verified_at":null}""",
        ).toUser()

        assertEquals(1, user.id)
        assertEquals("a@example.com", user.email)
        assertFalse(user.emailVerified)
    }

    @Test
    fun `an account with a timestamp maps to a confirmed user`() {
        val user = json.parseToJsonElement(
            """
            {"id":7,"name":"Reader","email":"a@example.com",
             "email_verified_at":"2026-10-25T09:14:22.000000Z"}
            """.trimIndent(),
        ).toUser()

        assertTrue(user.emailVerified)
    }

    @Test
    fun `a nested account payload maps the same way`() {
        // register and login answer {"token": ..., "user": {...}};
        // /auth/me answers with the account itself.
        val nested = json.parseToJsonElement(
            """
            {"token":"1|abc","user":{"id":3,"name":"Reader","email":"a@example.com",
             "email_verified_at":null}}
            """.trimIndent(),
        ).toUser()

        assertEquals(3, nested.id)
        assertFalse(nested.emailVerified)
    }

    @Test
    fun `an account payload missing the field maps to unconfirmed`() {
        val user = json.parseToJsonElement(
            """{"id":1,"name":"Reader","email":"a@example.com"}""",
        ).toUser()

        assertFalse(user.emailVerified)
    }

    @Test
    fun `a null verified timestamp reads as absent`() {
        val user = json.parseToJsonElement(
            """{"id":1,"name":"Reader","email":"a@example.com","email_verified_at":null}""",
        )

        assertNull(user.stringOrNull("email_verified_at"))
    }

    @Test
    fun `a verified timestamp reads back as a value`() {
        val user = json.parseToJsonElement(
            """
            {"id":1,"name":"Reader","email":"a@example.com",
             "email_verified_at":"2026-10-25T09:14:22.000000Z"}
            """.trimIndent(),
        )

        assertEquals("2026-10-25T09:14:22.000000Z", user.stringOrNull("email_verified_at"))
    }

    @Test
    fun `an account payload without the field is treated as unconfirmed`() {
        val user = json.parseToJsonElement(
            """{"id":1,"name":"Reader","email":"a@example.com"}""",
        )

        assertNull(user.stringOrNull("email_verified_at"))
    }

    @Test
    fun `the resend reply says whether the address was already verified`() {
        val already = json.parseToJsonElement(
            """{"message":"Your email address is already verified.","verified":true}""",
        )
        val sent = json.parseToJsonElement(
            """{"message":"Verification email sent.","verified":false}""",
        )

        assertEquals(true, already.boolOrNull("verified"))
        assertEquals(false, sent.boolOrNull("verified"))
    }
}
