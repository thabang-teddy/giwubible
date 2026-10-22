package com.giwu.bible.update

import com.giwu.bible.data.remote.ApiClient
import com.giwu.bible.data.remote.boolOrNull
import com.giwu.bible.data.remote.stringOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Where the app looks for a build newer than its own. */
interface ReleaseSource {

    /** The newest release carrying an APK, or null when there is none. */
    suspend fun latest(): AppRelease?
}

/**
 * Reads the repository releases straight from GitHub.
 *
 * Deliberately not through the Giwu API: a sideloaded APK has to be able to
 * find its successor even when the reader has pointed the app at a server
 * that knows nothing about releases, and this way the update check needs
 * nothing deployed. If the unauthenticated rate limit (60 an hour per IP)
 * ever becomes a problem, the Laravel app already caches the same release in
 * `App\Services\GithubReleases` — point [RELEASES_URL] at an endpoint over
 * that instead, and nothing else here changes.
 *
 * `/releases` rather than `/releases/latest` because CI publishes every build
 * as a PRE-release, and `latest` skips those entirely.
 */
class GithubReleaseSource(
    private val client: ApiClient,
    private val url: String = RELEASES_URL,
) : ReleaseSource {

    override suspend fun latest(): AppRelease? {
        val releases = client.getExternal(url, HEADERS) as? JsonArray ?: return null
        return releases.firstNotNullOfOrNull { toRelease(it) }
    }

    companion object {
        /** The newest few, because the very newest may be Windows-only. */
        const val RELEASES_URL =
            "https://api.github.com/repos/thabang-teddy/giwubible/releases?per_page=5"

        private val HEADERS = mapOf(
            "Accept" to "application/vnd.github+json",
            "X-GitHub-Api-Version" to "2022-11-28",
            "User-Agent" to "giwubible-android",
        )

        /**
         * One release of the GitHub payload, or null when it is of no use
         * here — a draft, a tag in another scheme, or a release with no APK
         * attached (the Windows installer on its own, for instance).
         */
        internal fun toRelease(element: JsonElement): AppRelease? {
            val release = element as? JsonObject ?: return null
            if (release.boolOrNull("draft") == true) return null

            val tag = release.stringOrNull("tag_name") ?: return null
            val version = ReleaseVersion.parse(tag) ?: return null

            val apk = (release["assets"] as? JsonArray)
                ?.filterIsInstance<JsonObject>()
                ?.firstOrNull { asset ->
                    asset.stringOrNull("name")?.endsWith(".apk", ignoreCase = true) == true
                }
                ?: return null
            val apkUrl = apk.stringOrNull("browser_download_url") ?: return null

            return AppRelease(
                tag = tag,
                title = release.stringOrNull("name")?.takeIf { it.isNotBlank() } ?: tag,
                notes = release.stringOrNull("body").orEmpty().trim(),
                pageUrl = release.stringOrNull("html_url").orEmpty(),
                apkUrl = apkUrl,
                apkSizeBytes = apk.stringOrNull("size")?.toLongOrNull() ?: 0L,
                version = version,
            )
        }
    }
}
