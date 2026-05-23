import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/chapter.dart';
import '../models/verse.dart';

typedef ChapterKey = ({String bible, int book, int chapter});

final chapterVersesProvider =
    FutureProvider.family<List<VerseModel>, ChapterKey>(
  (ref, key) => getChapter(key.bible, key.book, key.chapter),
);
