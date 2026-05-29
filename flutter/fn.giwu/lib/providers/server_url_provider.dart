import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../api/client.dart';
import '../api/download.dart';
import '../data/bible_database.dart';

/// Manages the API server URL.
///
/// The initial value is loaded from [BibleDatabase] in [main] and passed as a
/// provider override. Calling [save] persists the new URL to the database and
/// immediately updates both Dio clients so subsequent requests use it.
class ServerUrlNotifier extends StateNotifier<String> {
  ServerUrlNotifier(this._db, String initial) : super(initial);

  final BibleDatabase _db;

  /// Persists [url], normalises the trailing slash, and updates the Dio clients.
  Future<void> save(String url) async {
    final normalized = url.endsWith('/') ? url : '$url/';
    await _db.saveServerUrl(normalized);
    setApiBaseUrl(normalized);
    setDownloadBaseUrl(normalized);
    state = normalized;
  }

  /// Resets to the compiled-in default URL.
  Future<void> reset() => save(kDefaultBaseUrl);
}

// Overridden at app startup with the loaded URL and the BibleDatabase instance.
final serverUrlProvider = StateNotifierProvider<ServerUrlNotifier, String>(
  (ref) => throw UnimplementedError('serverUrlProvider not initialized'),
);
