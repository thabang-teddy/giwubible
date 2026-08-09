import 'voice_model.dart';
import 'tts_engine_native.dart' as impl;

final _bracketChars = RegExp(r'[\[\]\{\}]');
final _whitespace = RegExp(r'\s+');

/// Strips editorial markup from a verse so the engine reads a clean sentence.
///
/// Public-domain bible texts wrap translator-supplied words in brackets
/// (KJV `[...]`, some exports `{...}`). The words themselves are part of the
/// sentence, so only the delimiters are removed — dropping the contents would
/// leave ungrammatical speech.
String prepareVerseText(String raw) =>
    raw.replaceAll(_bracketChars, '').replaceAll(_whitespace, ' ').trim();

/// Whether the reader can speak right now.
enum TtsReadiness {
  /// The voice model is installed; playback works with no network at all.
  ready,

  /// No voice model on device yet — it has to be downloaded once.
  needsVoiceModel,

  /// Speech is not possible here (unsupported platform, or a model that will
  /// not load).
  unsupported,
}

/// The slice of speech synthesis this app needs.
///
/// Kept as an interface so the playback state machine can be unit-tested
/// without loading a real model, and so the web build can substitute a no-op.
abstract class TtsEngine {
  /// Checks for an installed voice model and loads it.
  Future<TtsReadiness> prepare();

  /// Drops the loaded model so a freshly downloaded one is picked up.
  Future<void> reload();

  /// [multiplier] is user-facing: 1.0 is normal speed.
  Future<void> setRate(double multiplier);

  /// Completes when the utterance has finished playing.
  Future<void> speak(String text);

  Future<void> stop();

  void dispose();
}

/// Builds the speech engine for the platform this build targets.
TtsEngine createTtsEngine(VoiceModelStore store) => impl.createTtsEngine(store);
