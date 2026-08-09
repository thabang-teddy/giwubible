import 'package:dio/dio.dart';

import '../services/voice_model.dart';
import 'client.dart';

// Separate client: the voice archive is ~63MB, far beyond the timeouts that
// suit the JSON endpoints.
final _voiceClient = Dio(
  BaseOptions(
    baseUrl: kDefaultBaseUrl,
    connectTimeout: const Duration(seconds: 30),
    receiveTimeout: const Duration(minutes: 15),
    responseType: ResponseType.bytes,
  ),
);

/// Updates the voice client's base URL at runtime.
/// Call alongside [setApiBaseUrl] whenever the server URL changes.
void setVoiceBaseUrl(String url) {
  _voiceClient.options.baseUrl = url;
}

/// Downloads the read-aloud voice archive.
///
/// [onProgress] receives a value in 0..1, or null while the server has not
/// declared a content length.
Future<List<int>> downloadVoiceArchive({
  void Function(double? progress)? onProgress,
  CancelToken? cancelToken,
}) async {
  final response = await _voiceClient.get<List<int>>(
    kVoiceModelArchivePath,
    cancelToken: cancelToken,
    onReceiveProgress: (received, total) {
      if (onProgress == null) return;
      onProgress(total > 0 ? received / total : null);
    },
  );

  final bytes = response.data;
  if (bytes == null || bytes.isEmpty) {
    throw StateError('Voice download returned no data');
  }
  return bytes;
}
