@file:OptIn(ExperimentalMaterial3Api::class)

package com.giwu.bible.ui.reader

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwu.bible.AppContainer
import com.giwu.bible.AppGraph
import com.giwu.bible.R
import com.giwu.bible.model.Verse
import com.giwu.bible.tts.TtsReadiness
import com.giwu.bible.tts.TtsStatus
import com.giwu.bible.ui.theme.DesktopBreakpoint
import com.giwu.bible.ui.theme.PanelWidth
import com.giwu.bible.ui.theme.SidebarWidth
import com.giwu.bible.ui.theme.giwu
import kotlinx.coroutines.launch

/**
 * The reading screen.
 *
 * One composable serves both layouts: a phone gets the book list in a drawer
 * and the comparison panel in a bottom sheet, while a tablet or a desktop
 * window gets all three columns at once.
 */
@Composable
fun ReadScreen(
    container: AppContainer,
    graph: AppGraph,
    onOpenSettings: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenRegister: () -> Unit,
) {
    val viewModel: ReaderViewModel = viewModel(
        factory = ReaderViewModel.factory(container, graph),
    )
    val prefs = container.prefs
    val tts = container.tts
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbars = remember { SnackbarHostState() }

    val bibles by viewModel.bibles.collectAsStateWithLifecycle()
    val books by viewModel.books.collectAsStateWithLifecycle()
    val verses by viewModel.chapter.collectAsStateWithLifecycle()
    val readiness by viewModel.readiness.collectAsStateWithLifecycle()
    val playback by tts.playback.collectAsStateWithLifecycle()

    val primaryBible by prefs.primaryBible.collectAsStateWithLifecycle()
    val book by prefs.book.collectAsStateWithLifecycle()
    val chapter by prefs.chapter.collectAsStateWithLifecycle()
    val activeVerse by prefs.activeVerse.collectAsStateWithLifecycle()
    val darkMode by prefs.darkMode.collectAsStateWithLifecycle()
    val speechRate by prefs.speechRate.collectAsStateWithLifecycle()
    val parallelBibles by prefs.parallelBibles.collectAsStateWithLifecycle()

    val user by graph.auth.user.collectAsStateWithLifecycle()
    val bookmarks by graph.bookmarks.bookmarks.collectAsStateWithLifecycle()

    val currentBook = books.valueOrNull?.firstOrNull { it.number == book }
    val currentBible = bibles.firstOrNull { it.table == primaryBible }
    val isBookmarked = activeVerse?.let { verse ->
        bookmarks.valueOrNull?.any { it.matches(primaryBible, book, chapter, verse) } == true
    } == true

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val settingsDrawerState = rememberDrawerState(DrawerValue.Closed)
    var showPanelSheet by remember { mutableStateOf(false) }
    var showVoiceData by remember { mutableStateOf(false) }

    // The engine may have gained voice data while the reader was away.
    LaunchedEffect(Unit) { viewModel.probeReadiness(forceReload = true) }

    // Window width, not screen width: a freeform or split-screen window can
    // be far narrower than the display it is on.
    val containerWidth = LocalWindowInfo.current.containerSize.width
    val isWide = with(LocalDensity.current) { containerWidth.toDp() } >= DesktopBreakpoint

    // Clearing the selection closes the sheet, and selecting a verse on a
    // phone opens it.
    LaunchedEffect(activeVerse, isWide) {
        showPanelSheet = activeVerse != null && !isWide
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val body: @Composable () -> Unit = {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbars) },
                topBar = {
                    ReaderTopBar(
                        isWide = isWide,
                        bookmarkCount = bookmarks.valueOrNull?.size ?: 0,
                        isSignedIn = user != null,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenBookmarks = onOpenBookmarks,
                        onOpenSettings = { scope.launch { settingsDrawerState.open() } },
                    )
                },
                bottomBar = {
                    ReaderBottomBar(
                        bookName = currentBook?.name,
                        chapter = chapter,
                        verse = activeVerse,
                        versionAbbreviation = currentBible?.abbreviation,
                        isBookmarked = isBookmarked,
                        onOpenPanel = if (activeVerse != null && !isWide) {
                            { showPanelSheet = true }
                        } else {
                            null
                        },
                        onToggleBookmark = activeVerse?.let { verse ->
                            {
                                if (user == null) {
                                    onOpenLogin()
                                } else {
                                    scope.launch {
                                        viewModel.toggleBookmark(verse)?.let {
                                            snackbars.showSnackbar(it)
                                        }
                                    }
                                }
                            }
                        },
                        onShare = activeVerse?.let { verse ->
                            {
                                shareVerse(
                                    context = context,
                                    bookName = currentBook?.name,
                                    chapter = chapter,
                                    verse = verse,
                                    abbreviation = currentBible?.abbreviation,
                                    verses = verses.valueOrNull,
                                )
                            }
                        },
                    )
                },
                floatingActionButton = {
                    if (readiness != null && readiness != TtsReadiness.UNSUPPORTED &&
                        !verses.valueOrNull.isNullOrEmpty()
                    ) {
                        ReadAloudControls(
                            playback = playback,
                            rate = speechRate,
                            onStart = {
                                if (readiness == TtsReadiness.NEEDS_VOICE_DATA) {
                                    showVoiceData = true
                                } else {
                                    viewModel.startReading()
                                }
                            },
                            onPlayPause = {
                                if (playback.status == TtsStatus.PAUSED) {
                                    tts.resume()
                                } else {
                                    tts.pause()
                                }
                            },
                            onPrevious = tts::previous,
                            onNext = tts::next,
                            onStop = tts::stop,
                            onCycleRate = prefs::cycleSpeechRate,
                        )
                    }
                },
            ) { padding ->
                val reader: @Composable (Modifier) -> Unit = { modifier ->
                    ChapterReader(
                        bookName = currentBook?.name.orEmpty(),
                        book = book,
                        chapter = chapter,
                        verses = verses,
                        activeVerse = activeVerse,
                        speakingVerse = playback.verse,
                        onVerseTap = viewModel::toggleVerse,
                        onChapterChange = viewModel::selectChapter,
                        onRetry = viewModel::retryChapter,
                        modifier = modifier,
                    )
                }

                if (isWide) {
                    Row(modifier = Modifier.padding(padding)) {
                        BookListPanel(
                            books = books,
                            selectedBook = book,
                            onSelectBook = viewModel::selectBook,
                            onRetry = viewModel::loadBooks,
                            modifier = Modifier.width(SidebarWidth),
                        )
                        VerticalDivider(color = MaterialTheme.giwu.divider)
                        reader(Modifier.weight(1f))
                        VerticalDivider(color = MaterialTheme.giwu.divider)
                        ComparisonPanel(
                            bibles = bibles,
                            primaryBible = primaryBible,
                            book = book,
                            chapter = chapter,
                            verse = activeVerse,
                            selectedTables = parallelBibles,
                            onToggleTable = prefs::toggleParallelBible,
                            loadComparison = { table ->
                                viewModel.comparison(table, book, chapter, activeVerse ?: 1)
                            },
                            modifier = Modifier
                                .width(PanelWidth)
                                .fillMaxHeight(),
                        )
                    }
                } else {
                    reader(Modifier.padding(padding))
                }
            }
        }

        val readerLayout: @Composable () -> Unit = {
            if (isWide) {
                body()
            } else {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            BookListPanel(
                                books = books,
                                selectedBook = book,
                                onSelectBook = viewModel::selectBook,
                                onRetry = viewModel::loadBooks,
                                onClose = { scope.launch { drawerState.close() } },
                                afterSelect = { scope.launch { drawerState.close() } },
                            )
                        }
                    },
                    content = body,
                )
            }
        }

        // Material's modal drawer always hangs off the start edge, so the
        // settings drawer is laid out in the opposite direction and
        // everything inside it flipped back — the usual way to reach the end
        // edge. Mirroring the reader's own direction rather than hard-coding
        // Rtl keeps the drawer on the end edge in an RTL locale too.
        val closeSettings = { scope.launch { settingsDrawerState.close() }; Unit }
        val readingDirection = LocalLayoutDirection.current
        val drawerDirection = when (readingDirection) {
            LayoutDirection.Ltr -> LayoutDirection.Rtl
            LayoutDirection.Rtl -> LayoutDirection.Ltr
        }
        CompositionLocalProvider(LocalLayoutDirection provides drawerDirection) {
            ModalNavigationDrawer(
                drawerState = settingsDrawerState,
                // Without this a drag anywhere in the chapter would pull the
                // drawer open, including the swipe that opens the book list.
                gesturesEnabled = settingsDrawerState.isOpen,
                drawerContent = {
                    // The sheet itself stays right-to-left so Material rounds
                    // the edge facing the chapter; only its contents flip back.
                    ModalDrawerSheet {
                        CompositionLocalProvider(
                            LocalLayoutDirection provides readingDirection,
                        ) {
                            SettingsDrawerSheet(
                                bibles = bibles,
                                primaryBible = primaryBible,
                                darkMode = darkMode,
                                user = user,
                                onToggleDarkMode = prefs::toggleDarkMode,
                                onSelectBible = { table ->
                                    closeSettings()
                                    viewModel.selectBible(table)
                                },
                                onReset = {
                                    closeSettings()
                                    viewModel.reset()
                                },
                                onOpenLogin = {
                                    closeSettings()
                                    onOpenLogin()
                                },
                                onOpenRegister = {
                                    closeSettings()
                                    onOpenRegister()
                                },
                                onSignOut = {
                                    closeSettings()
                                    scope.launch { graph.auth.logout() }
                                },
                                onOpenBookmarks = {
                                    closeSettings()
                                    onOpenBookmarks()
                                },
                                onOpenAllSettings = {
                                    closeSettings()
                                    onOpenSettings()
                                },
                                onClose = closeSettings,
                            )
                        }
                    }
                },
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides readingDirection) {
                    readerLayout()
                }
            }
        }
    }

    if (showPanelSheet && !isWide) {
        ModalBottomSheet(
            onDismissRequest = {
                showPanelSheet = false
                viewModel.clearVerse()
            },
        ) {
            ComparisonPanel(
                bibles = bibles,
                primaryBible = primaryBible,
                book = book,
                chapter = chapter,
                verse = activeVerse,
                selectedTables = parallelBibles,
                onToggleTable = prefs::toggleParallelBible,
                loadComparison = { table ->
                    viewModel.comparison(table, book, chapter, activeVerse ?: 1)
                },
                onClose = {
                    showPanelSheet = false
                    viewModel.clearVerse()
                },
                modifier = Modifier.fillMaxHeight(0.6f),
            )
        }
    }

    if (showVoiceData) {
        VoiceDataSheet(
            onDismiss = { showVoiceData = false },
            onInstall = {
                showVoiceData = false
                installVoiceData(context)
            },
        )
    }
}

/**
 * The reader's top bar.
 *
 * Reset, the theme switch and the translation chip used to sit here; they are
 * in the settings drawer now, so the bar carries the book list, bookmarks and
 * the way into that drawer.
 */
@Composable
private fun ReaderTopBar(
    isWide: Boolean,
    bookmarkCount: Int,
    isSignedIn: Boolean,
    onOpenDrawer: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        title = {
            // One line only: a wrapped title squeezes the actions beside it.
            if (!isWide) {
                Text(
                    text = "Giwu Bible",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            if (isWide) {
                Image(
                    painter = painterResource(R.drawable.giwu_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = "Book list",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onOpenBookmarks, modifier = Modifier.size(ActionSize)) {
                BadgedBox(
                    badge = {
                        if (bookmarkCount > 0) {
                            Badge { Text(if (bookmarkCount > 9) "9+" else "$bookmarkCount") }
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmarks",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(ActionSize)) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = if (isSignedIn) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
        },
    )
}

/**
 * Compact action buttons: at the default 48dp the title has no room left on a
 * narrow phone.
 */
private val ActionSize = 38.dp

/** Hands off to the system's voice-data installer. */
private fun installVoiceData(context: Context) {
    val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "This device has no text-to-speech installer. Check Settings → " +
                "Accessibility → Text-to-speech.",
            Toast.LENGTH_LONG,
        ).show()
    }
}

private fun shareVerse(
    context: Context,
    bookName: String?,
    chapter: Int,
    verse: Int,
    abbreviation: String?,
    verses: List<Verse>?,
) {
    val text = verses?.firstOrNull { it.number == verse }?.text ?: return
    val reference = listOfNotNull(bookName, "$chapter:$verse").joinToString(" ")
    val suffix = abbreviation?.let { " ($it)" }.orEmpty()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, reference + suffix)
        putExtra(Intent.EXTRA_TEXT, "$text\n\n— $reference$suffix")
    }
    context.startActivity(Intent.createChooser(intent, "Share verse"))
}
