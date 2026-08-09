import 'package:archive/archive.dart';
import 'package:path/path.dart' as p;

import 'voice_model_io.dart' as impl;

/// Where the read-aloud voice lives on disk.
///
/// The model is not shipped in the app — it is downloaded once from the Giwu
/// server (the same server that serves the bible downloads) and unpacked into
/// the app's documents directory. After that first download the reader never
/// touches the network again.
class VoiceModel {
  const VoiceModel({
    required this.onnxPath,
    required this.tokensPath,
    required this.dataDir,
  });

  /// The VITS acoustic model.
  final String onnxPath;

  /// Token-to-id table that accompanies the model.
  final String tokensPath;

  /// espeak-ng phoneme data directory used to turn text into phonemes.
  final String dataDir;
}

/// Directory name the model is unpacked into, and the archive served for it.
const String kVoiceModelName = 'vits-piper-en_US-amy-low';

/// Path on the Giwu server, relative to the configured base URL.
///
/// Served by `AppDownloadController`, which only serves allowlisted filenames —
/// adding a new voice means adding it to that allowlist too.
const String kVoiceModelArchivePath = 'downloads/$kVoiceModelName.tar.bz2';

/// Locates, installs and removes the on-device voice model.
abstract class VoiceModelStore {
  /// Returns the installed model, or null when it is absent or incomplete
  /// (a half-finished install must not be treated as usable).
  Future<VoiceModel?> installed();

  /// Unpacks a downloaded `.tar.bz2` into the voices directory.
  /// Returns null when the archive does not contain a usable voice.
  Future<VoiceModel?> install(List<int> archiveBytes);

  Future<void> remove();

  /// Bytes the installed voice occupies on disk.
  Future<int> installedSize();
}

/// Builds the store for the platform this build targets.
VoiceModelStore createVoiceModelStore() => impl.createVoiceModelStore();

/// Whether an extracted archive actually contains a usable voice.
///
/// The decoder returns an empty archive for corrupt input instead of throwing,
/// so the contents have to be checked explicitly.
bool archiveLooksLikeVoiceModel(Map<String, List<int>> files) {
  var hasModel = false;
  var hasTokens = false;
  var hasPhonemeData = false;

  for (final path in files.keys) {
    final parts = p.split(path);
    if (parts.length < 2) continue;
    final name = parts.last;

    if (name.endsWith('.onnx')) hasModel = true;
    if (name == 'tokens.txt') hasTokens = true;
    if (parts.contains('espeak-ng-data')) hasPhonemeData = true;
  }

  return hasModel && hasTokens && hasPhonemeData;
}

/// Decompresses a bzip2 tar archive into a path-to-bytes map.
///
/// Top-level and isolate-safe so it can be run through `compute`, keeping the
/// ~60MB decode off the UI thread.
Map<String, List<int>> extractVoiceArchive(List<int> archiveBytes) {
  final tar = BZip2Decoder().decodeBytes(archiveBytes);
  final archive = TarDecoder().decodeBytes(tar);

  return {
    for (final file in archive.files)
      if (file.isFile) file.name: file.content as List<int>,
  };
}
