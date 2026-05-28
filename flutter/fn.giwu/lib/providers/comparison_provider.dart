import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../api/verse.dart' as api;
import '../models/verse.dart';
import 'database_provider.dart';

typedef VerseKey = ({String bible, int book, int chapter, int verse});

final verseComparisonProvider =
    FutureProvider.family<ComparisonResult?, VerseKey>((ref, key) async {
  final db = ref.watch(bibleDatabaseProvider);

  // Always try local first — same rationale as chapterVersesProvider.
  try {
    final local =
        await db.getVerse(key.bible, key.book, key.chapter, key.verse);
    if (local != null) return local;
  } catch (_) {}

  // Local had nothing — fall back to the API.
  // Return null (not throw) when the API also fails so callers can render a
  // graceful "not available" state instead of the error branch.
  try {
    return await api.getVerse(key.bible, key.book, key.chapter, key.verse);
  } catch (_) {
    return null;
  }
});
