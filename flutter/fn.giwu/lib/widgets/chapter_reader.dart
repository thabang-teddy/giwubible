import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/verse.dart';
import '../providers/chapter_provider.dart';
import '../providers/prefs_provider.dart';
import '../providers/tts_provider.dart';
import '../theme.dart';
import 'chapter_nav.dart';
import 'verse_list.dart';

/// Chapter heading, navigation pill and the scrollable verse body.
///
/// Tracks two independent highlights: the verse the reader tapped
/// (`activeVerseProvider`, drives the comparison panel) and the verse being
/// read aloud (`speakingVerseProvider`), which also auto-scrolls into view.
class ChapterReader extends ConsumerStatefulWidget {
  const ChapterReader({
    super.key,
    required this.bookName,
    required this.book,
    required this.chapter,
    required this.versesAsync,
    required this.onVerseTap,
  });

  final String bookName;
  final int book;
  final int chapter;
  final AsyncValue<List<VerseModel>> versesAsync;
  final void Function(int verse) onVerseTap;

  @override
  ConsumerState<ChapterReader> createState() => _ChapterReaderState();
}

class _ChapterReaderState extends ConsumerState<ChapterReader> {
  /// One stable key per verse number so the key never moves between widgets,
  /// which would trip the duplicate-GlobalKey assertion in a lazy list.
  final Map<int, GlobalKey> _verseKeys = {};

  @override
  void didUpdateWidget(ChapterReader old) {
    super.didUpdateWidget(old);
    if (old.book != widget.book || old.chapter != widget.chapter) {
      _verseKeys.clear();
    }
  }

  GlobalKey _keyFor(int verse) =>
      _verseKeys.putIfAbsent(verse, () => GlobalKey());

  void _scrollToSpeaking(int? verse) {
    if (verse == null) return;
    // Wait for the frame that paints the new highlight before measuring.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final ctx = _verseKeys[verse]?.currentContext;
      if (ctx == null) return;
      Scrollable.ensureVisible(
        ctx,
        alignment: 0.3,
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeOut,
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    ref.listen<int?>(
      speakingVerseProvider,
      (_, next) => _scrollToSpeaking(next),
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        ChapterNav(
          bookName: widget.bookName,
          book: widget.book,
          chapter: widget.chapter,
        ),
        Expanded(
          child: widget.versesAsync.when(
            loading: () => const VerseListSkeleton(),
            error: (_, __) => _ChapterError(
              onRetry: () => ref.invalidate(
                chapterVersesProvider(
                  (
                    bible: ref.read(primaryBibleProvider),
                    book: ref.read(selectedBookProvider),
                    chapter: ref.read(selectedChapterProvider),
                  ),
                ),
              ),
            ),
            data: _buildVerses,
          ),
        ),
      ],
    );
  }

  Widget _buildVerses(List<VerseModel> verses) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    // The viewport's default cache extent keeps several verses either side of
    // the visible range built, which is what lets auto-scroll resolve the next
    // verse's context as playback steps through the chapter.
    return CustomScrollView(
      slivers: [
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(24, 20, 24, 12),
            child: Text(
              '${widget.bookName} ${widget.chapter}',
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.w700,
                    color: isDark ? Colors.white : const Color(0xFF111827),
                  ),
            ),
          ),
        ),
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 96),
          sliver: SliverList(
            delegate: SliverChildBuilderDelegate(
              (context, i) => _VerseRow(
                key: _keyFor(verses[i].v),
                verse: verses[i],
                onTap: () => widget.onVerseTap(verses[i].v),
              ),
              childCount: verses.length,
            ),
          ),
        ),
      ],
    );
  }
}

// ── Single verse ───────────────────────────────────────────────────────────

class _VerseRow extends ConsumerWidget {
  const _VerseRow({
    super.key,
    required this.verse,
    required this.onTap,
  });

  final VerseModel verse;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cs = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    final isSelected = verse.v == ref.watch(activeVerseProvider);
    final isSpeaking = verse.v == ref.watch(speakingVerseProvider);

    // Being read aloud reads as a stronger fill plus an accent rule, so it
    // stays distinguishable from the tap-to-compare selection.
    final Color background;
    if (isSpeaking) {
      background = cs.primary.withOpacity(isDark ? 0.26 : 0.14);
    } else if (isSelected) {
      background = cs.primary.withOpacity(isDark ? 0.15 : 0.07);
    } else {
      background = Colors.transparent;
    }

    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        margin: const EdgeInsets.only(bottom: 2),
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
        decoration: BoxDecoration(
          color: background,
          borderRadius: BorderRadius.circular(4),
          border: isSpeaking
              ? Border(left: BorderSide(color: cs.primary, width: 3))
              : null,
        ),
        child: RichText(
          text: TextSpan(
            style: DefaultTextStyle.of(context).style.copyWith(
                  fontSize: 15,
                  height: 1.7,
                  color: isDark
                      ? const Color(0xFFE5E5E5)
                      : const Color(0xFF1F1F1F),
                ),
            children: [
              WidgetSpan(
                alignment: PlaceholderAlignment.top,
                child: Padding(
                  padding: const EdgeInsets.only(right: 3, top: 2),
                  child: Text(
                    '${verse.v}',
                    style: TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.w700,
                      color: cs.primary,
                      height: 1,
                    ),
                  ),
                ),
              ),
              TextSpan(text: verse.t),
            ],
          ),
        ),
      ),
    );
  }
}

// ── Error state ────────────────────────────────────────────────────────────

class _ChapterError extends StatelessWidget {
  const _ChapterError({required this.onRetry});

  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text('Failed to load chapter.', style: TextStyle(color: kMuted)),
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: onRetry,
            icon: const Icon(Icons.refresh, size: 16),
            label: const Text('Retry'),
          ),
        ],
      ),
    );
  }
}
