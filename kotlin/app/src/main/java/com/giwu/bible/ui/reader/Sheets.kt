@file:OptIn(ExperimentalMaterial3Api::class)

package com.giwu.bible.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.giwu.bible.model.Bible
import com.giwu.bible.ui.common.VersionBadge
import com.giwu.bible.ui.theme.giwu

/** Chapter numbers as a grid, so Psalm 119 is one tap away. */
@Composable
fun ChapterPickerSheet(
    maxChapter: Int,
    current: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        SheetTitle("Select Chapter")
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.heightIn(max = 420.dp),
        ) {
            items((1..maxChapter).toList()) { number ->
                val isActive = number == current
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        )
                        .clickable { onSelect(number) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$number",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}

/**
 * The translation picker.
 *
 * Translations that are not downloaded are listed but not selectable — seeing
 * what else exists is the point, and the Settings screen is where they are
 * fetched.
 */
@Composable
fun BiblePickerSheet(
    bibles: List<Bible>,
    primaryBible: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetTitle("Select Bible Version")
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            bibles.forEach { bible ->
                val isSelected = bible.table == primaryBible
                ListItem(
                    leadingContent = {
                        VersionBadge(
                            abbreviation = bible.abbreviation,
                            enabled = bible.downloaded,
                        )
                    },
                    headlineContent = {
                        Text(
                            text = bible.version,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (bible.downloaded) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.giwu.muted
                            },
                        )
                    },
                    trailingContent = {
                        when {
                            isSelected -> Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Current translation",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )

                            !bible.downloaded -> Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = "Not downloaded",
                                tint = MaterialTheme.giwu.muted,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    },
                    modifier = Modifier.clickable(enabled = bible.downloaded) {
                        onSelect(bible.table)
                    },
                )
            }
        }
    }
}

/** Who is signed in, with the two things they can do about it. */
@Composable
fun AccountSheet(
    name: String,
    email: String,
    onDismiss: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onSignOut: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
            Text(text = name, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.giwu.muted,
            )
        }
        HorizontalDivider()
        ListItem(
            leadingContent = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = null) },
            headlineContent = { Text("Bookmarks") },
            modifier = Modifier.clickable(onClick = onOpenBookmarks),
        )
        ListItem(
            leadingContent = {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
            },
            headlineContent = { Text("Sign out") },
            modifier = Modifier.clickable(onClick = onSignOut),
        )
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * Shown when the device has a speech engine but no voice data for it.
 *
 * The system owns that download, so this explains what is missing and hands
 * off to the installer rather than pretending to fetch anything itself.
 */
@Composable
fun VoiceDataSheet(
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.RecordVoiceOver,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Reading voice not installed",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Android reads chapters aloud with its own voice, and this " +
                    "device has not downloaded the English voice data yet. " +
                    "Installing it is a one-time download, after which reading " +
                    "aloud works with no network at all.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.giwu.muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onInstall, modifier = Modifier.fillMaxWidth()) {
                Text("Install voice data")
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Not now")
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}
