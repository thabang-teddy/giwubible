import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/bible.dart';
import '../models/book.dart';
import '../providers/bibles_provider.dart';
import '../providers/chapter_provider.dart';
import '../providers/prefs_provider.dart';
import '../theme.dart';
import '../widgets/app_panel.dart';
import '../widgets/app_sidebar.dart';
import '../widgets/book_drawer.dart';
import '../widgets/chapter_nav.dart';
import '../widgets/verse_list.dart';

class ReadPage extends ConsumerStatefulWidget {
  const ReadPage({super.key});

  @override
  ConsumerState<ReadPage> createState() => _ReadPageState();
}

class _ReadPageState extends ConsumerState<ReadPage> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  PersistentBottomSheetController? _sheetController;

  // ── Verse selection ──────────────────────────────────────────────────────

  void _selectVerse(int tapped) {
    final isDesktop =
        MediaQuery.of(context).size.width >= kDesktopBreakpoint;
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
          final isDark =
              Theme.of(ctx).brightness == Brightness.dark;
          return Container(
            decoration: BoxDecoration(
              color: isDark ? kDarkSurface : Colors.white,
              borderRadius:
                  const BorderRadius.vertical(top: Radius.circular(16)),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.12),
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

  void _reset() {
    ref.read(primaryBibleProvider.notifier).set('t_kjv');
    ref.read(selectedBookProvider.notifier).set(1);
    ref.read(selectedChapterProvider.notifier).set(1);
    _closeSheet();
  }

  // ── Build ────────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    final width = MediaQuery.of(context).size.width;
    final isDesktop = width >= kDesktopBreakpoint;

    final biblesAsync = ref.watch(biblesProvider);
    final booksAsync = ref.watch(booksProvider);
    final primaryBible = ref.watch(primaryBibleProvider);
    final book = ref.watch(selectedBookProvider);
    final chapter = ref.watch(selectedChapterProvider);
    final activeVerse = ref.watch(activeVerseProvider);
    final isDark = ref.watch(darkModeProvider);

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

    final appBar = _buildAppBar(
      context,
      isDesktop: isDesktop,
      bibles: bibles,
      primaryBible: primaryBible,
      isDark: isDark,
      currentBible: currentBible,
    );

    final mainContent = _MainContent(
      bookName: currentBook?.n ?? '',
      book: book,
      chapter: chapter,
      versesAsync: versesAsync,
      onVerseTap: _selectVerse,
    );

    final bottomBar = _BottomBar(
      bookName: currentBook?.n,
      chapter: chapter,
      verse: activeVerse,
      versionAbbr: currentBible?.abbreviation,
      onOpenPanel: activeVerse != null && !isDesktop ? _openSheet : null,
    );

    if (isDesktop) {
      // ── Desktop: 3-column layout ─────────────────────────────────────────
      return Scaffold(
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
      appBar: appBar,
      body: mainContent,
      bottomNavigationBar: bottomBar,
    );
  }

  // ── AppBar ───────────────────────────────────────────────────────────────

  PreferredSizeWidget _buildAppBar(
    BuildContext context, {
    required bool isDesktop,
    required List<BibleModel> bibles,
    required String primaryBible,
    required bool isDark,
    required BibleModel? currentBible,
  }) {
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
              onPressed: () =>
                  _scaffoldKey.currentState?.openDrawer(),
              tooltip: 'Book list',
            ),
      title: isDesktop ? null : const Text('Giwu Bible'),
      actions: [
        // Reset
        IconButton(
          icon: const Icon(Icons.refresh_rounded, size: 18),
          onPressed: _reset,
          tooltip: 'Reset to Genesis 1 (KJV)',
        ),
        // Dark mode
        IconButton(
          icon: Icon(
            isDark
                ? Icons.wb_sunny_outlined
                : Icons.dark_mode_outlined,
            size: 18,
          ),
          onPressed: () =>
              ref.read(darkModeProvider.notifier).toggle(),
          tooltip: isDark ? 'Light mode' : 'Dark mode',
        ),
        // Bible version picker
        if (bibles.isNotEmpty)
          _BibleButton(
            bibles: bibles,
            primaryBible: primaryBible,
            abbreviation: currentBible?.abbreviation ?? '…',
            onChanged: (table) {
              ref.read(primaryBibleProvider.notifier).set(table);
              ref.read(selectedBookProvider.notifier).set(1);
              ref.read(selectedChapterProvider.notifier).set(1);
              _closeSheet();
            },
          ),
        const SizedBox(width: 4),
      ],
    );
  }
}

// ── Bible version picker ───────────────────────────────────────────────────

class _BibleButton extends StatelessWidget {
  const _BibleButton({
    required this.bibles,
    required this.primaryBible,
    required this.abbreviation,
    required this.onChanged,
  });

  final List<BibleModel> bibles;
  final String primaryBible;
  final String abbreviation;
  final void Function(String) onChanged;

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final borderColor = isDark ? kDarkBorder2 : kDivider;

    return GestureDetector(
      onTap: () => _show(context),
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 10),
        padding: const EdgeInsets.symmetric(horizontal: 10),
        decoration: BoxDecoration(
          border: Border.all(color: borderColor),
          borderRadius: BorderRadius.circular(6),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              abbreviation,
              style: const TextStyle(
                  fontSize: 12, fontWeight: FontWeight.w600),
            ),
            const SizedBox(width: 2),
            Icon(Icons.keyboard_arrow_down, size: 14, color: kMuted),
          ],
        ),
      ),
    );
  }

  void _show(BuildContext context) {
    showModalBottomSheet<void>(
      context: context,
      builder: (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
              child: Text(
                'Select Bible Version',
                style: Theme.of(ctx).textTheme.titleMedium,
              ),
            ),
            Flexible(
              child: ListView(
                shrinkWrap: true,
                children: bibles.map((b) {
                  final isSelected = b.table == primaryBible;
                  final isDownloaded = b.downloaded;
                  final cs = Theme.of(ctx).colorScheme;
                  return ListTile(
                    dense: true,
                    enabled: isDownloaded,
                    leading: Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: isDownloaded
                            ? cs.primaryContainer
                            : cs.surfaceContainerHigh,
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Text(
                        b.abbreviation,
                        style: TextStyle(
                          fontSize: 10,
                          fontWeight: FontWeight.w700,
                          color: isDownloaded
                              ? cs.onPrimaryContainer
                              : kMuted,
                        ),
                      ),
                    ),
                    title: Text(b.version,
                        style: const TextStyle(fontSize: 13)),
                    trailing: isSelected
                        ? Icon(Icons.check, color: cs.primary, size: 16)
                        : !isDownloaded
                            ? Icon(Icons.download_outlined,
                                size: 14, color: kMuted)
                            : null,
                    selected: isSelected,
                    onTap: isDownloaded
                        ? () {
                            Navigator.of(ctx).pop();
                            onChanged(b.table);
                          }
                        : null,
                  );
                }).toList(),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Main content (chapter nav + verse list) ────────────────────────────────

class _MainContent extends ConsumerWidget {
  const _MainContent({
    required this.bookName,
    required this.book,
    required this.chapter,
    required this.versesAsync,
    required this.onVerseTap,
  });

  final String bookName;
  final int book;
  final int chapter;
  final AsyncValue<dynamic> versesAsync;
  final void Function(int) onVerseTap;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cs = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        ChapterNav(bookName: bookName, book: book, chapter: chapter),
        Expanded(
          child: versesAsync.when(
            loading: () => const VerseListSkeleton(),
            error: (_, __) => Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    'Failed to load chapter.',
                    style: TextStyle(color: kMuted),
                  ),
                  const SizedBox(height: 12),
                  OutlinedButton.icon(
                    onPressed: () => ref.invalidate(
                      chapterVersesProvider(
                        (
                          bible: ref.read(primaryBibleProvider),
                          book: ref.read(selectedBookProvider),
                          chapter: ref.read(selectedChapterProvider),
                        ),
                      ),
                    ),
                    icon: const Icon(Icons.refresh, size: 16),
                    label: const Text('Retry'),
                  ),
                ],
              ),
            ),
            data: (verses) => CustomScrollView(
              slivers: [
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(24, 20, 24, 12),
                    child: Text(
                      '$bookName $chapter',
                      style: Theme.of(context)
                          .textTheme
                          .headlineSmall
                          ?.copyWith(
                            fontWeight: FontWeight.w700,
                            color: isDark
                                ? Colors.white
                                : const Color(0xFF111827),
                          ),
                    ),
                  ),
                ),
                SliverPadding(
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 80),
                  sliver: SliverList(
                    delegate: SliverChildBuilderDelegate(
                      (context, i) {
                        final v = verses[i];
                        final activeVerse =
                            ref.watch(activeVerseProvider);
                        final isActive = v.v == activeVerse;
                        return GestureDetector(
                          onTap: () => onVerseTap(v.v),
                          child: Container(
                            margin: const EdgeInsets.only(bottom: 2),
                            padding: const EdgeInsets.symmetric(
                                horizontal: 8, vertical: 6),
                            decoration: BoxDecoration(
                              color: isActive
                                  ? cs.primary.withOpacity(
                                      isDark ? 0.15 : 0.07)
                                  : Colors.transparent,
                              borderRadius:
                                  BorderRadius.circular(4),
                            ),
                            child: RichText(
                              text: TextSpan(
                                style: DefaultTextStyle.of(context)
                                    .style
                                    .copyWith(
                                      fontSize: 15,
                                      height: 1.7,
                                      color: isDark
                                          ? const Color(0xFFE5E5E5)
                                          : const Color(0xFF1F1F1F),
                                    ),
                                children: [
                                  WidgetSpan(
                                    alignment:
                                        PlaceholderAlignment.top,
                                    child: Padding(
                                      padding: const EdgeInsets.only(
                                          right: 3, top: 2),
                                      child: Text(
                                        '${v.v}',
                                        style: TextStyle(
                                          fontSize: 10,
                                          fontWeight: FontWeight.w700,
                                          color: cs.primary,
                                          height: 1,
                                        ),
                                      ),
                                    ),
                                  ),
                                  TextSpan(text: v.t),
                                ],
                              ),
                            ),
                          ),
                        );
                      },
                      childCount: verses.length,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

// ── Bottom bar ─────────────────────────────────────────────────────────────

class _BottomBar extends StatelessWidget {
  const _BottomBar({
    required this.bookName,
    required this.chapter,
    required this.verse,
    required this.versionAbbr,
    required this.onOpenPanel,
  });

  final String? bookName;
  final int chapter;
  final int? verse;
  final String? versionAbbr;
  final VoidCallback? onOpenPanel;

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final borderColor =
        isDark ? kDarkBorder : kDivider;
    final ref =
        verse != null && bookName != null && versionAbbr != null
            ? '${bookName!.toUpperCase()} $chapter:$verse  •  ${versionAbbr!.toUpperCase()}'
            : '';

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
          // Verse ref (tappable on mobile to open sheet)
          Expanded(
            child: GestureDetector(
              onTap: onOpenPanel,
              child: Text(
                ref.isNotEmpty ? ref : 'BUILT FOR SEEKERS OF TRUTH',
                style: TextStyle(
                  fontSize: 11,
                  letterSpacing: 0.5,
                  fontWeight: FontWeight.w500,
                  color: ref.isNotEmpty
                      ? Theme.of(context).colorScheme.primary
                      : kMuted,
                ),
              ),
            ),
          ),
          // Icons
          IconButton(
            icon: const Icon(Icons.menu_book_outlined, size: 16),
            onPressed: onOpenPanel,
            tooltip: 'Parallel translations',
            padding: const EdgeInsets.all(8),
            constraints: const BoxConstraints(),
          ),
          const SizedBox(width: 4),
          IconButton(
            icon: const Icon(Icons.bookmark_border, size: 16),
            onPressed: null,
            tooltip: 'Bookmark',
            padding: const EdgeInsets.all(8),
            constraints: const BoxConstraints(),
          ),
          const SizedBox(width: 4),
          IconButton(
            icon: const Icon(Icons.share_outlined, size: 16),
            onPressed: null,
            tooltip: 'Share',
            padding: const EdgeInsets.all(8),
            constraints: const BoxConstraints(),
          ),
        ],
      ),
    );
  }
}
