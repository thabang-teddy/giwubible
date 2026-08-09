import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:fn_giwu/services/wav_encoder.dart';

String _ascii(Uint8List bytes, int offset, int length) =>
    String.fromCharCodes(bytes.sublist(offset, offset + length));

void main() {
  group('encodeWav', () {
    test('writes a RIFF/WAVE header of the expected length', () {
      // Arrange
      final samples = Float32List.fromList([0.0, 0.5, -0.5]);

      // Act
      final wav = encodeWav(samples, 22050);

      // Assert
      expect(wav.length, 44 + samples.length * 2);
      expect(_ascii(wav, 0, 4), 'RIFF');
      expect(_ascii(wav, 8, 4), 'WAVE');
      expect(_ascii(wav, 12, 4), 'fmt ');
      expect(_ascii(wav, 36, 4), 'data');
    });

    test('declares 16-bit mono PCM at the given sample rate', () {
      final wav = encodeWav(Float32List.fromList([0.0]), 16000);
      final view = ByteData.sublistView(wav);

      expect(view.getUint16(20, Endian.little), 1, reason: 'PCM format');
      expect(view.getUint16(22, Endian.little), 1, reason: 'mono');
      expect(view.getUint32(24, Endian.little), 16000);
      expect(view.getUint32(28, Endian.little), 32000, reason: 'byte rate');
      expect(view.getUint16(32, Endian.little), 2, reason: 'block align');
      expect(view.getUint16(34, Endian.little), 16, reason: 'bit depth');
    });

    test('records the data chunk size in both headers', () {
      final samples = Float32List.fromList([0.1, 0.2, 0.3, 0.4]);
      final wav = encodeWav(samples, 22050);
      final view = ByteData.sublistView(wav);

      expect(view.getUint32(40, Endian.little), 8, reason: 'data chunk');
      expect(view.getUint32(4, Endian.little), 36 + 8, reason: 'riff size');
    });

    test('scales floats to signed 16-bit', () {
      final wav = encodeWav(Float32List.fromList([0.0, 1.0, -1.0]), 16000);
      final view = ByteData.sublistView(wav);

      expect(view.getInt16(44, Endian.little), 0);
      expect(view.getInt16(46, Endian.little), 32767);
      expect(view.getInt16(48, Endian.little), -32767);
    });

    test('clamps out-of-range samples instead of wrapping into loud clicks',
        () {
      final wav = encodeWav(Float32List.fromList([2.5, -2.5]), 16000);
      final view = ByteData.sublistView(wav);

      expect(view.getInt16(44, Endian.little), 32767);
      expect(view.getInt16(46, Endian.little), -32767);
    });

    test('produces a header-only file for empty audio', () {
      final wav = encodeWav(Float32List(0), 22050);

      expect(wav.length, 44);
      expect(ByteData.sublistView(wav).getUint32(40, Endian.little), 0);
    });
  });
}
