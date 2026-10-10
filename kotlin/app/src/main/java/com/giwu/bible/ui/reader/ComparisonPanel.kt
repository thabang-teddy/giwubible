@file:OptIn(ExperimentalMaterial3Api::class)

package com.giwu.bible.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.giwu.bible.model.Async
import com.giwu.bible.model.Bible
import com.giwu.bible.model.ComparisonResult
import com.giwu.bible.ui.common.VersionBadge
import com.giwu.bible.ui.theme.giwu

/**
 * The comparison panel: the tapped verse in other translations, and which
 * translations to compare against.
 *
 * Rendered inline as the right-hand column on a wide screen and inside a
 * bottom sheet on a phone.
 */
@Composable
fun ComparisonPanel(
    bibles: List<Bible>,
    primaryBible: String,
    book: Int,
    chapter: Int,
    verse: Int?,
    selectedTables: List<String>,
    onToggleTable: (String, List<String>) -> Unit,
    loadComparison: suspend (String) -> ComparisonResult?,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
) {
    var tab by remember { mutableIntStateOf(0) }

    // Every other translation, for the selection list.
    val comparable = bibles.filter { it.table != primaryBible }

    // Only downloaded translations can serve verses locally; a translation
    // that is not on the device would just render a blank card offline, so it
    // is kept out of the verses tab.
    val downloaded = comparable.filter { it.downloaded }
    val active = if (selectedTables.isEmpty()) {
        downloaded
    } else {
        downloaded.filter { selectedTables.contains(it.table) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PrimaryTabRow(selectedTabIndex = tab, modifier = Modifier.weight(1f)) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text("PARALLEL VERSES", style = MaterialTheme.typography.labelSmall) },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text("PARALLEL BIBLES", style = MaterialTheme.typography.labelSmall) },
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.giwu.muted,
                    modifier = Modifier.size(16.dp),
                )
            }
            if (onClose != null) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close the panel",
                        tint = MaterialTheme.giwu.muted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        when (tab) {
            0 -> ParallelVersesTab(
                bibles = active,
                book = book,
                chapter = chapter,
                verse = verse,
                loadComparison = loadComparison,
            )

            else -> ParallelBiblesTab(
                comparable = comparable,
                selectedTables = selectedTables,
                onToggleTable = onToggleTable,
            )
        }
    }
}

@Composable
private fun ParallelVersesTab(
    bibles: List<Bible>,
    book: Int,
    chapter: Int,
    verse: Int?,
    loadComparison: suspend (String) -> ComparisonResult?,
) {
    if (verse == null) {
        PanelMessage("Tap any verse to see parallel translations.")
        return
    }
    if (bibles.isEmpty()) {
        PanelMessage(
            "No other translations downloaded.\n" +
                "Download more from Settings → Manage translations.",
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(bibles, key = { it.table }) { bible ->
            val result by produceState<Async<ComparisonResult?>>(
                initialValue = Async.Loading,
                bible.table,
                book,
                chapter,
                verse,
            ) {
                value = try {
                    Async.Data(loadComparison(bible.table))
                } catch (e: Exception) {
                    Async.Error(e)
                }
            }

            when (val state = result) {
                is Async.Loading -> CardSkeleton()
                // A translation that cannot answer is left out rather than
                // filling the panel with error cards.
                is Async.Error -> Unit
                is Async.Data -> state.value?.let { VersionCard(it) }
            }
        }
    }
}

@Composable
private fun VersionCard(result: ComparisonResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.giwu.divider, RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = result.version,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            VersionBadge(abbreviation = result.abbreviation)
        }
        Spacer(Modifier.height(6.dp))
        if (result.text != null) {
            Text(text = result.text, style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(
                text = "• • •",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.giwu.muted,
            )
        }
    }
}

@Composable
private fun CardSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(90.dp)
                .height(11.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(13.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
    }
}

@Composable
private fun ParallelBiblesTab(
    comparable: List<Bible>,
    selectedTables: List<String>,
    onToggleTable: (String, List<String>) -> Unit,
) {
    val allTables = comparable.map { it.table }

    LazyColumn(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                text = "Select bibles to compare",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.giwu.muted,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(comparable, key = { it.table }) { bible ->
            // An empty selection means "all of them", so a downloaded
            // translation shows as ticked until one is explicitly turned off.
            val isChecked = bible.downloaded &&
                (selectedTables.isEmpty() || selectedTables.contains(bible.table))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Checkbox(
                    checked = isChecked,
                    enabled = bible.downloaded,
                    onCheckedChange = { onToggleTable(bible.table, allTables) },
                )
                Text(
                    text = bible.version,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (bible.downloaded) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.giwu.muted
                    },
                    modifier = Modifier.weight(1f),
                )
                if (!bible.downloaded) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = "Not downloaded",
                        tint = MaterialTheme.giwu.muted,
                        modifier = Modifier.size(13.dp),
                    )
                }
                VersionBadge(
                    abbreviation = bible.abbreviation,
                    enabled = bible.downloaded,
                )
            }
        }
    }
}

@Composable
private fun PanelMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.giwu.muted,
            textAlign = TextAlign.Center,
        )
    }
}
