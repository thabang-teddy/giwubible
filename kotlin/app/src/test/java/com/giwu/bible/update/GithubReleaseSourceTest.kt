package com.giwu.bible.update

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Reading one release out of what the GitHub API actually sends. */
class GithubReleaseSourceTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun release(body: String) =
        GithubReleaseSource.toRelease(json.parseToJsonElement(body))

    @Test
    fun `reads the tag, notes and APK asset`() {
        val parsed = release(
            """
            {
              "tag_name": "v1.0.0-staging.42",
              "name": "Staging v1.0.0-staging.42",
              "body": "  What changed  ",
              "html_url": "https://github.com/thabang-teddy/giwubible/releases/tag/x",
              "prerelease": true,
              "draft": false,
              "assets": [
                {
                  "name": "giwubible-v1.0.0-staging.42-windows-setup.exe",
                  "size": 90,
                  "browser_download_url": "https://example.test/setup.exe"
                },
                {
                  "name": "giwubible-v1.0.0-staging.42.apk",
                  "size": 12345,
                  "browser_download_url": "https://example.test/app.apk"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("v1.0.0-staging.42", parsed?.tag)
        assertEquals("Staging v1.0.0-staging.42", parsed?.title)
        assertEquals("What changed", parsed?.notes)
        assertEquals("https://example.test/app.apk", parsed?.apkUrl)
        assertEquals(12345L, parsed?.apkSizeBytes)
        assertEquals(42, parsed?.version?.build)
    }

    @Test
    fun `skips a release that attaches no APK`() {
        val parsed = release(
            """
            {
              "tag_name": "v1.0.0-staging.43",
              "draft": false,
              "assets": [
                {
                  "name": "giwubible-v1.0.0-staging.43-windows-setup.exe",
                  "size": 90,
                  "browser_download_url": "https://example.test/setup.exe"
                }
              ]
            }
            """.trimIndent(),
        )

        assertNull(parsed)
    }

    @Test
    fun `skips a draft`() {
        val parsed = release(
            """
            {
              "tag_name": "v9.9.9-staging.99",
              "draft": true,
              "assets": [
                {
                  "name": "giwubible.apk",
                  "size": 1,
                  "browser_download_url": "https://example.test/app.apk"
                }
              ]
            }
            """.trimIndent(),
        )

        assertNull(parsed)
    }

    @Test
    fun `falls back to the tag when the release has no title`() {
        val parsed = release(
            """
            {
              "tag_name": "v1.2.3-staging.4",
              "name": "",
              "draft": false,
              "assets": [
                {
                  "name": "giwubible.apk",
                  "size": 1,
                  "browser_download_url": "https://example.test/app.apk"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("v1.2.3-staging.4", parsed?.title)
        assertEquals("", parsed?.notes)
    }
}
