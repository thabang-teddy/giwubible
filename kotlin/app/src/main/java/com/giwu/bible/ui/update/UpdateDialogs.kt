package com.giwu.bible.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giwu.bible.ui.theme.giwu
import com.giwu.bible.update.AppRelease
import com.giwu.bible.update.AppUpdates
import com.giwu.bible.update.UpdateState
import com.giwu.bible.update.formatBytes

/**
 * The update prompt, wherever the reader happens to be.
 *
 * Hosted once above the navigation graph rather than per screen: the daily
 * check can finish while the reader is in Settings, in Bookmarks or part-way
 * through a chapter, and the answer should look the same in all three.
 */
@Composable
fun UpdateDialogHost(updates: AppUpdates) {
    val state by updates.state.collectAsStateWithLifecycle()

    when (val current = state) {
        UpdateState.Idle -> Unit

        // A silent daily check says nothing until it has something to say.
        is UpdateState.Checking ->
            if (current.announced) CheckingDialog(onDismiss = updates::dismiss)

        UpdateState.UpToDate -> UpToDateDialog(
            version = updates.installed.display,
            onDismiss = updates::dismiss,
        )

        is UpdateState.Available -> AvailableDialog(
            release = current.release,
            installed = updates.installed.display,
            onUpdate = { updates.install(current.release) },
            onSkip = { updates.skip(current.release) },
            onDismiss = updates::dismiss,
        )

        is UpdateState.NeedsPermission -> PermissionDialog(
            onContinue = { updates.install(current.release) },
            onDismiss = updates::dismiss,
        )

        is UpdateState.Downloading -> DownloadingDialog(
            release = current.release,
            percent = current.percent,
        )

        is UpdateState.Failed -> FailedDialog(
            message = current.message,
            onDismiss = updates::dismiss,
        )
    }
}

@Composable
private fun CheckingDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) },
        title = { Text("Checking for updates") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun UpToDateDialog(version: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("You are up to date") },
        text = { Text("Giwu Bible $version is the newest build.") },
        confirmButton = { Button(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun AvailableDialog(
    release: AppRelease,
    installed: String,
    onUpdate: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
        title = { Text("Update available") },
        text = {
            Column {
                val size = formatBytes(release.apkSizeBytes)
                Text(
                    text = "Giwu Bible ${release.version.display}" +
                        if (size.isEmpty()) "" else " · $size",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "You have $installed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.giwu.muted,
                )
                if (release.notes.isNotBlank()) {
                    Spacer(Modifier.padding(top = 8.dp))
                    Text(
                        text = release.notes,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onUpdate) { Text("Update") } },
        dismissButton = {
            Column {
                TextButton(onClick = onDismiss) { Text("Later") }
                TextButton(onClick = onSkip) { Text("Skip this version") }
            }
        },
    )
}

@Composable
private fun PermissionDialog(onContinue: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Allow the install") },
        text = {
            Text(
                "Android needs your permission for Giwu Bible to install apps. " +
                    "Turn it on in the screen that just opened, come back, and " +
                    "tap Continue.",
            )
        },
        confirmButton = { Button(onClick = onContinue) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DownloadingDialog(release: AppRelease, percent: Int) {
    AlertDialog(
        // Not dismissible: the download carries on either way, and a prompt
        // that vanished would leave no way back to its progress.
        onDismissRequest = {},
        title = { Text("Downloading ${release.version.display}") },
        text = {
            Column {
                LinearProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.giwu.muted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun FailedDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update failed") },
        text = { Text(message) },
        confirmButton = { Button(onClick = onDismiss) { Text("Close") } },
    )
}
