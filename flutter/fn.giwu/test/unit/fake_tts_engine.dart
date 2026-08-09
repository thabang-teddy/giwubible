import 'dart:async';

import 'package:fn_giwu/services/tts_service.dart';

/// In-memory [TtsEngine] that lets a test decide exactly when an utterance
/// finishes, so the controller's sequencing can be driven step by step.
class FakeTtsEngine implements TtsEngine {
  FakeTtsEngine({this.readiness = TtsReadiness.ready});

  final TtsReadiness readiness;

  /// Every utterance handed to the engine, in order.
  final List<String> spoken = [];

  /// Rate multipliers applied, in order.
  final List<double> rates = [];

  int stopCount = 0;
  bool disposed = false;

  /// Set to make the next [speak] throw instead of completing.
  Object? speakError;

  Completer<void>? _pending;

  /// True while an utterance is in flight.
  bool get isSpeaking => _pending != null && !_pending!.isCompleted;

  /// Completes the in-flight utterance, as the platform engine would.
  Future<void> finishUtterance() async {
    final pending = _pending;
    if (pending == null || pending.isCompleted) return;
    pending.complete();
    // Let the controller's loop advance before the test asserts.
    await Future<void>.delayed(Duration.zero);
  }

  int reloadCount = 0;

  @override
  Future<TtsReadiness> prepare() async => readiness;

  @override
  Future<void> reload() async => reloadCount++;

  @override
  Future<void> setRate(double multiplier) async => rates.add(multiplier);

  @override
  Future<void> speak(String text) {
    final error = speakError;
    if (error != null) {
      speakError = null;
      return Future<void>.error(error);
    }
    spoken.add(text);
    final completer = Completer<void>();
    _pending = completer;
    return completer.future;
  }

  @override
  Future<void> stop() async {
    stopCount++;
    // The platform resolves a pending utterance when speech is cancelled.
    final pending = _pending;
    if (pending != null && !pending.isCompleted) pending.complete();
    _pending = null;
  }

  @override
  void dispose() => disposed = true;
}
