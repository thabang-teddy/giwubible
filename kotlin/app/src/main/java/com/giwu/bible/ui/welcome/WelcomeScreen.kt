package com.giwu.bible.ui.welcome

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwu.bible.AppContainer
import com.giwu.bible.model.Bible
import com.giwu.bible.ui.common.VersionBadge
import com.giwu.bible.ui.theme.giwu
import com.giwu.bible.util.SqliteImport
import kotlinx.coroutines.launch

/**
 * First run, and "Manage translations" later on.
 *
 * Nothing can be read until at least one translation is on the device, so this
 * screen offers both ways to get one: download from the Giwu server, or
 * sideload a SQLite database the reader already has.
 */
@Composable
fun WelcomeScreen(
    container: AppContainer,
    onReady: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val viewModel: WelcomeViewModel = viewModel(factory = WelcomeViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbars = remember { SnackbarHostState() }

    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val staged = SqliteImport.stage(context, uri)
            if (staged == null || !SqliteImport.looksLikeBibleDatabase(staged)) {
                SqliteImport.discard(context)
                snackbars.showSnackbar(
                    "Invalid file — expected a Giwu Bible SQLite database.",
                )
                return@launch
            }
            viewModel.syncFromFile(staged.absolutePath, onReady)
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbars.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbars) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Header(onBack = onBack)
                BibleGrid(
                    bibles = state.bibles,
                    selected = state.selected,
                    enabled = !state.working,
                    onToggle = viewModel::toggle,
                    modifier = Modifier.weight(1f),
                )
                Footer(
                    label = when {
                        state.hasPendingDownloads -> "Download (${state.selected.size})"
                        state.canContinue -> "Continue"
                        // Nothing ticked and nothing on the device yet: say
                        // what is missing rather than offering a dead
                        // "Continue".
                        else -> "Select a translation"
                    },
                    canSubmit = !state.working &&
                        (state.hasPendingDownloads || state.canContinue),
                    working = state.working,
                    onSubmit = { viewModel.downloadSelected(onReady) },
                    onUpload = {
                        // Picked files are validated before anything is read
                        // out of them, so any MIME type is allowed through —
                        // a .db file is often reported as octet-stream.
                        pickFile.launch(arrayOf("*/*"))
                    },
                    onOpenSettings = onOpenSettings,
                )
            }

            if (state.working) {
                ProgressOverlay(
                    status = state.status,
                    done = state.done,
                    total = state.total,
                )
            }
        }
    }
}

@Composable
private fun Header(onBack: (() -> Unit)?) {
    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp)) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.padding(bottom = 4.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(text = "Giwu Bible", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Choose Bible translations to download.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.giwu.muted,
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun BibleGrid(
    bibles: List<Bible>,
    selected: Set<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (bibles.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No Bible versions found.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 170.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        items(bibles, key = { it.table }) { bible ->
            BibleCard(
                bible = bible,
                selected = selected.contains(bible.table),
                enabled = enabled,
                onClick = { onToggle(bible.table) },
            )
        }
    }
}

@Composable
private fun BibleCard(
    bible: Bible,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) {
                    primary.copy(alpha = 0.08f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) primary else MaterialTheme.giwu.divider,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VersionBadge(abbreviation = bible.abbreviation)
            Spacer(Modifier.weight(1f))
            if (bible.downloaded) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Already downloaded",
                    tint = primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = bible.version,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (bible.downloaded) "On this device" else "Not downloaded",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.giwu.muted,
        )
    }
}

@Composable
private fun Footer(
    label: String,
    canSubmit: Boolean,
    working: Boolean,
    onSubmit: () -> Unit,
    onUpload: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {
        HorizontalDivider(color = MaterialTheme.giwu.divider)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onSubmit,
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.giwu.divider)
            Text(
                text = "or",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.giwu.muted,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.giwu.divider)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onUpload,
            enabled = !working,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.UploadFile,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Upload SQLite Database")
        }
        TextButton(
            onClick = onOpenSettings,
            enabled = !working,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Settings", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ProgressOverlay(status: String, done: Int, total: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(32.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = status.ifBlank { "Working…" },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                if (total > 0) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { done.toFloat() / total.toFloat() },
                        modifier = Modifier
                            .width(200.dp)
                            .height(4.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "$done of $total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.giwu.muted,
                    )
                }
            }
        }
    }
}
