package com.giwu.bible.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Fetches a release APK and hands it to the system package installer. */
interface ApkInstaller {

    /** False when the reader has not allowed this app to install packages. */
    fun canInstallPackages(): Boolean

    /** Opens the system screen where that permission is granted. */
    fun requestInstallPermission()

    /** Downloads [release], reporting 0..100, then opens the installer. */
    suspend fun downloadAndInstall(release: AppRelease, onProgress: (Int) -> Unit)
}

/**
 * The real one, built on [DownloadManager].
 *
 * The transfer belongs to the system rather than to this process: a release
 * APK is tens of megabytes over whatever connection the reader happens to be
 * on, and leaving the app part-way through should not throw it away. The file
 * lands in the external files directory belonging to this app, which needs no
 * storage permission and is what `res/xml/file_paths.xml` shares back to the
 * installer.
 */
class AndroidApkInstaller(context: Context) : ApkInstaller {

    private val appContext = context.applicationContext

    private val downloads: DownloadManager?
        get() = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager

    override fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            appContext.packageManager.canRequestPackageInstalls()

    override fun requestInstallPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${appContext.packageName}".toUri(),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { appContext.startActivity(intent) }
    }

    override suspend fun downloadAndInstall(release: AppRelease, onProgress: (Int) -> Unit) {
        val file = withContext(Dispatchers.IO) { download(release, onProgress) }
        install(file)
    }

    private suspend fun download(release: AppRelease, onProgress: (Int) -> Unit): File {
        val manager = downloads ?: throw IOException("This device has no download manager.")
        val directory = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: throw IOException("No storage available for the download.")
        val target = File(directory, FILE_NAME)

        // DownloadManager appends -1, -2 and so on rather than overwriting, so
        // a leftover from an abandoned attempt has to go first or the install
        // intent would point at the stale copy.
        target.delete()

        val request = DownloadManager.Request(release.apkUrl.toUri())
            .setTitle("Giwu Bible ${release.version.display}")
            .setDescription("Downloading the update")
            .setMimeType(APK_MIME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(
                appContext,
                Environment.DIRECTORY_DOWNLOADS,
                FILE_NAME,
            )

        val id = manager.enqueue(request)
        try {
            awaitCompletion(manager, id, onProgress)
        } catch (e: Throwable) {
            manager.remove(id)
            throw e
        }
        return target
    }

    /** Polls the download row; DownloadManager offers nothing to suspend on. */
    private suspend fun awaitCompletion(
        manager: DownloadManager,
        id: Long,
        onProgress: (Int) -> Unit,
    ) {
        while (true) {
            val row = manager.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
                if (cursor.moveToFirst()) Row.read(cursor) else null
            } ?: throw IOException("The download was cancelled.")

            when (row.status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    onProgress(100)
                    return
                }

                DownloadManager.STATUS_FAILED ->
                    throw IOException("The download failed (error ${row.reason}).")

                else -> {
                    onProgress(row.percent)
                    delay(POLL_MS)
                }
            }
        }
    }

    private fun install(file: File) {
        val uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    private data class Row(
        val status: Int,
        val reason: Int,
        val soFar: Long,
        val total: Long,
    ) {
        /** The server does not always send a length; 0 then means unknown. */
        val percent: Int
            get() = if (total <= 0) 0 else ((soFar * 100) / total).toInt().coerceIn(0, 99)

        companion object {
            fun read(cursor: android.database.Cursor) = Row(
                status = cursor.getInt(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS),
                ),
                reason = cursor.getInt(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON),
                ),
                soFar = cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                        DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR,
                    ),
                ),
                total = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
                ),
            )
        }
    }

    private companion object {
        const val FILE_NAME = "giwu-bible-update.apk"
        const val APK_MIME = "application/vnd.android.package-archive"
        const val POLL_MS = 400L
    }
}
