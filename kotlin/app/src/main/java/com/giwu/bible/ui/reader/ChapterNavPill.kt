package com.giwu.bible.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.giwu.bible.data.BibleSeed
import com.giwu.bible.ui.theme.giwu

/**
 * Previous / chapter / next, as one outlined pill.
 *
 * Tapping the middle opens the chapter grid, which beats stepping through
 * Psalms one chapter at a time.
 */
@Composable
fun ChapterNavPill(
    bookName: String,
    book: Int,
    chapter: Int,
    onChapterChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxChapter = BibleSeed.maxChaptersFor(book)
    val border = MaterialTheme.giwu.divider
    var showPicker by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, border, RoundedCornerShape(20.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PillButton(
                icon = Icons.Rounded.ChevronLeft,
                description = "Previous chapter",
                enabled = chapter > 1,
                onClick = { onChapterChange(chapter - 1) },
            )
            PillDivider(border)
            Row(
                modifier = Modifier
                    .clickable { showPicker = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .semantics { contentDescription = "Choose a chapter" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "$bookName $chapter",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.giwu.muted,
                    modifier = Modifier
                        .padding(start = 3.dp)
                        .size(15.dp),
                )
            }
            PillDivider(border)
            PillButton(
                icon = Icons.Rounded.ChevronRight,
                description = "Next chapter",
                enabled = chapter < maxChapter,
                onClick = { onChapterChange(chapter + 1) },
            )
        }
    }

    if (showPicker) {
        ChapterPickerSheet(
            maxChapter = maxChapter,
            current = chapter,
            onDismiss = { showPicker = false },
            onSelect = {
                showPicker = false
                onChapterChange(it)
            },
        )
    }
}

@Composable
private fun PillButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.giwu.muted.copy(alpha = 0.4f)
    }
    Box(
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun PillDivider(color: Color) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(color),
    )
}
