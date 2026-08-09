import 'package:archive/archive.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:fn_giwu/services/voice_model.dart';

/// Builds a `.tar.bz2` with the given path-to-content entries.
List<int> _archive(Map<String, String> files) {
  final archive = Archive();
  for (final entry in files.entries) {
    final bytes = entry.value.codeUnits;
    archive.addFile(ArchiveFile(entry.key, bytes.length, bytes));
  }
  final tar = TarEncoder().encode(archive);
  return BZip2Encoder().encode(tar);
}

void main() {
  group('extractVoiceArchive', () {
    test('unpacks a bzip2 tar into a path-to-bytes map', () {
      // Arrange
      final bytes = _archive({
        '$kVoiceModelName/tokens.txt': 'a b c',
        '$kVoiceModelName/espeak-ng-data/phontab': 'data',
      });

      // Act
      final files = extractVoiceArchive(bytes);

      // Assert
      expect(files.keys, contains('$kVoiceModelName/tokens.txt'));
      expect(
        String.fromCharCodes(files['$kVoiceModelName/tokens.txt']!),
        'a b c',
      );
      expect(
        files.keys,
        contains('$kVoiceModelName/espeak-ng-data/phontab'),
      );
    });

    test('preserves nested directory structure in the keys', () {
      final files = extractVoiceArchive(
        _archive({'$kVoiceModelName/espeak-ng-data/lang/gmw/en': 'x'}),
      );

      expect(
        files.keys.single,
        '$kVoiceModelName/espeak-ng-data/lang/gmw/en',
      );
    });

    test('yields nothing for data that is not a bzip2 archive', () {
      // The decoder tolerates junk rather than throwing, so callers must treat
      // an empty result as failure.
      expect(extractVoiceArchive(List<int>.filled(64, 7)), isEmpty);
    });

    test('yields nothing for an HTML error page served instead of a file', () {
      expect(
        extractVoiceArchive('<html>404 Not Found</html>'.codeUnits),
        isEmpty,
      );
    });
  });

  group('archiveLooksLikeVoiceModel', () {
    Map<String, List<int>> entries(List<String> paths) => {
          for (final path in paths) path: const <int>[],
        };

    test('accepts an archive with a model, tokens and phoneme data', () {
      expect(
        archiveLooksLikeVoiceModel(entries([
          '$kVoiceModelName/en_US-amy-low.onnx',
          '$kVoiceModelName/tokens.txt',
          '$kVoiceModelName/espeak-ng-data/phontab',
        ])),
        isTrue,
      );
    });

    test('accepts a differently named voice, so the model can be swapped', () {
      expect(
        archiveLooksLikeVoiceModel(entries([
          'some-other-voice/en_GB-alan-low.onnx',
          'some-other-voice/tokens.txt',
          'some-other-voice/espeak-ng-data/phontab',
        ])),
        isTrue,
      );
    });

    test('rejects an empty archive from a corrupt download', () {
      expect(archiveLooksLikeVoiceModel(const {}), isFalse);
    });

    test('rejects an archive missing the phoneme data directory', () {
      expect(
        archiveLooksLikeVoiceModel(entries([
          '$kVoiceModelName/en_US-amy-low.onnx',
          '$kVoiceModelName/tokens.txt',
        ])),
        isFalse,
      );
    });

    test('rejects an archive missing the model itself', () {
      expect(
        archiveLooksLikeVoiceModel(entries([
          '$kVoiceModelName/tokens.txt',
          '$kVoiceModelName/espeak-ng-data/phontab',
        ])),
        isFalse,
      );
    });
  });

  group('voice model constants', () {
    test('the archive path uses the allowlisted downloads route', () {
      expect(kVoiceModelArchivePath, 'downloads/$kVoiceModelName.tar.bz2');
    });
  });
}
