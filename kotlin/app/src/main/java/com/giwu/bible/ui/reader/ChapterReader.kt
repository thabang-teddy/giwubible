package com.giwu.bible.ui.reader

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giwu.bible.model.Async
import com.giwu.bible.model.Verse
import com.giwu.bible.ui.theme.giwu

/**
 * The chapter heading, the navigation pill and the verse body.
 *
 * Two highlights run independently: the verse the reader tapped (which drives
 * the comparison panel) and the verse being read aloud, which also scrolls
 * itself into view.
 */
@Composable
fun ChapterReader(
    bookName: String,
    book: Int,
    chapter: Int,
    verses: Async<List<Verse>>,
    activeVerse: Int?,
    speakingVerse: Int?,
    onVerseTap: (Int) -> Unit,
    onChapterChange: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ChapterNavPill(
            bookName = bookName,
            book = book,
            chapter = chapter,
            onChapterChange = onChapterChange,
        )

        when (verses) {
            is Async.Loading -> VerseSkeleton()
            is Async.Error -> ChapterError(onRetry = onRetry)
            is Async.Data -> VerseBody(
                bookName = bookName,
                chapter = chapter,
                verses = verses.value,
                activeVerse = activeVerse,
                speakingVerse = speakingVerse,
                onVerseTap = onVerseTap,
            )
        }
    }
}

@Composable
private fun VerseBody(
    bookName: String,
    chapter: Int,
    verses: List<Verse>,
    activeVerse: Int?,
    speakingVerse: Int?,
    onVerseTap: (Int) -> Unit,
) {
    val listState = rememberLazyListState()

    // Index 0 is the heading, so a verse sits one row further down.
    LaunchedEffect(speakingVerse, verses) {
        val index = verses.indexOfFirst { it.number == speakingVerse }
        if (index >= 0) listState.animateScrollToItem(index + 1)
    }

    if (verses.isEmpty()) {
        EmptyChapter()
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
    ) {
        item(key = "heading") {
            Text(
                text = "$bookName $chapter",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.giwu.verseText,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 12.dp),
            )
        }
        items(count = verses.size, key = { verses[it].number }) { index ->
            val verse = verses[index]
            VerseRow(
                verse = verse,
                isSelected = verse.number == activeVerse,
                isSpeaking = verse.number == speakingVerse,
                onTap = { onVerseTap(verse.number) },
            )
        }
    }
}

@Composable
private fun VerseRow(
    verse: Verse,
    isSelected: Boolean,
    isSpeaking: Boolean,
    onTap: () -> Unit,
) {
    val colors = MaterialTheme.giwu
    val target = when {
        // Being read aloud reads as a stronger fill plus an accent rule, so it
        // stays distinguishable from the tap-to-compare selection.
        isSpeaking -> colors.speakingHighlight
        isSelected -> colors.selectedHighlight
        else -> Color.Transparent
    }
    val background by animateColorAsState(target, label = "verseHighlight")

    val text = buildAnnotatedString {
        withStyle(
            SpanStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.W700,
                color = MaterialTheme.colorScheme.primary,
                baselineShift = BaselineShift.Superscript,
            ),
        ) {
            append("${verse.number} ")
        }
        append(verse.text)
    }

    val accent = MaterialTheme.colorScheme.primary
    val accentWidth = with(LocalDensity.current) { 3.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .drawBehind {
                // A rule down the left edge marks the verse being spoken.
                if (isSpeaking) {
                    drawRect(
                        color = accent,
                        size = Size(accentWidth, size.height),
                    )
                }
            }
            .clickable(onClick = onTap),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.verseText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun VerseSkeleton() {
    val widths = listOf(0.85f, 0.70f, 0.90f, 0.65f, 0.80f, 0.75f, 0.88f, 0.60f, 0.82f, 0.72f)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        widths.forEach { fraction ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(17.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ChapterError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Failed to load chapter.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.giwu.muted,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Retry")
        }
    }
}

@Composable
private fun EmptyChapter() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "This chapter is not on the device yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.giwu.muted,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Download the translation from Settings, or connect to the server.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.giwu.muted,
        )
    }
}
