import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/client.dart';
import '../data/chapter_counts.dart';
import '../models/bible.dart';
import '../models/book.dart';
import '../providers/auth_provider.dart';
import '../providers/bibles_provider.dart';
import '../providers/bookmarks_provider.dart';
import '../providers/chapter_provider.dart';
import '../providers/database_provider.dart';
import '../providers/prefs_provider.dart';
import '../providers/tts_provider.dart';
import '../theme.dart';
import '../widgets/app_panel.dart';
import '../widgets/app_sidebar.dart';
import '../widgets/book_drawer.dart';
import '../widgets/chapter_reader.dart';
import '../widgets/read_aloud_fab.dart';
import '../widgets/settings_drawer.dart';
import '../widgets/verify_email.dart';
import 'bookmarks_page.dart';
import 'login_page.dart';

class ReadPage extends ConsumerStatefulWidget {
  const ReadPage({super.key});

  @override
  ConsumerState<ReadPage> createState() => _ReadPageState();
}

class _ReadPageState extends ConsumerState<ReadPage>
    with WidgetsBindingObserver {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  PersistentBottomSheetController? _sheetController;

  /// True while the reader is deliberately rolling into the next chapter, so
  /// the "selection changed" guard doesn't stop the playback it just caused.
  bool _autoAdvancing = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    ref.read(ttsControllerProvider.notifier).stop();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.detached) {
      ref.read(ttsControllerProvider.notifier).stop();
    }
    if (state == AppLifecycleState.resumed) {
      // The verification link opens the web app, so coming back to this
      // window is the moment to find out whether it was followed.
      ref.read(authProvider.notifier).refresh();
    }
  }

  // ── Read aloud ───────────────────────────────────────────────────────────

  /// Any change of bible, book or chapter invalidates what is being spoken.
  void _onSelectionChanged() {
    if (_autoAdvancing) return;
    ref.read(ttsControllerProvider.notifier).stop();
  }

  Future<void> _onChapterFinished() async {
    ref.read(ttsControllerProvider.notifier).clearReachedEnd();
    if (!ref.read(continueToNextChapterProvider)) return;

    final book = ref.read(selectedBookProvider);
    final chapter = ref.read(selectedChapterProvider);
    if (chapter >= maxChaptersForBook(book)) return;

    _autoAdvancing = true;
    try {
      ref.read(selectedChapterProvider.notifier).set(chapter + 1);
      ref.read(activeVerseProvider.notifier).state = null;

      final verses = await ref.read(
        chapterVersesProvider(
          (
            bible: ref.read(primaryBibleProvider),
            book: book,
            chapter: chapter + 1,
          ),
        ).future,
      );
      if (!mounted) return;
      await ref.read(ttsControllerProvider.notifier).start(verses);
    } on Exception catch (_) {
      // Next chapter unavailable offline — stop rather than stall silently.
      if (mounted) await ref.read(ttsControllerProvider.notifier).stop();
    } finally {
      _autoAdvancing = false;
    }
  }

  // ── Verse selection ──────────────────────────────────────────────────────

  void _selectVerse(int tapped) {
    final isDesktop = MediaQuery.of(context).size.width >= kDesktopBreakpoint;
    final current = ref.read(activeVerseProvider);

    if (current == tapped) {
      // Deselect
      ref.read(activeVerseProvider.notifier).state = null;
      if (!isDesktop) _closeSheet();
      return;
    }

    ref.read(activeVerseProvider.notifier).state = tapped;

    // On mobile open the sheet; on desktop the panel auto-updates.
    if (!isDesktop && _sheetController == null) {
      _openSheet();
    }
  }

  void _openSheet() {
    _sheetController = _scaffoldKey.currentState?.showBottomSheet(
      (ctx) => Consumer(
        builder: (ctx, ref, _) {
          final bibles = ref.watch(biblesProvider).valueOrNull ?? [];
          final primaryBible = ref.watch(primaryBibleProvider);
          final book = ref.watch(selectedBookProvider);
          final chapter = ref.watch(selectedChapterProvider);
          final isDark = Theme.of(ctx).brightness == Brightness.dark;
          return Container(
            decoration: BoxDecoration(
              color: isDark ? kDarkSurface : Colors.white,
              borderRadius:
                  const BorderRadius.vertical(top: Radius.circular(16)),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.12),
                  blurRadius: 20,
                  offset: const Offset(0, -4),
                ),
              ],
            ),
            child: AppPanel(
              bibles: bibles,
              primaryBible: primaryBible,
              book: book,
              chapter: chapter,
              onClose: _closeSheet,
              isSheet: true,
            ),
          );
        },
      ),
      enableDrag: true,
      backgroundColor: Colors.transparent,
    );

    _sheetController?.closed.whenComplete(() {
      ref.read(activeVerseProvider.notifier).state = null;
      _sheetController = null;
    });
  }

  void _closeSheet() {
    ref.read(activeVerseProvider.notifier).state = null;
    _sheetController?.close();
    _sheetController = null;
  }

  Future<void> _reset() async {
    final db = ref.read(bibleDatabaseProvider);
    final bible = await db.resolveDefaultBible();
    ref.read(primaryBibleProvider.notifier).set(bible);
    ref.read(selectedBookProvider.notifier).set(1);
    ref.read(selectedChapterProvider.notifier).set(1);
    _closeSheet();
  }

  // ── Build ────────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    ref.listen<String>(primaryBibleProvider, (_, __) => _onSelectionChanged());
    ref.listen<int>(selectedBookProvider, (_, __) => _onSelectionChanged());
    ref.listen<int>(selectedChapterProvider, (_, __) => _onSelectionChanged());
    ref.listen<TtsState>(ttsControllerProvider, (prev, next) {
      if (next.reachedEnd && prev?.reachedEnd != true) _onChapterFinished();
    });

    final width = MediaQuery.of(context).size.width;
    final isDesktop = width >= kDesktopBreakpoint;

    final biblesAsync = ref.watch(biblesProvider);
    final booksAsync = ref.watch(booksProvider);
    final primaryBible = ref.watch(primaryBibleProvider);
    final book = ref.watch(selectedBookProvider);
    final chapter = ref.watch(selectedChapterProvider);
    final activeVerse = ref.watch(activeVerseProvider);

    final chapterKey = (bible: primaryBible, book: book, chapter: chapter);
    final versesAsync = ref.watch(chapterVersesProvider(chapterKey));

    final bibles = biblesAsync.valueOrNull ?? [];
    final books = booksAsync.valueOrNull ?? [];

    final currentBook = books.cast<BookModel?>().firstWhere(
          (b) => b?.b == book,
          orElse: () => null,
        );
    final currentBible = bibles.cast<BibleModel?>().firstWhere(
          (b) => b?.table == primaryBible,
          orElse: () => null,
        );

    final appBar = _buildAppBar(context, isDesktop: isDesktop);

    final mainContent = ChapterReader(
      bookName: currentBook?.n ?? '',
      book: book,
      chapter: chapter,
      versesAsync: versesAsync,
      onVerseTap: _selectVerse,
    );

    final readAloudFab = ReadAloudFab(verses: versesAsync.valueOrNull);

    final bottomBar = _BottomBar(
      bookName: currentBook?.n,
      chapter: chapter,
      verse: activeVerse,
      versionAbbr: currentBible?.abbreviation,
      primaryBible: primaryBible,
      book: book,
      onOpenPanel: activeVerse != null && !isDesktop ? _openSheet : null,
    );

    final settingsDrawer = SettingsDrawer(
      bibles: bibles,
      onSelectBible: (table) {
        ref.read(primaryBibleProvider.notifier).set(table);
        ref.read(selectedBookProvider.notifier).set(1);
        ref.read(selectedChapterProvider.notifier).set(1);
        _closeSheet();
      },
      onReset: _reset,
    );

    if (isDesktop) {
      // ── Desktop: 3-column layout ─────────────────────────────────────────
      return Scaffold(
        key: _scaffoldKey,
        endDrawer: settingsDrawer,
        appBar: appBar,
        body: Row(
          children: [
            SizedBox(
              width: kSidebarWidth,
              child: AppSidebar(
                books: books,
                isLoading: booksAsync.isLoading,
                isError: booksAsync.hasError,
                errorMessage: booksAsync.error?.toString(),
                onRetry: () => ref.invalidate(booksProvider),
              ),
            ),
            const VerticalDivider(width: 1),
            Expanded(child: mainContent),
            const VerticalDivider(width: 1),
            SizedBox(
              width: kPanelWidth,
              child: AppPanel(
                bibles: bibles,
                primaryBible: primaryBible,
                book: book,
                chapter: chapter,
              ),
            ),
          ],
        ),
        floatingActionButton: readAloudFab,
        bottomNavigationBar: bottomBar,
      );
    }

    // ── Mobile: drawer + bottom sheet ──────────────────────────────────────
    return Scaffold(
      key: _scaffoldKey,
      drawer: BookDrawer(
        books: books,
        isLoading: booksAsync.isLoading,
        isError: booksAsync.hasError,
        errorMessage: booksAsync.error?.toString(),
        onRetry: () => ref.invalidate(booksProvider),
      ),
      endDrawer: settingsDrawer,
      appBar: appBar,
      body: mainContent,
      floatingActionButton: readAloudFab,
      bottomNavigationBar: bottomBar,
    );
  }

  // ── AppBar ───────────────────────────────────────────────────────────────

  /// The reader's app bar.
  ///
  /// Reset, the theme switch, the account icon and the translation chip used
  /// to sit here; they are in the settings drawer now, so the bar carries the
  /// book list, bookmarks and the way into that drawer.
  PreferredSizeWidget _buildAppBar(
    BuildContext context, {
    required bool isDesktop,
  }) {
    final user = ref.watch(authProvider);
    final bookmarksAsync = ref.watch(bookmarksProvider);
    final bmCount = bookmarksAsync.valueOrNull?.length ?? 0;

    return AppBar(
      automaticallyImplyLeading: false,
      leading: isDesktop
          ? Padding(
              padding: const EdgeInsets.all(10),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: Image.asset('assets/icon.png', fit: BoxFit.contain),
              ),
            )
          : IconButton(
              icon: const Icon(Icons.menu, size: 20),
              onPressed: () => _scaffoldKey.currentState?.openDrawer(),
              tooltip: 'Book list',
            ),
      title: isDesktop ? null : const Text('Giwu Bible'),
      actions: [
        // Bookmarks
        Stack(
          alignment: Alignment.center,
          children: [
            IconButton(
              icon: const Icon(Icons.bookmark_border, size: 18),
              tooltip: 'Bookmarks',
              onPressed: () => Navigator.of(context).push(
                MaterialPageRoute<void>(builder: (_) => const BookmarksPage()),
              ),
            ),
            if (bmCount > 0)
              Positioned(
                top: 8,
                right: 8,
                child: Container(
                  width: 14,
                  height: 14,
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.primary,
                    shape: BoxShape.circle,
                  ),
                  child: Center(
                    child: Text(
                      bmCount > 9 ? '9+' : '$bmCount',
                      style: const TextStyle(
                          fontSize: 8,
                          color: Colors.white,
                          fontWeight: FontWeight.bold),
                    ),
                  ),
                ),
              ),
          ],
        ),
        // Settings — opens the drawer that holds everything else
        IconButton(
          icon: Icon(
            Icons.settings_outlined,
            size: 18,
            color: user != null ? Theme.of(context).colorScheme.primary : null,
          ),
          tooltip: user != null
              ? 'Settings — signed in as ${user.name}'
              : 'Settings',
          onPressed: () => _scaffoldKey.currentState?.openEndDrawer(),
        ),
        const SizedBox(width: 4),
      ],
    );
  }
}

// ── Bottom bar ─────────────────────────────────────────────────────────────

class _BottomBar extends ConsumerWidget {
  const _BottomBar({
    required this.bookName,
    required this.chapter,
    required this.verse,
    required this.versionAbbr,
    required this.primaryBible,
    required this.book,
    required this.onOpenPanel,
  });

  final String? bookName;
  final int chapter;
  final int? verse;
  final String? versionAbbr;
  final String primaryBible;
  final int book;
  final VoidCallback? onOpenPanel;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final borderColor = isDark ? kDarkBorder : kDivider;
    final verseRef = verse != null && bookName != null && versionAbbr != null
        ? '${bookName!.toUpperCase()} $chapter:$verse  •  ${versionAbbr!.toUpperCase()}'
        : '';

    final user = ref.watch(authProvider);
    final bookmarksNotifier = ref.read(bookmarksProvider.notifier);
    final isBookmarked = verse != null
        ? ref.watch(bookmarksProvider).valueOrNull?.any(
                  (b) => b.matches(primaryBible, book, chapter, verse!),
                ) ??
            false
        : false;

    return Container(
      decoration: BoxDecoration(
        color: isDark ? kDarkSurface : Colors.white,
        border: Border(top: BorderSide(color: borderColor)),
      ),
      padding: EdgeInsets.fromLTRB(
        16,
        8,
        8,
        8 + MediaQuery.of(context).padding.bottom,
      ),
      child: Row(
        children: [
          Expanded(
            child: GestureDetector(
              onTap: onOpenPanel,
              child: Text(
                verseRef.isNotEmpty ? verseRef : 'BUILT FOR SEEKERS OF TRUTH',
                style: TextStyle(
                  fontSize: 11,
                  letterSpacing: 0.5,
                  fontWeight: FontWeight.w500,
                  color: verseRef.isNotEmpty
                      ? Theme.of(context).colorScheme.primary
                      : kMuted,
                ),
              ),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.menu_book_outlined, size: 16),
            onPressed: onOpenPanel,
            tooltip: 'Parallel translations',
            padding: const EdgeInsets.all(8),
            constraints: const BoxConstraints(),
          ),
          const SizedBox(width: 4),
          IconButton(
            icon: Icon(
              isBookmarked ? Icons.bookmark : Icons.bookmark_border,
              size: 16,
              color:
                  isBookmarked ? Theme.of(context).colorScheme.primary : null,
            ),
            tooltip: isBookmarked ? 'Remove bookmark' : 'Bookmark verse',
            padding: const EdgeInsets.all(8),
            constraints: const BoxConstraints(),
            onPressed: verse == null
                ? null
                : () async {
                    if (user == null) {
                      await Navigator.of(context).push(
                        MaterialPageRoute<void>(
                          builder: (_) => LoginPage(
                            onSuccess: () => Navigator.of(context).pop(),
                          ),
                        ),
                      );
                      return;
                    }
                    // Get verse text from the current chapter data
                    final chapterKey = (
                      bible: primaryBible,
                      book: book,
                      chapter: chapter,
                    );
                    final verses =
                        ref.read(chapterVersesProvider(chapterKey)).valueOrNull;
                    final verseText = verses
                            ?.firstWhere(
                              (v) => v.v == verse,
                              orElse: () => verses.first,
                            )
                            .t ??
                        '';
                    try {
                      await bookmarksNotifier.toggle(
                          primaryBible, book, chapter, verse!, verseText);
                    } on Exception catch (error) {
                      if (!isEmailUnverifiedError(error)) {
                        if (context.mounted) _bookmarkFailed(context);
                        return;
                      }
                      // The server is always the one asked, because the local
                      // flag goes stale: the link opens in a browser, so this
                      // window can be holding a "not confirmed" that stopped
                      // being true minutes ago. Re-read the account before
                      // blaming the address — that also keeps a 403 raised by
                      // something other than the gate from showing the wrong
                      // explanation.
                      await ref.read(authProvider.notifier).refresh();
                      if (!context.mounted) return;
                      final account = ref.read(authProvider);
                      if (account != null && !account.emailVerified) {
                        await showVerifyEmailDialog(context, ref,
                            email: account.email);
                      } else {
                        _bookmarkFailed(context);
                      }
                    }
                  },
          ),
          const SizedBox(width: 4),
          const IconButton(
            icon: Icon(Icons.share_outlined, size: 16),
            onPressed: null,
            tooltip: 'Share',
            padding: EdgeInsets.all(8),
            constraints: BoxConstraints(),
          ),
        ],
      ),
    );
  }
}

/// The one message for a bookmark that would not save for an ordinary reason
/// — offline, or a server error. A refused verification gets the dialog
/// instead, which explains itself.
void _bookmarkFailed(BuildContext context) {
  ScaffoldMessenger.of(context).showSnackBar(
    const SnackBar(content: Text('Could not save the bookmark.')),
  );
}
