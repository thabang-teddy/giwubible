import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../api/bookmarks.dart' as bm_api;
import '../models/bookmark.dart';
import 'auth_provider.dart';

class BookmarksNotifier extends StateNotifier<AsyncValue<List<BookmarkModel>>> {
  BookmarksNotifier(this._ref) : super(const AsyncValue.data([])) {
    _ref.listen(authProvider, (prev, next) {
      if (next != null) {
        _load();
      } else {
        state = const AsyncValue.data([]);
      }
    }, fireImmediately: true);
  }

  final Ref _ref;

  Future<void> _load() async {
    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() => bm_api.listBookmarks());
  }

  bool isBookmarked(String bible, int book, int chapter, int verse) {
    final list = state.valueOrNull ?? [];
    return list.any((b) => b.matches(bible, book, chapter, verse));
  }

  BookmarkModel? getBookmark(String bible, int book, int chapter, int verse) {
    final list = state.valueOrNull ?? [];
    try {
      return list.firstWhere((b) => b.matches(bible, book, chapter, verse));
    } on StateError {
      return null;
    }
  }

  Future<void> toggle(
      String bible, int book, int chapter, int verse, String text) async {
    final existing = getBookmark(bible, book, chapter, verse);
    if (existing != null) {
      final current = state.valueOrNull ?? [];
      state = AsyncValue.data(current.where((b) => b.id != existing.id).toList());
      await bm_api.deleteBookmark(existing.id);
    } else {
      final created = await bm_api.saveBookmark(
        bible: bible,
        book: book,
        chapter: chapter,
        verse: verse,
        text: text,
      );
      final current = state.valueOrNull ?? [];
      state = AsyncValue.data([...current, created]);
    }
  }
}

final bookmarksProvider =
    StateNotifierProvider<BookmarksNotifier, AsyncValue<List<BookmarkModel>>>(
  (ref) => BookmarksNotifier(ref),
);
