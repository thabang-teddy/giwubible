import 'dart:convert';

import 'package:flutter/foundation.dart';

import '../models/verse.dart';
import 'client.dart';

Future<List<VerseModel>> getChapter(
  String bible,
  int book,
  int chapter,
) async {
  final res = await dio.get(
    'chapter',
    queryParameters: {'bible': bible, 'book': book, 'chapter': chapter},
  );

  dynamic raw = res.data;
  if (raw is String) raw = jsonDecode(raw);

  final List<dynamic> list;
  if (raw is List) {
    list = raw;
  } else if (raw is Map) {
    final inner = raw['data'] ?? raw['verses'] ?? raw['items'];
    if (inner is List) {
      list = inner;
    } else {
      debugPrint('[Chapter] ERROR: unexpected response shape — ${raw.runtimeType}');
      return [];
    }
  } else {
    debugPrint('[Chapter] ERROR: unexpected response shape — ${raw.runtimeType}');
    return [];
  }

  return list.map((e) {
    final map = (e as Map).cast<String, dynamic>();
    return VerseModel.fromJson(map);
  }).toList();
}
