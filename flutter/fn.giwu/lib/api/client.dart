import 'package:dio/dio.dart';

const baseUrl = 'http://api.giwu.test/api/';

final apiClient = Dio(
  BaseOptions(
    baseUrl: baseUrl,
    headers: {'Accept': 'application/json'},
    connectTimeout: const Duration(seconds: 10),
    receiveTimeout: const Duration(seconds: 10),
  ),
);
