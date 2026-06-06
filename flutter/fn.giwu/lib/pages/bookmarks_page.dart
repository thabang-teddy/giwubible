import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/bookmark.dart';
import '../providers/auth_provider.dart';
import '../providers/bookmarks_provider.dart';
import '../providers/prefs_provider.dart';
import '../theme.dart';
import 'login_page.dart';

class BookmarksPage extends ConsumerWidget {
  const BookmarksPage({super.key});

  static const _bookAbbr = [
    'Gen','Exo','Lev','Num','Deu','Jos','Jdg','Rut','1Sa','2Sa','1Ki','2Ki',
    '1Ch','2Ch','Ezr','Neh','Est','Job','Psa','Pro','Ecc','SoS','Isa','Jer',
    'Lam','Eze','Dan','Hos','Joe','Amo','Oba','Jon','Mic','Nah','Hab','Zep',
    'Hag','Zec','Mal','Mat','Mar','Luk','Joh','Act','Rom','1Co','2Co','Gal',
    'Eph','Phi','Col','1Th','2Th','1Ti','2Ti','Tit','Phm','Heb','Jam','1Pe',
    '2Pe','1Jo','2Jo','3Jo','Jud','Rev',
  ];

  String _bookName(int b) =>
      b >= 1 && b <= _bookAbbr.length ? _bookAbbr[b - 1] : 'Book $b';

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authProvider);
    final bookmarksAsync = ref.watch(bookmarksProvider);

    if (user == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Bookmarks')),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.bookmark_border, size: 48, color: kMuted),
              const SizedBox(height: 16),
              const Text('Sign in to save bookmarks',
                  style: TextStyle(color: kMuted)),
              const SizedBox(height: 20),
              FilledButton(
                onPressed: () => Navigator.of(context).push(
                  MaterialPageRoute<void>(builder: (_) => LoginPage(
                    onSuccess: () => Navigator.of(context).pop(),
                  )),
                ),
                child: const Text('Sign in'),
              ),
            ],
          ),
        ),
      );
    }

    final bookmarks = bookmarksAsync.valueOrNull ?? [];

    return Scaffold(
      appBar: AppBar(
        title: const Text('Bookmarks'),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout_outlined, size: 18),
            tooltip: 'Sign out',
            onPressed: () async {
              await ref.read(authProvider.notifier).logout();
              if (context.mounted) Navigator.of(context).pop();
            },
          ),
        ],
      ),
      body: bookmarksAsync.isLoading
          ? const Center(child: CircularProgressIndicator())
          : bookmarks.isEmpty
              ? Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.bookmark_border, size: 48, color: kMuted),
                      const SizedBox(height: 16),
                      const Text('No bookmarks yet',
                          style: TextStyle(color: kMuted)),
                      const SizedBox(height: 4),
                      const Text(
                        'Long-press a verse while reading to bookmark it.',
                        style: TextStyle(fontSize: 13, color: kMuted),
                        textAlign: TextAlign.center,
                      ),
                    ],
                  ),
                )
              : ListView.separated(
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  itemCount: bookmarks.length,
                  separatorBuilder: (_, __) =>
                      const Divider(height: 1, indent: 16, endIndent: 16),
                  itemBuilder: (context, i) =>
                      _BookmarkTile(bookmark: bookmarks[i], bookName: _bookName(bookmarks[i].book)),
                ),
    );
  }
}

class _BookmarkTile extends ConsumerWidget {
  const _BookmarkTile({required this.bookmark, required this.bookName});

  final BookmarkModel bookmark;
  final String bookName;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cs = Theme.of(context).colorScheme;

    return ListTile(
      onTap: () {
        ref.read(selectedBookProvider.notifier).set(bookmark.book);
        ref.read(selectedChapterProvider.notifier).set(bookmark.chapter);
        ref.read(primaryBibleProvider.notifier).set(bookmark.bible);
        ref.read(activeVerseProvider.notifier).state = bookmark.verse;
        Navigator.of(context).pop();
      },
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      title: Row(
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
            decoration: BoxDecoration(
              color: cs.primaryContainer,
              borderRadius: BorderRadius.circular(4),
            ),
            child: Text(
              '$bookName ${bookmark.chapter}:${bookmark.verse}',
              style: TextStyle(
                fontSize: 10,
                fontWeight: FontWeight.w700,
                color: cs.onPrimaryContainer,
                letterSpacing: 0.5,
              ),
            ),
          ),
          const SizedBox(width: 8),
          Text(
            bookmark.bible.replaceFirst('t_', '').toUpperCase(),
            style: TextStyle(fontSize: 10, color: kMuted, fontWeight: FontWeight.w600),
          ),
        ],
      ),
      subtitle: Padding(
        padding: const EdgeInsets.only(top: 4),
        child: Text(
          bookmark.text,
          style: const TextStyle(fontSize: 14, height: 1.5),
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
        ),
      ),
      trailing: IconButton(
        icon: const Icon(Icons.close, size: 16),
        color: kMuted,
        onPressed: () => ref
            .read(bookmarksProvider.notifier)
            .toggle(bookmark.bible, bookmark.book, bookmark.chapter,
                bookmark.verse, bookmark.text),
      ),
    );
  }
}
