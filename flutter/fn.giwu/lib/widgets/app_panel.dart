import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/bible.dart';
import '../providers/comparison_provider.dart';
import '../providers/prefs_provider.dart';
import '../theme.dart';

/// Reusable comparison panel widget.
/// - Desktop: rendered inline (isSheet = false, onClose = null).
/// - Mobile: wrapped in a bottom sheet (isSheet = true, onClose closes sheet).
class AppPanel extends ConsumerStatefulWidget {
  const AppPanel({
    super.key,
    required this.bibles,
    required this.primaryBible,
    required this.book,
    required this.chapter,
    this.onClose,
    this.isSheet = false,
  });

  final List<BibleModel> bibles;
  final String primaryBible;
  final int book;
  final int chapter;
  final VoidCallback? onClose;
  final bool isSheet;

  @override
  ConsumerState<AppPanel> createState() => _AppPanelState();
}

class _AppPanelState extends ConsumerState<AppPanel>
    with SingleTickerProviderStateMixin {
  late final TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final activeVerse = ref.watch(activeVerseProvider);
    final cs = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final bg = isDark ? kDarkSurface : Colors.white;

    final comparableBibles =
        widget.bibles.where((b) => b.table != widget.primaryBible).toList();
    final selectedTables = ref.watch(parallelBiblesProvider);
    final activeBibles = selectedTables.isEmpty
        ? comparableBibles
        : comparableBibles
            .where((b) => selectedTables.contains(b.table))
            .toList();

    return Container(
      color: bg,
      child: Column(
        children: [
          // Drag handle for sheet mode
          if (widget.isSheet)
            Center(
              child: Container(
                margin: const EdgeInsets.only(top: 10, bottom: 4),
                width: 36,
                height: 4,
                decoration: BoxDecoration(
                  color: kDivider,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
          // Tab bar
          Row(
            children: [
              Expanded(
                child: TabBar(
                  controller: _tabController,
                  labelStyle: const TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 0.5,
                  ),
                  unselectedLabelStyle: const TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w500,
                    letterSpacing: 0.5,
                  ),
                  tabs: const [
                    Tab(text: 'PARALLEL VERSES'),
                    Tab(text: 'PARALLEL BIBLES'),
                  ],
                ),
              ),
              if (widget.onClose != null)
                Padding(
                  padding: const EdgeInsets.only(right: 4),
                  child: IconButton(
                    icon: Icon(Icons.close, size: 16, color: kMuted),
                    onPressed: widget.onClose,
                    tooltip: 'Close',
                    padding: const EdgeInsets.all(8),
                    constraints: const BoxConstraints(),
                  ),
                ),
            ],
          ),
          // Tab content
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [
                _ParallelVersesTab(
                  bibles: activeBibles,
                  book: widget.book,
                  chapter: widget.chapter,
                  verse: activeVerse,
                ),
                _ParallelBiblesTab(comparableBibles: comparableBibles),
              ],
            ),
          ),
          if (widget.isSheet)
            SizedBox(height: MediaQuery.of(context).padding.bottom),
        ],
      ),
    );
  }
}

// ── Parallel Verses tab ────────────────────────────────────────────────────

class _ParallelVersesTab extends ConsumerWidget {
  const _ParallelVersesTab({
    required this.bibles,
    required this.book,
    required this.chapter,
    required this.verse,
  });

  final List<BibleModel> bibles;
  final int book;
  final int chapter;
  final int? verse;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cs = Theme.of(context).colorScheme;

    if (verse == null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(
            'Click any verse to see parallel translations.',
            style: TextStyle(color: kMuted, fontSize: 13),
            textAlign: TextAlign.center,
          ),
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
      itemCount: bibles.length,
      itemBuilder: (context, i) {
        final bible = bibles[i];
        final key = (
          bible: bible.table,
          book: book,
          chapter: chapter,
          verse: verse!,
        );
        return ref.watch(verseComparisonProvider(key)).when(
              loading: () => _CardSkeleton(cs: cs),
              error: (_, __) => const SizedBox.shrink(),
              data: (result) {
                if (result == null) return const SizedBox.shrink();
                return _VersionCard(
                  version: result.version,
                  abbreviation: result.abbreviation,
                  text: result.text,
                  cs: cs,
                );
              },
            );
      },
    );
  }
}

class _VersionCard extends StatelessWidget {
  const _VersionCard({
    required this.version,
    required this.abbreviation,
    required this.text,
    required this.cs,
  });

  final String version;
  final String abbreviation;
  final String? text;
  final ColorScheme cs;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: cs.surfaceContainerLow,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: kDivider, width: 0.5),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  version,
                  style: const TextStyle(
                      fontWeight: FontWeight.w600, fontSize: 13),
                ),
              ),
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(
                  color: cs.primaryContainer,
                  borderRadius: BorderRadius.circular(4),
                ),
                child: Text(
                  abbreviation,
                  style: TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.w700,
                    color: cs.onPrimaryContainer,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          text != null
              ? Text(text!,
                  style: const TextStyle(fontSize: 13, height: 1.55))
              : Text('• • •',
                  style: TextStyle(color: kMuted, fontSize: 14)),
        ],
      ),
    );
  }
}

class _CardSkeleton extends StatelessWidget {
  const _CardSkeleton({required this.cs});
  final ColorScheme cs;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: cs.surfaceContainerLow,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
              height: 11, width: 90, color: cs.surfaceContainerHigh),
          const SizedBox(height: 8),
          Container(
              height: 13,
              width: double.infinity,
              color: cs.surfaceContainerHigh),
        ],
      ),
    );
  }
}

// ── Parallel Bibles tab ────────────────────────────────────────────────────

class _ParallelBiblesTab extends ConsumerWidget {
  const _ParallelBiblesTab({required this.comparableBibles});
  final List<BibleModel> comparableBibles;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selectedTables = ref.watch(parallelBiblesProvider);
    final cs = Theme.of(context).colorScheme;
    final allTables = comparableBibles.map((b) => b.table).toList();

    return ListView(
      padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
      children: [
        Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: Text(
            'Select bibles to compare',
            style: TextStyle(fontSize: 12, color: kMuted),
          ),
        ),
        ...comparableBibles.map((b) {
          final isChecked =
              selectedTables.isEmpty || selectedTables.contains(b.table);
          return CheckboxListTile(
            dense: true,
            value: isChecked,
            onChanged: (_) => ref
                .read(parallelBiblesProvider.notifier)
                .toggle(b.table, allTables),
            title: Text(b.version, style: const TextStyle(fontSize: 13)),
            secondary: Container(
              padding:
                  const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
              decoration: BoxDecoration(
                color: cs.primaryContainer,
                borderRadius: BorderRadius.circular(4),
              ),
              child: Text(
                b.abbreviation,
                style: TextStyle(
                  fontSize: 10,
                  fontWeight: FontWeight.w700,
                  color: cs.onPrimaryContainer,
                ),
              ),
            ),
            controlAffinity: ListTileControlAffinity.leading,
          );
        }),
      ],
    );
  }
}
