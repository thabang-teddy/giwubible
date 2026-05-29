import 'package:dio/dio.dart';

import '../models/bible.dart';
import '../models/verse.dart';
import 'client.dart';

// Separate client with a longer receive timeout for large verse payloads.
final _downloadClient = Dio(
  BaseOptions(
    baseUrl: kDefaultBaseUrl,
    headers: {'Accept': 'application/json'},
    connectTimeout: const Duration(seconds: 30),
    receiveTimeout: const Duration(minutes: 5),
  ),
);

/// Updates the download client's base URL at runtime.
/// Call alongside [setApiBaseUrl] whenever the server URL changes.
void setDownloadBaseUrl(String url) {
  _downloadClient.options.baseUrl = url;
}

class BibleDownloadPayload {
  final BibleModel bible;
  final List<VerseModel> verses;

  const BibleDownloadPayload({required this.bible, required this.verses});
}

Future<BibleDownloadPayload> downloadBibleFromApi(String table) async {
  final res = await _downloadClient.get('bibles/$table/download');
  final data = (res.data['data'] as Map).cast<String, dynamic>();

  final bible = BibleModel(
    table: data['table'] as String,
    abbreviation: data['abbreviation'] as String,
    version: data['version'] as String,
    downloaded: true,
  );

  final verses = (data['verses'] as List)
      .map((v) => VerseModel.fromJson((v as Map).cast<String, dynamic>()))
      .toList();

  return BibleDownloadPayload(bible: bible, verses: verses);
}
