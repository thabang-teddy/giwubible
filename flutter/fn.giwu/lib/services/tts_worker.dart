import 'dart:async';
import 'dart:isolate';
import 'dart:typed_data';

import 'package:sherpa_onnx/sherpa_onnx.dart';

import 'voice_model.dart';

/// Audio produced for one utterance.
class Synthesis {
  const Synthesis({required this.samples, required this.sampleRate});

  final Float32List samples;
  final int sampleRate;
}

/// Runs sherpa_onnx synthesis on a background isolate.
///
/// Generation is a blocking FFI call that takes a meaningful fraction of a
/// second per verse. On the UI isolate that would freeze scrolling every time a
/// verse starts, so the model is loaded once inside a dedicated isolate and
/// driven over a port.
class TtsWorker {
  TtsWorker._({
    required SendPort sendPort,
    required Isolate isolate,
    required ReceivePort receivePort,
    required Map<int, Completer<Synthesis>> pending,
  })  : _sendPort = sendPort,
        _isolate = isolate,
        _receivePort = receivePort,
        _pending = pending;

  final SendPort _sendPort;
  final Isolate _isolate;
  final ReceivePort _receivePort;

  /// Shared with the port listener, which completes entries as results arrive.
  final Map<int, Completer<Synthesis>> _pending;

  var _nextId = 0;
  var _disposed = false;

  /// Loads [model] in a fresh isolate. Throws when the model cannot be loaded.
  static Future<TtsWorker> spawn(VoiceModel model) async {
    final receivePort = ReceivePort();
    final pending = <int, Completer<Synthesis>>{};
    final ready = Completer<SendPort>();

    receivePort.listen((message) {
      switch (message) {
        case SendPort():
          if (!ready.isCompleted) ready.complete(message);
        case _WorkerFailure(:final error):
          if (!ready.isCompleted) ready.completeError(StateError(error));
        case _SynthesisResult(:final id, :final error, :final samples):
          final completer = pending.remove(id);
          if (completer == null || completer.isCompleted) return;
          if (error != null || samples == null) {
            completer.completeError(StateError(error ?? 'synthesis failed'));
          } else {
            completer.complete(
              Synthesis(samples: samples, sampleRate: message.sampleRate),
            );
          }
      }
    });

    final isolate = await Isolate.spawn(
      _workerMain,
      _WorkerInit(
        reply: receivePort.sendPort,
        onnxPath: model.onnxPath,
        tokensPath: model.tokensPath,
        dataDir: model.dataDir,
      ),
      onError: receivePort.sendPort,
      errorsAreFatal: true,
    );

    try {
      final sendPort = await ready.future;
      return TtsWorker._(
        sendPort: sendPort,
        isolate: isolate,
        receivePort: receivePort,
        pending: pending,
      );
    } on Object {
      receivePort.close();
      isolate.kill(priority: Isolate.immediate);
      rethrow;
    }
  }

  /// Synthesizes [text]. [speed] is a multiplier where 1.0 is the model's
  /// natural pace.
  Future<Synthesis> synthesize(String text, {double speed = 1.0}) {
    if (_disposed) {
      return Future.error(StateError('TtsWorker has been disposed'));
    }
    final id = _nextId++;
    final completer = Completer<Synthesis>();
    _pending[id] = completer;
    _sendPort.send(_SynthesisRequest(id: id, text: text, speed: speed));
    return completer.future;
  }

  Future<void> dispose() async {
    if (_disposed) return;
    _disposed = true;

    for (final completer in _pending.values) {
      if (!completer.isCompleted) {
        completer.completeError(StateError('TtsWorker disposed'));
      }
    }
    _pending.clear();

    _receivePort.close();
    _isolate.kill(priority: Isolate.immediate);
  }
}

// ── Isolate side ───────────────────────────────────────────────────────────

void _workerMain(_WorkerInit init) {
  final commands = ReceivePort();

  final OfflineTts tts;
  try {
    // Bindings are per-isolate, so the native library is loaded again here.
    initBindings();
    tts = OfflineTts(
      OfflineTtsConfig(
        model: OfflineTtsModelConfig(
          vits: OfflineTtsVitsModelConfig(
            model: init.onnxPath,
            tokens: init.tokensPath,
            dataDir: init.dataDir,
          ),
          numThreads: 2,
          debug: false,
        ),
      ),
    );
  } on Object catch (e) {
    init.reply.send(_WorkerFailure('Failed to load voice model: $e'));
    commands.close();
    return;
  }

  init.reply.send(commands.sendPort);

  commands.listen((message) {
    if (message is! _SynthesisRequest) return;
    try {
      final audio = tts.generate(text: message.text, speed: message.speed);
      init.reply.send(
        _SynthesisResult(
          id: message.id,
          samples: audio.samples,
          sampleRate: audio.sampleRate,
        ),
      );
    } on Object catch (e) {
      init.reply.send(
        _SynthesisResult(id: message.id, sampleRate: 0, error: '$e'),
      );
    }
  });
}

// ── Port messages ──────────────────────────────────────────────────────────

class _WorkerInit {
  const _WorkerInit({
    required this.reply,
    required this.onnxPath,
    required this.tokensPath,
    required this.dataDir,
  });

  final SendPort reply;
  final String onnxPath;
  final String tokensPath;
  final String dataDir;
}

class _WorkerFailure {
  const _WorkerFailure(this.error);
  final String error;
}

class _SynthesisRequest {
  const _SynthesisRequest({
    required this.id,
    required this.text,
    required this.speed,
  });

  final int id;
  final String text;
  final double speed;
}

class _SynthesisResult {
  const _SynthesisResult({
    required this.id,
    required this.sampleRate,
    this.samples,
    this.error,
  });

  final int id;
  final int sampleRate;
  final Float32List? samples;
  final String? error;
}
