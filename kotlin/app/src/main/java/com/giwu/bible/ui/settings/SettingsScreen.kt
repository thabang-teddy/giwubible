@file:OptIn(ExperimentalMaterial3Api::class)

package com.giwu.bible.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giwu.bible.AppContainer
import com.giwu.bible.AppGraph
import com.giwu.bible.data.AppPrefs
import com.giwu.bible.tts.TtsReadiness
import com.giwu.bible.ui.reader.formatRate
import com.giwu.bible.ui.theme.giwu
import kotlinx.coroutines.launch

/**
 * Appearance, read-aloud, the server address and the reset switch.
 *
 * The voice itself belongs to Android, so the voice row explains its state and
 * hands off to the system screens rather than managing a download here.
 */
@Composable
fun SettingsScreen(
    container: AppContainer,
    graph: AppGraph,
    onBack: () -> Unit,
    onManageTranslations: () -> Unit,
) {
    val prefs = container.prefs
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbars = remember { SnackbarHostState() }

    val darkMode by prefs.darkMode.collectAsStateWithLifecycle()
    val rate by prefs.speechRate.collectAsStateWithLifecycle()
    val announceNumbers by prefs.announceVerseNumbers.collectAsStateWithLifecycle()
    val continueToNext by prefs.continueToNextChapter.collectAsStateWithLifecycle()
    val serverUrl by graph.serverSettings.url.collectAsStateWithLifecycle()

    var voiceState by remember { mutableStateOf<TtsReadiness?>(null) }
    var showServerDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showRateMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        container.ttsEngine.reload()
        voiceState = container.ttsEngine.prepare()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbars) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("APPEARANCE")
            ListItem(
                leadingContent = {
                    Icon(Icons.Outlined.DarkMode, contentDescription = null)
                },
                headlineContent = { Text("Dark mode") },
                trailingContent = {
                    Switch(checked = darkMode, onCheckedChange = { prefs.setDarkMode(it) })
                },
                modifier = Modifier.clickable { prefs.toggleDarkMode() },
            )
            HorizontalDivider(color = MaterialTheme.giwu.divider)

            SectionHeader("READ ALOUD")
            ListItem(
                leadingContent = {
                    Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null)
                },
                headlineContent = { Text("Reading voice") },
                supportingContent = {
                    Text(
                        text = when (voiceState) {
                            TtsReadiness.READY ->
                                "Installed · works offline"

                            TtsReadiness.NEEDS_VOICE_DATA ->
                                "Voice data not installed on this device"

                            TtsReadiness.UNSUPPORTED ->
                                "No speech engine available on this device"

                            null -> "Checking…"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                trailingContent = {
                    TextButton(
                        onClick = {
                            if (voiceState == TtsReadiness.NEEDS_VOICE_DATA) {
                                openVoiceInstaller(context) {
                                    scope.launch { snackbars.showSnackbar(it) }
                                }
                            } else {
                                openSpeechSettings(context) {
                                    scope.launch { snackbars.showSnackbar(it) }
                                }
                            }
                        },
                    ) {
                        Text(
                            if (voiceState == TtsReadiness.NEEDS_VOICE_DATA) {
                                "Install"
                            } else {
                                "Manage"
                            },
                        )
                    }
                },
            )
            ListItem(
                leadingContent = { Icon(Icons.Outlined.Speed, contentDescription = null) },
                headlineContent = { Text("Reading speed") },
                trailingContent = {
                    Row {
                        TextButton(onClick = { showRateMenu = true }) {
                            Text("${formatRate(rate)}x")
                        }
                        DropdownMenu(
                            expanded = showRateMenu,
                            onDismissRequest = { showRateMenu = false },
                        ) {
                            AppPrefs.SPEECH_RATE_PRESETS.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text("${formatRate(preset)}x") },
                                    onClick = {
                                        showRateMenu = false
                                        prefs.setSpeechRate(preset)
                                    },
                                )
                            }
                        }
                    }
                },
            )
            ListItem(
                leadingContent = {
                    Icon(Icons.Outlined.FormatListNumbered, contentDescription = null)
                },
                headlineContent = { Text("Announce verse numbers") },
                supportingContent = {
                    Text(
                        text = "Say \"Verse 3\" before each verse",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                trailingContent = {
                    Switch(
                        checked = announceNumbers,
                        onCheckedChange = { prefs.setAnnounceVerseNumbers(it) },
                    )
                },
                modifier = Modifier.clickable {
                    prefs.setAnnounceVerseNumbers(!announceNumbers)
                },
            )
            ListItem(
                leadingContent = {
                    Icon(Icons.AutoMirrored.Outlined.PlaylistPlay, contentDescription = null)
                },
                headlineContent = { Text("Continue to next chapter") },
                supportingContent = {
                    Text(
                        text = "Keep reading when a chapter ends",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                trailingContent = {
                    Switch(
                        checked = continueToNext,
                        onCheckedChange = { prefs.setContinueToNextChapter(it) },
                    )
                },
                modifier = Modifier.clickable {
                    prefs.setContinueToNextChapter(!continueToNext)
                },
            )
            HorizontalDivider(color = MaterialTheme.giwu.divider)

            SectionHeader("CONNECTION")
            ListItem(
                leadingContent = { Icon(Icons.Outlined.Dns, contentDescription = null) },
                headlineContent = { Text("Server URL") },
                supportingContent = {
                    Text(
                        text = serverUrl,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                modifier = Modifier.clickable { showServerDialog = true },
            )
            HorizontalDivider(color = MaterialTheme.giwu.divider)

            SectionHeader("DATA")
            ListItem(
                leadingContent = {
                    Icon(Icons.Outlined.DownloadForOffline, contentDescription = null)
                },
                headlineContent = { Text("Manage translations") },
                supportingContent = {
                    Text(
                        text = "Download more Bible versions",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                modifier = Modifier.clickable(onClick = onManageTranslations),
            )
            HorizontalDivider(color = MaterialTheme.giwu.divider)

            SectionHeader("DANGER ZONE")
            ListItem(
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                headlineContent = {
                    Text("Reset app", color = MaterialTheme.colorScheme.error)
                },
                supportingContent = {
                    Text(
                        text = "Removes all downloaded Bibles and returns to setup.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                modifier = Modifier.clickable { showResetDialog = true },
            )
        }
    }

    if (showServerDialog) {
        ServerUrlDialog(
            initial = serverUrl,
            onDismiss = { showServerDialog = false },
            onSave = { url ->
                showServerDialog = false
                scope.launch {
                    graph.serverSettings.save(url)
                    snackbars.showSnackbar("Server URL updated.")
                }
            },
            onResetToDefault = {
                showServerDialog = false
                scope.launch {
                    graph.serverSettings.reset()
                    snackbars.showSnackbar("Server URL reset to the default.")
                }
            },
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset the app?") },
            text = {
                Text(
                    "Every downloaded Bible is removed and the app returns to " +
                        "setup. Bookmarks on your account are not affected.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        scope.launch {
                            container.repository.reset()
                            onManageTranslations()
                        }
                    },
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ServerUrlDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onResetToDefault: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    val isValid = value.startsWith("http://") || value.startsWith("https://")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Server URL") },
        text = {
            Column {
                Text(
                    text = "The address of the Giwu API, including the /api/ path.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.giwu.muted,
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    isError = !isValid,
                    supportingText = {
                        if (!isValid) Text("Must start with http:// or https://")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                TextButton(onClick = onResetToDefault) { Text("Use the default server") }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(value.trim()) }, enabled = isValid) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.giwu.muted,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

private fun openVoiceInstaller(
    context: Context,
    onError: (String) -> Unit,
) = startSystemScreen(
    context = context,
    intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
    onError = onError,
)

private fun openSpeechSettings(
    context: Context,
    onError: (String) -> Unit,
) = startSystemScreen(
    context = context,
    intent = Intent("com.android.settings.TTS_SETTINGS")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    fallback = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
    onError = onError,
)

/**
 * Opens a system screen, falling back when the OEM does not ship it.
 *
 * Text-to-speech settings in particular are missing or renamed on a fair
 * number of devices, so a failure says where to look instead of doing nothing.
 */
private fun startSystemScreen(
    context: Context,
    intent: Intent,
    fallback: Intent? = null,
    onError: (String) -> Unit,
) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        if (fallback != null) {
            try {
                context.startActivity(fallback)
                return
            } catch (e: ActivityNotFoundException) {
                // Fall through to the message below.
            }
        }
        onError(
            "This device has no text-to-speech screen. Look under Settings → " +
                "Accessibility → Text-to-speech.",
        )
    }
}
