import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

import 'voice_model.dart';

VoiceModelStore createVoiceModelStore() => const FileVoiceModelStore();

/// [VoiceModelStore] backed by the app's documents directory.
class FileVoiceModelStore implements VoiceModelStore {
  const FileVoiceModelStore();

  /// Root directory holding every installed voice.
  Future<Directory> rootDir() async {
    final docs = await getApplicationDocumentsDirectory();
    return Directory(p.join(docs.path, 'voices'));
  }

  Future<Directory> modelDir() async =>
      Directory(p.join((await rootDir()).path, kVoiceModelName));

  @override
  Future<VoiceModel?> installed() async {
    final dir = await modelDir();
    if (!await dir.exists()) return null;
    return _resolve(dir);
  }

  /// Finds the model files inside [dir] by shape rather than by hard-coded
  /// names, so the served archive can be swapped for another Piper voice
  /// without a code change.
  Future<VoiceModel?> _resolve(Directory dir) async {
    final entries = await dir.list(recursive: false).toList();

    String? onnx;
    String? tokens;
    String? dataDir;

    for (final entry in entries) {
      final name = p.basename(entry.path);
      if (entry is File && name.endsWith('.onnx')) {
        onnx = entry.path;
      } else if (entry is File && name == 'tokens.txt') {
        tokens = entry.path;
      } else if (entry is Directory && name == 'espeak-ng-data') {
        dataDir = entry.path;
      }
    }

    if (onnx == null || tokens == null || dataDir == null) return null;
    return VoiceModel(onnxPath: onnx, tokensPath: tokens, dataDir: dataDir);
  }

  /// Decompressing ~60MB of bzip2 takes seconds, so it runs on a background
  /// isolate rather than freezing the download screen.
  @override
  Future<VoiceModel?> install(List<int> archiveBytes) async {
    final files = await compute(extractVoiceArchive, archiveBytes);

    // Validate before touching disk. A truncated download or an HTML error
    // page decodes to an empty archive rather than throwing, and deleting the
    // working voice first would turn a failed update into a broken install.
    if (!archiveLooksLikeVoiceModel(files)) return null;

    final root = await rootDir();
    final target = await modelDir();

    // Clear any partial previous attempt so a retry starts clean.
    if (await target.exists()) await target.delete(recursive: true);
    await root.create(recursive: true);

    for (final entry in files.entries) {
      final path = p.join(root.path, entry.key);
      // Refuse paths that escape the voices directory (zip-slip).
      if (!p.isWithin(root.path, path)) continue;
      final file = File(path);
      await file.parent.create(recursive: true);
      await file.writeAsBytes(entry.value);
    }

    return _resolve(target);
  }

  @override
  Future<void> remove() async {
    final dir = await modelDir();
    if (await dir.exists()) await dir.delete(recursive: true);
  }

  @override
  Future<int> installedSize() async {
    final dir = await modelDir();
    if (!await dir.exists()) return 0;

    var total = 0;
    await for (final entry in dir.list(recursive: true)) {
      if (entry is File) total += await entry.length();
    }
    return total;
  }
}
