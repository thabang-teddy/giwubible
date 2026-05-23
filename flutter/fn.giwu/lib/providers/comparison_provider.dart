import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/verse.dart';
import '../models/verse.dart';

typedef VerseKey = ({String bible, int book, int chapter, int verse});

final verseComparisonProvider =
    FutureProvider.family<ComparisonResult?, VerseKey>(
  (ref, key) => getVerse(key.bible, key.book, key.chapter, key.verse),
);
