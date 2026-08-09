@Tags(['live'])
library;

import 'package:flutter_test/flutter_test.dart';
import 'package:fn_giwu/api/voice_download.dart';
import 'package:fn_giwu/services/voice_model.dart';

/// Exercises the real voice download against a running Giwu server.
///
/// Skipped by default (see `dart_test.yaml`) because it needs a live server and
/// pulls ~64MB. Run it against a dev server with:
///
///     flutter test --tags live --run-skipped
///
/// Override the server with:
///
///     --dart-define=GIWU_BASE_URL=http://giwubible.test/api/
const _baseUrl = String.fromEnvironment(
  'GIWU_BASE_URL',
  defaultValue: 'http://giwubible.test/api/',
);

void main() {
  setUpAll(() => setVoiceBaseUrl(_baseUrl));

  test('downloads and unpacks a usable voice from the server', () async {
    // Arrange
    final progress = <double>[];

    // Act
    final bytes = await downloadVoiceArchive(
      onProgress: (p) {
        if (p != null) progress.add(p);
      },
    );

    // Assert — the transfer itself
    expect(bytes, isNotEmpty, reason: 'server returned no bytes');
    expect(
      bytes.length,
      greaterThan(50 * 1024 * 1024),
      reason: 'archive is far smaller than a Piper voice should be',
    );
    expect(progress, isNotEmpty, reason: 'no progress was reported');
    expect(progress.last, closeTo(1.0, 0.01));

    // Assert — the archive is what the installer expects
    final files = extractVoiceArchive(bytes);
    expect(files, isNotEmpty, reason: 'archive did not decompress');
    expect(
      archiveLooksLikeVoiceModel(files),
      isTrue,
      reason: 'archive lacks a .onnx, tokens.txt or espeak-ng-data',
    );

    // Assert — the exact files the engine loads are present
    expect(
      files.keys.where((k) => k.endsWith('.onnx')),
      isNotEmpty,
      reason: 'no acoustic model in the archive',
    );
    expect(
      files.keys.where((k) => k.endsWith('/tokens.txt')),
      isNotEmpty,
      reason: 'no token table in the archive',
    );
    expect(
      files.keys.where((k) => k.contains('espeak-ng-data/')),
      isNotEmpty,
      reason: 'no phoneme data in the archive',
    );
  }, timeout: const Timeout(Duration(minutes: 10)));
}
