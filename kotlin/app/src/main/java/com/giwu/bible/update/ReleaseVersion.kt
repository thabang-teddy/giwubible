package com.giwu.bible.update

/**
 * A version read off a release tag.
 *
 * CI tags every build `<major>.<minor>.<run number>` — say `1.0.44` — and
 * stamps the APK from it: `versionName` is the whole tag, `versionCode` is the
 * run number. So the tag alone carries everything needed to decide whether
 * what is installed is behind.
 *
 * Tags in the older `v<version>-<channel>.<run number>` form, such as
 * `v1.0.0-staging.42`, still parse — the channel is optional — so releases
 * published before the scheme changed are still comparable.
 */
data class ReleaseVersion(
    /** The dotted core: `1.0.0` arrives as `[1, 0, 0]`. */
    val core: List<Int>,

    /** `staging` in the example above; null for a plain `v1.2.3` tag. */
    val channel: String?,

    /** The run number, which is also the APK's `versionCode`. */
    val build: Int?,
) {

    /** `1.0.0 (42)`, or just `1.0.0` when the tag carries no run number. */
    val display: String
        get() = core.joinToString(".") + (build?.let { " ($it)" } ?: "")

    /**
     * True when this release is ahead of the installed build.
     *
     * The core decides first, so `v1.1.0-staging.3` beats build 90 of 1.0.0.
     * Only when the cores match does the run number speak, which is what
     * separates two staging builds of the same version — and what makes a
     * locally built APK (`versionCode` 1) see every published build as newer.
     */
    fun isNewerThan(installedName: String, installedCode: Long): Boolean {
        val comparison = compare(core, coreOf(installedName))
        return when {
            comparison > 0 -> true
            comparison < 0 -> false
            else -> build != null && build > installedCode
        }
    }

    companion object {
        /**
         * `v1.0.0-staging.42` in three pieces.
         *
         * The channel class excludes `.` so the trailing run number cannot be
         * swallowed by it.
         */
        private val TAG = Regex("""^v?(\d+(?:\.\d+)*)(?:-([0-9A-Za-z-]+))?(?:\.(\d+))?$""")

        /** Null for a tag this scheme does not recognise, which is skipped. */
        fun parse(tag: String): ReleaseVersion? {
            val match = TAG.matchEntire(tag.trim()) ?: return null
            val (core, channel, build) = match.destructured
            return ReleaseVersion(
                core = coreOf(core),
                channel = channel.ifEmpty { null },
                build = build.toIntOrNull(),
            )
        }

        /** Reads the leading digits of each dotted part, ignoring any suffix. */
        private fun coreOf(version: String): List<Int> =
            version.trim().removePrefix("v").split('.')
                .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }

        /** Component-wise, with the shorter list padded out with zeroes. */
        private fun compare(left: List<Int>, right: List<Int>): Int {
            repeat(maxOf(left.size, right.size)) { index ->
                val difference = (left.getOrNull(index) ?: 0) - (right.getOrNull(index) ?: 0)
                if (difference != 0) return difference
            }
            return 0
        }
    }
}
