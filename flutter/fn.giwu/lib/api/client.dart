import 'package:dio/dio.dart';

const kDefaultBaseUrl = 'https://giwu.co.za/api/';  // live server
// const kDefaultBaseUrl = 'http://giwubible.test/api/'; // dev server 

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

/// True when the server refused the request because the account's email
/// address has not been confirmed.
///
/// The bookmark endpoints are the only ones behind `verified`, and nothing
/// else in the API answers 403, so the status alone identifies it — more
/// durable than matching on the message text.
bool isEmailUnverifiedError(Object error) =>
    error is DioException && error.response?.statusCode == 403;

/// The `message` the API sent with a failure, when it sent one.
///
/// Laravel reports refusals as `{"message": "..."}`, and some of them are
/// worth showing verbatim — a 429 from the resend endpoint explains that
/// several links have already gone out, which a generic "try again" would
/// throw away.
String? serverMessage(Object error) {
  if (error is! DioException) return null;
  final data = error.response?.data;
  if (data is Map && data['message'] is String) {
    final message = data['message'] as String;
    return message.trim().isEmpty ? null : message;
  }
  return null;
}
