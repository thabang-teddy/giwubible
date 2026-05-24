import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../data/chapter_counts.dart';
import '../providers/prefs_provider.dart';
import '../theme.dart';

class ChapterNav extends ConsumerWidget {
  const ChapterNav({
    super.key,
    required this.bookName,
    required this.book,
    required this.chapter,
  });

  final String bookName;
  final int book;
  final int chapter;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final max = maxChaptersForBook(book);
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final borderColor =
        isDark ? kDarkBorder : kDivider;

    void change(int c) {
      ref.read(selectedChapterProvider.notifier).set(c);
      ref.read(activeVerseProvider.notifier).state = null;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      child: Center(
        child: Container(
          decoration: BoxDecoration(
            border: Border.all(color: borderColor),
            borderRadius: BorderRadius.circular(20),
          ),
          child: IntrinsicHeight(
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                // Prev
                _PillButton(
                  icon: Icons.chevron_left,
                  onPressed:
                      chapter <= 1 ? null : () => change(chapter - 1),
                  tooltip: 'Previous chapter',
                  borderColor: borderColor,
                  side: _Side.left,
                ),
                // Vertical divider
                VerticalDivider(width: 1, color: borderColor),
                // Chapter label + picker
                InkWell(
                  onTap: () =>
                      _showPicker(context, max, chapter, change),
                  borderRadius: const BorderRadius.all(Radius.zero),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 14, vertical: 7),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          '$bookName $chapter',
                          style: const TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                        const SizedBox(width: 3),
                        Icon(Icons.keyboard_arrow_down,
                            size: 15, color: kMuted),
                      ],
                    ),
                  ),
                ),
                // Vertical divider
                VerticalDivider(width: 1, color: borderColor),
                // Next
                _PillButton(
                  icon: Icons.chevron_right,
                  onPressed:
                      chapter >= max ? null : () => change(chapter + 1),
                  tooltip: 'Next chapter',
                  borderColor: borderColor,
                  side: _Side.right,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

enum _Side { left, right }

class _PillButton extends StatelessWidget {
  const _PillButton({
    required this.icon,
    required this.onPressed,
    required this.tooltip,
    required this.borderColor,
    required this.side,
  });

  final IconData icon;
  final VoidCallback? onPressed;
  final String tooltip;
  final Color borderColor;
  final _Side side;

  @override
  Widget build(BuildContext context) {
    final radius = side == _Side.left
        ? const BorderRadius.horizontal(left: Radius.circular(20))
        : const BorderRadius.horizontal(right: Radius.circular(20));

    return InkWell(
      onTap: onPressed,
      borderRadius: radius,
      child: Tooltip(
        message: tooltip,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
          child: Icon(
            icon,
            size: 16,
            color: onPressed == null ? kMuted.withOpacity(0.4) : null,
          ),
        ),
      ),
    );
  }
}

void _showPicker(
  BuildContext context,
  int max,
  int current,
  void Function(int) onSelect,
) {
  showModalBottomSheet<void>(
    context: context,
    builder: (ctx) => SafeArea(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
            child: Text(
              'Select Chapter',
              style: Theme.of(ctx).textTheme.titleMedium,
            ),
          ),
          Flexible(
            child: GridView.builder(
              shrinkWrap: true,
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
              gridDelegate:
                  const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 6,
                mainAxisSpacing: 6,
                crossAxisSpacing: 6,
                childAspectRatio: 1,
              ),
              itemCount: max,
              itemBuilder: (_, i) {
                final n = i + 1;
                final isActive = n == current;
                final cs = Theme.of(ctx).colorScheme;
                return InkWell(
                  borderRadius: BorderRadius.circular(6),
                  onTap: () {
                    Navigator.of(ctx).pop();
                    onSelect(n);
                  },
                  child: Container(
                    decoration: BoxDecoration(
                      color: isActive
                          ? cs.primary
                          : cs.surfaceContainerHigh,
                      borderRadius: BorderRadius.circular(6),
                    ),
                    alignment: Alignment.center,
                    child: Text(
                      '$n',
                      style: TextStyle(
                        fontSize: 13,
                        color: isActive ? cs.onPrimary : cs.onSurface,
                        fontWeight:
                            isActive ? FontWeight.w600 : FontWeight.normal,
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    ),
  );
}
