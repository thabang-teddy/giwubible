import 'dart:convert';

import '../models/verse.dart';
import 'client.dart';

Future<ComparisonResult?> getVerse(
  String bible,
  int book,
  int chapter,
  int verse,
) async {
  final res = await dio.get(
    'verse',
    queryParameters: {
      'bible': bible,
      'book': book,
      'chapter': chapter,
      'verse': verse,
    },
  );

  dynamic raw = res.data;
  if (raw is String) raw = jsonDecode(raw);

  if (raw is! Map) return null;

  final data = raw['data'];
  if (data == null) return null;

  return ComparisonResult.fromJson((data as Map).cast<String, dynamic>());
}
