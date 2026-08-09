import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../api/voice_download.dart' as api;
import '../services/voice_model.dart';

enum VoiceInstallStatus { absent, downloading, extracting, installed, failed }

@immutable
class VoiceInstallState {
  const VoiceInstallState({
    this.status = VoiceInstallStatus.absent,
    this.progress,
    this.error,
    this.sizeBytes = 0,
  });

  final VoiceInstallStatus status;

  /// Download progress in 0..1, or null when the total size is unknown.
  final double? progress;
  final String? error;

  /// Bytes the installed voice occupies on disk.
  final int sizeBytes;

  bool get isBusy =>
      status == VoiceInstallStatus.downloading ||
      status == VoiceInstallStatus.extracting;
}

/// Owns the one-time download, extraction and removal of the voice model.
class VoiceModelNotifier extends StateNotifier<VoiceInstallState> {
  VoiceModelNotifier(this._store) : super(const VoiceInstallState()) {
    refresh();
  }

  final VoiceModelStore _store;
  CancelToken? _cancelToken;

  /// Re-reads what is actually on disk.
  Future<void> refresh() async {
    final model = await _store.installed();
    if (!mounted) return;
    if (model == null) {
      state = const VoiceInstallState();
      return;
    }
    state = VoiceInstallState(
      status: VoiceInstallStatus.installed,
      sizeBytes: await _store.installedSize(),
    );
  }

  Future<bool> download() async {
    if (state.isBusy) return false;
    _cancelToken = CancelToken();
    state = const VoiceInstallState(status: VoiceInstallStatus.downloading);

    try {
      final bytes = await api.downloadVoiceArchive(
        cancelToken: _cancelToken,
        onProgress: (progress) {
          if (!mounted || state.status != VoiceInstallStatus.downloading)
            return;
          state = VoiceInstallState(
            status: VoiceInstallStatus.downloading,
            progress: progress,
          );
        },
      );
      if (!mounted) return false;

      state = const VoiceInstallState(status: VoiceInstallStatus.extracting);
      final model = await _store.install(bytes);
      if (!mounted) return false;

      if (model == null) {
        state = const VoiceInstallState(
          status: VoiceInstallStatus.failed,
          error: 'The downloaded voice archive was not in the expected format.',
        );
        return false;
      }

      state = VoiceInstallState(
        status: VoiceInstallStatus.installed,
        sizeBytes: await _store.installedSize(),
      );
      return true;
    } on DioException catch (e) {
      if (!mounted) return false;
      state = VoiceInstallState(
        status: VoiceInstallStatus.failed,
        error: _messageFor(e),
      );
      return false;
    } on Object catch (e) {
      debugPrint('Voice install failed: $e');
      if (!mounted) return false;
      // A failed extract can leave a partial directory behind.
      await _store.remove();
      if (!mounted) return false;
      state = const VoiceInstallState(
        status: VoiceInstallStatus.failed,
        error: 'The voice could not be installed. Please try again.',
      );
      return false;
    } finally {
      _cancelToken = null;
    }
  }

  /// Turns a transport failure into something the reader can act on.
  ///
  /// A reachable server that has no voice file is a very different problem
  /// from an unreachable one, and saying "check your connection" for a 404
  /// sends people to fix the wrong thing.
  static String _messageFor(DioException e) {
    if (CancelToken.isCancel(e)) return 'Download cancelled.';

    final status = e.response?.statusCode;
    if (status == 404) {
      return 'The reading voice is not available on the server yet. '
          'Please contact support or try again later.';
    }
    if (status != null && status >= 500) {
      return 'The server had a problem sending the voice ($status). '
          'Please try again later.';
    }
    if (status != null) {
      return 'The server refused the download ($status).';
    }
    return 'Could not reach the server. Check your connection and the '
        'server URL in Settings.';
  }

  void cancel() => _cancelToken?.cancel('cancelled by user');

  Future<void> remove() async {
    await _store.remove();
    if (!mounted) return;
    state = const VoiceInstallState();
  }

  @override
  void dispose() {
    _cancelToken?.cancel('disposed');
    super.dispose();
  }
}

final voiceModelStoreProvider =
    Provider<VoiceModelStore>((ref) => createVoiceModelStore());

final voiceModelProvider =
    StateNotifierProvider<VoiceModelNotifier, VoiceInstallState>(
  (ref) => VoiceModelNotifier(ref.watch(voiceModelStoreProvider)),
);
