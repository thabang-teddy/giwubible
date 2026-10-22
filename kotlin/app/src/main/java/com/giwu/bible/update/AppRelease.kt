package com.giwu.bible.update

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat

/** A published release with an Android build attached. */
data class AppRelease(
    val tag: String,
    val title: String,
    val notes: String,
    val pageUrl: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val version: ReleaseVersion,
)

/**
 * The build running right now.
 *
 * Read from the package manager rather than `BuildConfig` so it is correct
 * whatever the build features are set to, and so a test can hand over a
 * version without an Android runtime.
 */
data class InstalledVersion(val name: String, val code: Long) {

    val display: String get() = "$name ($code)"

    companion object {
        fun of(context: Context): InstalledVersion {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            return InstalledVersion(
                name = info.versionName ?: "0",
                code = PackageInfoCompat.getLongVersionCode(info),
            )
        }
    }
}

/** `12.4 MB`, for the download prompt. An unknown size reads as empty. */
fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> ""
    bytes < 1024 * 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}
