package com.giwu.bible.ui.reader

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.giwu.bible.model.Async
import com.giwu.bible.model.Book
import com.giwu.bible.ui.common.ErrorState
import com.giwu.bible.ui.theme.giwu

/**
 * The book list, with a filter box.
 *
 * Used inline as the left column on a wide screen and inside the navigation
 * drawer on a phone, which is why closing and "after select" are separate
 * callbacks.
 */
@Composable
fun BookListPanel(
    books: Async<List<Book>>,
    selectedBook: Int,
    onSelectBook: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    afterSelect: (() -> Unit)? = null,
) {
    var query by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.giwu.sidebar),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 14.dp, end = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "BIBLE BOOKS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.giwu.muted,
            )
            Spacer(Modifier.weight(1f))
            if (onClose != null) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close the book list",
                    tint = MaterialTheme.giwu.muted,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onClose),
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = {
                Text(
                    text = "Search books...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.giwu.muted,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.giwu.muted,
                    modifier = Modifier.size(15.dp),
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )

        Spacer(Modifier.height(2.dp))

        when (books) {
            is Async.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            }

            is Async.Error -> ErrorState(
                title = "Failed to load books",
                detail = books.cause.message,
                onRetry = onRetry,
            )

            is Async.Data -> {
                val filtered = books.value.filter {
                    it.name.contains(query.trim(), ignoreCase = true)
                }
                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (books.value.isEmpty()) {
                                "No books — check the server"
                            } else {
                                "No books match that search"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.giwu.muted,
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.Top,
                    ) {
                        items(filtered, key = { it.number }) { book ->
                            BookRow(
                                book = book,
                                isActive = book.number == selectedBook,
                                onClick = {
                                    onSelectBook(book.number)
                                    afterSelect?.invoke()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookRow(book: Book, isActive: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isActive) primary.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(18.dp)
                .background(if (isActive) primary else Color.Transparent),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = book.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isActive) primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (isActive) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
