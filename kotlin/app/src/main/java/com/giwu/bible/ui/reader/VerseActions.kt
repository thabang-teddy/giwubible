package com.giwu.bible.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Compare
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * The actions for the verse the reader tapped.
 *
 * A tap used to open the comparison panel outright; it now selects the verse
 * and reveals this row, so sharing, comparing and bookmarking all sit in one
 * place and none of them is the accidental result of reading.
 *
 * Each action is optional: comparison is left out where the panel is already
 * on screen beside the chapter, and bookmarking where nobody is signed in.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VerseActions(
    isBookmarked: Boolean,
    onShare: () -> Unit,
    onCompare: (() -> Unit)?,
    onBookmarkToggle: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        VerseActionChip(
            label = "Share",
            icon = Icons.Outlined.Share,
            onClick = onShare,
        )

        if (onCompare != null) {
            VerseActionChip(
                label = "Compare",
                icon = Icons.Outlined.Compare,
                onClick = onCompare,
            )
        }

        if (onBookmarkToggle != null) {
            VerseActionChip(
                label = if (isBookmarked) "Bookmarked" else "Bookmark",
                icon = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                onClick = onBookmarkToggle,
                isOn = isBookmarked,
            )
        }
    }
}

@Composable
private fun VerseActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isOn: Boolean = false,
) {
    val accent = MaterialTheme.colorScheme.primary
    SuggestionChip(
        onClick = onClick,
        label = {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        },
        colors = if (isOn) {
            SuggestionChipDefaults.suggestionChipColors(
                labelColor = accent,
                iconContentColor = accent,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            )
        } else {
            SuggestionChipDefaults.suggestionChipColors()
        },
    )
}
