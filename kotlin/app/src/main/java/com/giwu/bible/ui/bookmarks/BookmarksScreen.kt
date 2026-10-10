@file:OptIn(ExperimentalMaterial3Api::class)

package com.giwu.bible.ui.bookmarks

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giwu.bible.AppGraph
import com.giwu.bible.data.BibleSeed
import com.giwu.bible.model.Async
import com.giwu.bible.model.Bookmark
import com.giwu.bible.ui.common.VersionBadge
import com.giwu.bible.ui.theme.giwu
import kotlinx.coroutines.launch

/**
 * The signed-in reader's saved verses.
 *
 * Tapping one jumps the reader to it; the cross removes it. Signed out, the
 * screen explains why it is empty rather than showing nothing.
 */
@Composable
fun BookmarksScreen(
    graph: AppGraph,
    onBack: () -> Unit,
    onOpenVerse: (Bookmark) -> Unit,
) {
    val user by graph.auth.user.collectAsStateWithLifecycle()
    val bookmarks by graph.bookmarks.bookmarks.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbars = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbars) },
        topBar = {
            TopAppBar(
                title = { Text("Bookmarks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (user != null) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    graph.auth.logout()
                                    onBack()
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = "Sign out",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                user == null -> SignedOutState(onBack = onBack)

                bookmarks is Async.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                else -> {
                    val list = bookmarks.valueOrNull.orEmpty()
                    if (list.isEmpty()) {
                        EmptyState()
                    } else {
                        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(list, key = { it.id }) { bookmark ->
                                BookmarkRow(
                                    bookmark = bookmark,
                                    onOpen = { onOpenVerse(bookmark) },
                                    onRemove = {
                                        scope.launch {
                                            try {
                                                graph.bookmarks.remove(bookmark.id)
                                            } catch (e: Exception) {
                                                snackbars.showSnackbar(
                                                    e.message
                                                        ?: "Could not remove the bookmark.",
                                                )
                                            }
                                        }
                                    },
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.giwu.divider,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkRow(
    bookmark: Bookmark,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VersionBadge(
                    abbreviation = "${shortBookName(bookmark.book)} " +
                        "${bookmark.chapter}:${bookmark.verse}",
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = bookmark.bible.removePrefix("t_").uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.giwu.muted,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = bookmark.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove bookmark",
                tint = MaterialTheme.giwu.muted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SignedOutState(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.BookmarkBorder,
            contentDescription = null,
            tint = MaterialTheme.giwu.muted,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Sign in to save bookmarks",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.giwu.muted,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onBack) { Text("Back to reading") }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.BookmarkBorder,
            contentDescription = null,
            tint = MaterialTheme.giwu.muted,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No bookmarks yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.giwu.muted,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tap a verse while reading, then use the bookmark button.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.giwu.muted,
            textAlign = TextAlign.Center,
        )
    }
}

/** Three-letter book names, which is all a reference chip has room for. */
private fun shortBookName(book: Int): String {
    val name = BibleSeed.books.firstOrNull { it.number == book }?.name ?: return "Book $book"
    // "1 Samuel" becomes "1Sa", "Genesis" becomes "Gen".
    return if (name.first().isDigit()) {
        name.first().toString() + name.substringAfter(' ').take(2)
    } else {
        name.take(3)
    }
}
