import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/verse.dart';
import '../providers/prefs_provider.dart';
import '../theme.dart';

class VerseList extends ConsumerWidget {
  const VerseList({
    super.key,
    required this.verses,
    required this.onVerseTap,
  });

  final List<VerseModel> verses;
  final void Function(int verse) onVerseTap;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final activeVerse = ref.watch(activeVerseProvider);
    final cs = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return ListView.builder(
      padding: const EdgeInsets.fromLTRB(24, 8, 24, 120),
      itemCount: verses.length,
      itemBuilder: (context, i) {
        final v = verses[i];
        final isActive = v.v == activeVerse;

        return GestureDetector(
          onTap: () => onVerseTap(v.v),
          child: Container(
            margin: const EdgeInsets.only(bottom: 2),
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
            decoration: BoxDecoration(
              color: isActive
                  ? cs.primary.withOpacity(isDark ? 0.15 : 0.07)
                  : Colors.transparent,
              borderRadius: BorderRadius.circular(4),
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
                  // Superscript-style verse number
                  WidgetSpan(
                    alignment: PlaceholderAlignment.top,
                    child: Padding(
                      padding: const EdgeInsets.only(right: 3, top: 2),
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
    );
  }
}

class VerseListSkeleton extends StatelessWidget {
  const VerseListSkeleton({super.key});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final widths = [0.85, 0.70, 0.90, 0.65, 0.80, 0.75, 0.88, 0.60, 0.82, 0.72];
    return ListView.builder(
      padding: const EdgeInsets.fromLTRB(24, 16, 24, 24),
      itemCount: widths.length,
      itemBuilder: (_, i) => Container(
        margin: const EdgeInsets.only(bottom: 12),
        height: 17,
        width: double.infinity,
        child: FractionallySizedBox(
          alignment: Alignment.centerLeft,
          widthFactor: widths[i],
          child: Container(
            decoration: BoxDecoration(
              color: cs.surfaceContainerHigh,
              borderRadius: BorderRadius.circular(3),
            ),
          ),
        ),
      ),
    );
  }
}
