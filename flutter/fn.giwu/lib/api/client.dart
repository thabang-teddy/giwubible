import 'package:dio/dio.dart';

/// Default API base URL. Users can override this in Settings; the stored value
/// is loaded from SQLite at startup and applied via [setApiBaseUrl].
const kDefaultBaseUrl = 'http://api.giwu.test/api/';

final apiClient = Dio(
  BaseOptions(
    baseUrl: kDefaultBaseUrl,
    headers: {'Accept': 'application/json'},
    connectTimeout: const Duration(seconds: 10),
    receiveTimeout: const Duration(seconds: 10),
  ),
);

/// Updates [apiClient]'s base URL at runtime.
/// Call once at startup (after reading from the database) and again whenever
/// the user saves a new server URL from Settings.
void setApiBaseUrl(String url) {
  apiClient.options.baseUrl = url;
}
