import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/book.dart';
import '../providers/prefs_provider.dart';
import '../theme.dart';

/// Reusable sidebar widget.
/// - Desktop: rendered inline in a Row (onClose = null, afterSelect = null).
/// - Mobile: wrapped in a Drawer (onClose closes drawer, afterSelect closes drawer).
class AppSidebar extends ConsumerStatefulWidget {
  const AppSidebar({
    super.key,
    required this.books,
    this.isLoading = false,
    this.isError = false,
    this.errorMessage,
    this.onRetry,
    this.onClose,
    this.afterSelect,
  });

  final List<BookModel> books;
  final bool isLoading;
  final bool isError;
  final String? errorMessage;
  final VoidCallback? onRetry;
  final VoidCallback? onClose;
  final VoidCallback? afterSelect;

  @override
  ConsumerState<AppSidebar> createState() => _AppSidebarState();
}

class _AppSidebarState extends ConsumerState<AppSidebar> {
  final _controller = TextEditingController();
  String _query = '';

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final selectedBook = ref.watch(selectedBookProvider);
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final bg = isDark ? kDarkSurface : kSidebarBg;

    final filtered = widget.books
        .where((b) => b.n.toLowerCase().contains(_query.toLowerCase()))
        .toList();

    return Container(
      color: bg,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Header
          Padding(
            padding: const EdgeInsets.fromLTRB(14, 14, 8, 6),
            child: Row(
              children: [
                Text(
                  'BIBLE BOOKS',
                  style: TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 1.0,
                    color: kMuted,
                  ),
                ),
                const Spacer(),
                if (widget.onClose != null)
                  GestureDetector(
                    onTap: widget.onClose,
                    child: Icon(Icons.close, size: 16, color: kMuted),
                  ),
              ],
            ),
          ),
          // Search
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            child: TextField(
              controller: _controller,
              decoration: InputDecoration(
                hintText: 'Search books...',
                hintStyle: TextStyle(fontSize: 12, color: kMuted),
                prefixIcon: Icon(Icons.search, size: 15, color: kMuted),
                isDense: true,
                filled: true,
                fillColor: isDark ? kDarkBg : Colors.white,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(6),
                  borderSide: const BorderSide(color: kDivider),
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(6),
                  borderSide: const BorderSide(color: kDivider),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(6),
                  borderSide: BorderSide(
                    color: Theme.of(context).colorScheme.primary,
                  ),
                ),
                contentPadding: const EdgeInsets.symmetric(vertical: 6),
              ),
              style: const TextStyle(fontSize: 12),
              onChanged: (v) => setState(() => _query = v),
            ),
          ),
          const SizedBox(height: 2),
          // Book list
          if (widget.isLoading)
            const Expanded(child: Center(child: CircularProgressIndicator()))
          else if (widget.isError)
            Expanded(
              child: Center(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.wifi_off_rounded, color: kMuted, size: 28),
                      const SizedBox(height: 8),
                      Text(
                        'Failed to load books',
                        style: TextStyle(fontSize: 12, color: kMuted),
                        textAlign: TextAlign.center,
                      ),
                      if (widget.errorMessage != null) ...[
                        const SizedBox(height: 4),
                        Text(
                          widget.errorMessage!,
                          style: TextStyle(fontSize: 10, color: kMuted),
                          textAlign: TextAlign.center,
                        ),
                      ],
                      if (widget.onRetry != null) ...[
                        const SizedBox(height: 10),
                        TextButton(
                          onPressed: widget.onRetry,
                          child: const Text('Retry', style: TextStyle(fontSize: 12)),
                        ),
                      ],
                    ],
                  ),
                ),
              ),
            )
          else if (widget.books.isEmpty)
            Expanded(
              child: Center(
                child: Text(
                  'No books — check API',
                  style: TextStyle(fontSize: 12, color: kMuted),
                ),
              ),
            )
          else
            Expanded(
              child: ListView.builder(
                padding: const EdgeInsets.only(bottom: 16),
                itemCount: filtered.length,
                itemBuilder: (context, i) {
                  final book = filtered[i];
                  final isActive = book.b == selectedBook;
                  return _BookItem(
                    book: book,
                    isActive: isActive,
                    onTap: () {
                      ref.read(selectedBookProvider.notifier).set(book.b);
                      ref.read(selectedChapterProvider.notifier).set(1);
                      ref.read(activeVerseProvider.notifier).state = null;
                      widget.afterSelect?.call();
                    },
                  );
                },
              ),
            ),
        ],
      ),
    );
  }
}

class _BookItem extends StatelessWidget {
  const _BookItem({
    required this.book,
    required this.isActive,
    required this.onTap,
  });

  final BookModel book;
  final bool isActive;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 9),
        decoration: BoxDecoration(
          color: isActive ? cs.primary.withOpacity(0.08) : Colors.transparent,
          border: isActive
              ? Border(left: BorderSide(color: cs.primary, width: 2))
              : const Border(left: BorderSide(color: Colors.transparent, width: 2)),
        ),
        child: Row(
          children: [
            Expanded(
              child: Text(
                book.n,
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: isActive ? FontWeight.w600 : FontWeight.normal,
                  color: isActive ? cs.primary : null,
                ),
              ),
            ),
            if (isActive)
              Icon(Icons.chevron_right, size: 14, color: cs.primary),
          ],
        ),
      ),
    );
  }
}
