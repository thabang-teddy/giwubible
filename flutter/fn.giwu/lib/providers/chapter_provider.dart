import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../api/chapter.dart' as api;
import '../models/verse.dart';
import 'database_provider.dart';

typedef ChapterKey = ({String bible, int book, int chapter});

final chapterVersesProvider =
    FutureProvider.family<List<VerseModel>, ChapterKey>((ref, key) async {
  final db = ref.watch(bibleDatabaseProvider);

  // Always try local first — avoids an API round-trip when the data is already
  // on-device and prevents false negatives when the downloaded flag lags behind
  // reality (e.g. immediately after a sync).
  try {
    final local = await db.getChapter(key.bible, key.book, key.chapter);
    if (local.isNotEmpty) return local;
  } catch (_) {}

  // Local had nothing — fall back to the API.
  return api.getChapter(key.bible, key.book, key.chapter);
});
