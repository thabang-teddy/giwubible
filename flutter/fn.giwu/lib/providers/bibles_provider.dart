import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../api/bibles.dart' as api;
import '../api/books.dart' as api;
import '../data/bible_versions.dart';
import '../data/book_names.dart';
import '../models/bible.dart';
import '../models/book.dart';
import 'database_provider.dart';

/// All bibles stored locally (includes the downloaded flag).
/// Falls back to the API, then to the bundled [defaultBibles] constant so
/// the provider never fails even when fully offline.
final biblesProvider = FutureProvider<List<BibleModel>>((ref) async {
  final db = ref.watch(bibleDatabaseProvider);
  try {
    final local = await db.getBibles();
    if (local.isNotEmpty) return local;
  } catch (_) {}
  try {
    return await api.getBibles();
  } catch (_) {}
  // Ultimate offline fallback — always available.
  return defaultBibles;
});

/// Books list — local DB first, API second, bundled constant as last resort.
/// This never fails: even fully offline with an empty DB the 66 books are
/// available immediately from [kBibleBooks].
final booksProvider = FutureProvider<List<BookModel>>((ref) async {
  final db = ref.watch(bibleDatabaseProvider);

  try {
    final local = await db.getBooks();
    if (local.isNotEmpty) return local;
  } catch (_) {}

  try {
    final remote = await api.getBooks();
    // Persist for next launch.
    try {
      await db.saveBooks(remote);
    } catch (_) {}
    return remote;
  } catch (_) {}

  // Ultimate offline fallback — always available.
  return defaultBooks;
});
