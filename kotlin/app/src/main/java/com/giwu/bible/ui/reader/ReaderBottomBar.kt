package com.giwu.bible.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giwu.bible.ui.theme.giwu

/**
 * The reference of the selected verse, and what can be done with it.
 *
 * With nothing selected it falls back to the strapline, which keeps the bar
 * from reading as broken on first launch.
 */
@Composable
fun ReaderBottomBar(
    bookName: String?,
    chapter: Int,
    verse: Int?,
    versionAbbreviation: String?,
    isBookmarked: Boolean,
    onOpenPanel: (() -> Unit)?,
    onToggleBookmark: (() -> Unit)?,
    onShare: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val reference = if (verse != null && bookName != null && versionAbbreviation != null) {
        "${bookName.uppercase()} $chapter:$verse  •  ${versionAbbreviation.uppercase()}"
    } else {
        null
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.giwu.divider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = reference ?: "BUILT FOR SEEKERS OF TRUTH",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                    ),
                    color = if (reference != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.giwu.muted
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (onOpenPanel != null) {
                                Modifier.clickable(onClick = onOpenPanel)
                            } else {
                                Modifier
                            },
                        ),
                )

                IconButton(onClick = { onOpenPanel?.invoke() }, enabled = onOpenPanel != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = "Parallel translations",
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(
                    onClick = { onToggleBookmark?.invoke() },
                    enabled = onToggleBookmark != null,
                ) {
                    Icon(
                        imageVector = if (isBookmarked) {
                            Icons.Outlined.Bookmark
                        } else {
                            Icons.Outlined.BookmarkBorder
                        },
                        contentDescription = if (isBookmarked) {
                            "Remove bookmark"
                        } else {
                            "Bookmark this verse"
                        },
                        tint = if (isBookmarked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(onClick = { onShare?.invoke() }, enabled = onShare != null) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share this verse",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
