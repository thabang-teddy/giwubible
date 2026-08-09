import 'dart:async';
import 'dart:io';

import 'package:audioplayers/audioplayers.dart';
import 'package:flutter/foundation.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

import 'tts_service.dart';
import 'tts_worker.dart';
import 'voice_model.dart';
import 'wav_encoder.dart';

TtsEngine createTtsEngine(VoiceModelStore store) =>
    SherpaTtsEngine(store: store);

/// [TtsEngine] built on sherpa_onnx.
///
/// Everything runs on-device: a Piper VITS model synthesizes the verse into PCM
/// samples on a background isolate, those samples are wrapped in a WAV
/// container and played back locally. Nothing is sent anywhere, and after the
/// one-time model download the feature never uses the network again.
class SherpaTtsEngine implements TtsEngine {
  SherpaTtsEngine({
    required VoiceModelStore store,
    AudioPlayer? player,
  })  : _store = store,
        _player = player ?? AudioPlayer();

  final VoiceModelStore _store;
  final AudioPlayer _player;

  TtsWorker? _worker;
  TtsReadiness? _readiness;
  double _speed = 1.0;
  Directory? _scratchDir;
  var _clipCounter = 0;

  /// Bumped by [stop] so audio synthesized for a cancelled run is discarded.
  var _generation = 0;

  /// Completes when the clip now playing finishes — or when [stop] cuts it off.
  Completer<void>? _activeUtterance;

  @override
  Future<TtsReadiness> prepare() async {
    final cached = _readiness;
    if (cached != null) return cached;
    return _readiness = await _load();
  }

  Future<TtsReadiness> _load() async {
    final model = await _store.installed();
    if (model == null) return TtsReadiness.needsVoiceModel;

    try {
      _worker = await TtsWorker.spawn(model);
      await _player.setReleaseMode(ReleaseMode.stop);
      return TtsReadiness.ready;
    } on Object catch (e) {
      debugPrint('Voice model failed to load: $e');
      return TtsReadiness.unsupported;
    }
  }

  @override
  Future<void> reload() async {
    await _worker?.dispose();
    _worker = null;
    _readiness = null;
  }

  @override
  Future<void> setRate(double multiplier) async {
    // Piper's speed parameter is a direct multiplier, so it maps one to one.
    _speed = multiplier;
  }

  @override
  Future<void> speak(String text) async {
    final worker = _worker;
    if (worker == null) throw StateError('Voice model is not loaded');

    // Synthesis is not cancellable, so note which playback generation asked
    // for it — if stop() ran while the isolate was working, drop the audio
    // instead of playing something the user already cancelled.
    final generation = _generation;
    final audio = await worker.synthesize(text, speed: _speed);
    if (audio.samples.isEmpty || generation != _generation) return;

    final file = await _writeClip(audio);
    final done = Completer<void>();
    // stop() completes this too — audioplayers does not emit onPlayerComplete
    // for a stopped clip, so awaiting that stream alone would hang forever.
    _activeUtterance = done;
    final subscription = _player.onPlayerComplete.listen((_) {
      if (!done.isCompleted) done.complete();
    });

    try {
      await _player.play(DeviceFileSource(file.path));
      await done.future;
    } finally {
      await subscription.cancel();
      if (identical(_activeUtterance, done)) _activeUtterance = null;
      // Best effort — a leftover clip is harmless, a crash here is not.
      try {
        if (await file.exists()) await file.delete();
      } on Object catch (_) {}
    }
  }

  Future<File> _writeClip(Synthesis audio) async {
    final dir = _scratchDir ??= await getTemporaryDirectory();
    final file = File(p.join(dir.path, 'giwu_tts_${_clipCounter++}.wav'));
    await file.writeAsBytes(encodeWav(audio.samples, audio.sampleRate));
    return file;
  }

  @override
  Future<void> stop() async {
    // Invalidates any synthesis still in flight so its audio is discarded.
    _generation++;
    try {
      await _player.stop();
    } on Object catch (e) {
      debugPrint('Audio stop failed: $e');
    }
    final active = _activeUtterance;
    if (active != null && !active.isCompleted) active.complete();
    _activeUtterance = null;
  }

  @override
  void dispose() {
    _worker?.dispose();
    _worker = null;
    _player.dispose();
  }
}
