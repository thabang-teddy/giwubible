import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/verse.dart';
import '../services/tts_service.dart';
import 'prefs_provider.dart';
import 'voice_model_provider.dart';

enum TtsStatus { idle, speaking, paused }

@immutable
class TtsState {
  const TtsState({
    this.status = TtsStatus.idle,
    this.verse,
    this.reachedEnd = false,
  });

  final TtsStatus status;

  /// Verse number currently being spoken, or the one playback is paused on.
  final int? verse;

  /// Set for a single transition when the last verse of a chapter finished,
  /// so a listener can decide whether to roll into the next chapter.
  final bool reachedEnd;

  bool get isActive => status != TtsStatus.idle;

  TtsState copyWith({
    TtsStatus? status,
    int? verse,
    bool? reachedEnd,
  }) =>
      TtsState(
        status: status ?? this.status,
        verse: verse ?? this.verse,
        reachedEnd: reachedEnd ?? this.reachedEnd,
      );
}

/// Speaks a chapter one verse at a time.
///
/// Each utterance is awaited individually so the UI can highlight and scroll to
/// the verse being read, and so playback can be resumed or skipped at verse
/// granularity. Pausing stops the engine and remembers the index — resuming
/// re-reads the current verse from its start, which every platform supports.
class TtsController extends StateNotifier<TtsState> {
  TtsController({
    required TtsEngine engine,
    double rate = 1.0,
    bool announceNumbers = false,
  })  : _engine = engine,
        _rate = rate,
        _announceNumbers = announceNumbers,
        super(const TtsState());

  final TtsEngine _engine;

  List<VerseModel> _verses = const [];
  int _index = 0;

  /// Incremented on every interruption. A speak-loop whose token is stale must
  /// not touch state — this is what makes stop/pause/skip race-free.
  int _session = 0;

  double _rate;
  bool _announceNumbers;

  set announceNumbers(bool value) => _announceNumbers = value;

  /// Starts reading [verses], optionally from a specific verse number.
  Future<void> start(List<VerseModel> verses, {int? fromVerse}) async {
    if (verses.isEmpty) return;
    _verses = verses;
    final i =
        fromVerse == null ? -1 : verses.indexWhere((v) => v.v == fromVerse);
    _index = i < 0 ? 0 : i;
    await _playFromIndex();
  }

  Future<void> pause() async {
    if (state.status != TtsStatus.speaking) return;
    _session++;
    await _engine.stop();
    if (!mounted) return;
    state = state.copyWith(status: TtsStatus.paused);
  }

  Future<void> resume() async {
    if (state.status != TtsStatus.paused) return;
    await _playFromIndex();
  }

  Future<void> stop() async {
    _session++;
    await _engine.stop();
    if (!mounted) return;
    state = const TtsState();
  }

  /// Skips forward; stops when already on the last verse.
  Future<void> next() async {
    if (!state.isActive) return;
    if (_index >= _verses.length - 1) {
      await stop();
      return;
    }
    _index++;
    await _playFromIndex();
  }

  /// Skips back; restarts the first verse when already at the start.
  Future<void> previous() async {
    if (!state.isActive) return;
    if (_index > 0) _index--;
    await _playFromIndex();
  }

  Future<void> setRate(double multiplier) async {
    _rate = multiplier;
    // Re-issue the current verse so the new rate takes effect immediately.
    if (state.status == TtsStatus.speaking) await _playFromIndex();
  }

  /// Acknowledges the end-of-chapter signal so it fires only once.
  void clearReachedEnd() {
    if (!mounted || !state.reachedEnd) return;
    state = TtsState(status: state.status, verse: state.verse);
  }

  Future<void> _playFromIndex() async {
    final session = ++_session;
    await _engine.stop();
    if (!mounted || session != _session) return;

    await _engine.setRate(_rate);
    if (!mounted || session != _session) return;

    await _speakLoop(session);
  }

  Future<void> _speakLoop(int session) async {
    while (_index < _verses.length) {
      if (!mounted || session != _session) return;

      final verse = _verses[_index];
      state = TtsState(status: TtsStatus.speaking, verse: verse.v);

      final text = _utteranceFor(verse);
      if (text.isNotEmpty) {
        try {
          await _engine.speak(text);
        } on Exception catch (e) {
          debugPrint('TTS speak failed: $e');
          if (!mounted || session != _session) return;
          state = const TtsState();
          return;
        }
      }

      if (!mounted || session != _session) return;
      _index++;
    }

    if (!mounted || session != _session) return;
    state = const TtsState(reachedEnd: true);
  }

  String _utteranceFor(VerseModel verse) {
    final text = prepareVerseText(verse.t);
    if (text.isEmpty) return '';
    return _announceNumbers ? 'Verse ${verse.v}. $text' : text;
  }

  @override
  void dispose() {
    _session++;
    _engine.stop();
    super.dispose();
  }
}

// ── Providers ──────────────────────────────────────────────────────────────

/// Overridden in tests with a fake engine.
final ttsEngineProvider = Provider<TtsEngine>((ref) {
  final engine = createTtsEngine(ref.watch(voiceModelStoreProvider));
  ref.onDispose(engine.dispose);
  return engine;
});

/// Whether the on-device voice is installed and loadable.
///
/// Re-probed whenever the voice model is installed or removed, so the reader
/// picks up a freshly downloaded voice without an app restart.
final ttsReadinessProvider = FutureProvider<TtsReadiness>((ref) async {
  final install = ref.watch(voiceModelProvider);
  final engine = ref.watch(ttsEngineProvider);

  // The engine caches its probe; the install state changing means that
  // cache is stale.
  await engine.reload();
  if (install.status != VoiceInstallStatus.installed) {
    return TtsReadiness.needsVoiceModel;
  }
  return engine.prepare();
});

final ttsControllerProvider =
    StateNotifierProvider<TtsController, TtsState>((ref) {
  final controller = TtsController(
    engine: ref.watch(ttsEngineProvider),
    rate: ref.read(speechRateProvider),
    announceNumbers: ref.read(announceVerseNumbersProvider),
  );

  ref.listen<double>(
    speechRateProvider,
    (_, next) => controller.setRate(next),
  );
  ref.listen<bool>(
    announceVerseNumbersProvider,
    (_, next) => controller.announceNumbers = next,
  );

  return controller;
});

/// Verse currently being read aloud.
///
/// Deliberately separate from `activeVerseProvider`: that one drives the
/// comparison sheet, and reusing it would re-open the sheet on every verse.
final speakingVerseProvider =
    Provider<int?>((ref) => ref.watch(ttsControllerProvider).verse);
