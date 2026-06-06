import 'dart:convert';

import 'package:flutter/foundation.dart';

import '../models/book.dart';
import 'client.dart';

Future<List<BookModel>> getBooks() async {
  try {
    final res = await dio.get('books');

    if (kDebugMode) {
      debugPrint('[Books] status: ${res.statusCode}');
      debugPrint('[Books] data type: ${res.data.runtimeType}');
      final preview = res.data.toString();
      debugPrint(
        '[Books] data preview: ${preview.substring(0, preview.length.clamp(0, 400))}',
      );
    }

    // Dio may skip JSON decoding if Content-Type is missing/wrong — decode
    // manually in that case.
    dynamic raw = res.data;
    if (raw is String) {
      debugPrint('[Books] WARNING: res.data is a String — decoding manually');
      raw = jsonDecode(raw);
    }

    // Plain array: [...]
    if (raw is List) {
      return _parseList(raw);
    }

    // Wrapped: {"data": [...]} or {"books": [...]} or {"items": [...]}
    if (raw is Map) {
      final inner =
          raw['data'] ?? raw['books'] ?? raw['items'];
      if (inner is List) {
        return _parseList(inner);
      }
      debugPrint('[Books] WARNING: Map response had no recognisable list key. Keys: ${raw.keys.toList()}');
    }

    debugPrint('[Books] ERROR: unexpected response shape — ${raw.runtimeType}');
    return [];
  } catch (e, st) {
    debugPrint('[Books] EXCEPTION: $e');
    debugPrint('[Books] STACKTRACE: $st');
    rethrow; // Let Riverpod surface this as an error so the UI shows it.
  }
}

List<BookModel> _parseList(List<dynamic> list) {
  final result = <BookModel>[];
  for (final e in list) {
    if (e is! Map) {
      debugPrint('[Books] WARNING: skipping non-Map element: ${e.runtimeType}');
      continue;
    }
    // Normalise Map<dynamic,dynamic> → Map<String,dynamic>
    final map = e.cast<String, dynamic>();
    result.add(BookModel.fromJson(map));
  }
  debugPrint('[Books] parsed ${result.length} books');
  return result;
}
