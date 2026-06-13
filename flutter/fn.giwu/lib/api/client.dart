import 'package:dio/dio.dart';

const kDefaultBaseUrl = 'https://api.giwu.test/api/';

final dio = Dio(
  BaseOptions(
    baseUrl: kDefaultBaseUrl,
    headers: {'Accept': 'application/json'},
    connectTimeout: const Duration(seconds: 10),
    receiveTimeout: const Duration(seconds: 10),
  ),
);

/// Updates the API base URL at runtime.
void setApiBaseUrl(String url) {
  dio.options.baseUrl = url;
}

/// Attaches or removes the Bearer token from all subsequent requests.
void setAuthToken(String? token) {
  if (token != null) {
    dio.options.headers['Authorization'] = 'Bearer $token';
  } else {
    dio.options.headers.remove('Authorization');
  }
}
