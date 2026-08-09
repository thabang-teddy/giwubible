import 'dart:typed_data';

/// Wraps float PCM samples in a 16-bit mono WAV container.
///
/// sherpa_onnx hands back raw `Float32List` samples in the range [-1, 1]; audio
/// players need a real container. Encoding here in pure Dart keeps the main
/// isolate free of sherpa's FFI bindings and makes the conversion testable.
Uint8List encodeWav(Float32List samples, int sampleRate) {
  const channels = 1;
  const bitsPerSample = 16;
  final byteRate = sampleRate * channels * bitsPerSample ~/ 8;
  final blockAlign = channels * bitsPerSample ~/ 8;
  final dataBytes = samples.length * 2;

  final out = ByteData(44 + dataBytes);

  // RIFF header
  _writeAscii(out, 0, 'RIFF');
  out.setUint32(4, 36 + dataBytes, Endian.little);
  _writeAscii(out, 8, 'WAVE');

  // fmt chunk
  _writeAscii(out, 12, 'fmt ');
  out.setUint32(16, 16, Endian.little); // PCM chunk size
  out.setUint16(20, 1, Endian.little); // format = PCM
  out.setUint16(22, channels, Endian.little);
  out.setUint32(24, sampleRate, Endian.little);
  out.setUint32(28, byteRate, Endian.little);
  out.setUint16(32, blockAlign, Endian.little);
  out.setUint16(34, bitsPerSample, Endian.little);

  // data chunk
  _writeAscii(out, 36, 'data');
  out.setUint32(40, dataBytes, Endian.little);

  for (var i = 0; i < samples.length; i++) {
    // Clamp before scaling: values slightly outside [-1, 1] would otherwise
    // wrap around and produce loud clicks.
    final clamped = samples[i].clamp(-1.0, 1.0);
    final value = (clamped * 32767).round();
    out.setInt16(44 + i * 2, value, Endian.little);
  }

  return out.buffer.asUint8List();
}

void _writeAscii(ByteData data, int offset, String value) {
  for (var i = 0; i < value.length; i++) {
    data.setUint8(offset + i, value.codeUnitAt(i));
  }
}
