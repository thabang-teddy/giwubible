import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/verse.dart';
import '../providers/prefs_provider.dart';
import '../providers/tts_provider.dart';
import '../services/tts_service.dart';
import 'voice_download_sheet.dart';

/// Floating control for reading the open chapter aloud.
///
/// Collapsed to a single button while idle; expands into a transport bar
/// (previous / play-pause / next / stop / speed) once playback starts.
class ReadAloudFab extends ConsumerWidget {
  const ReadAloudFab({super.key, required this.verses});

  /// Verses of the chapter on screen. Null while the chapter is still loading.
  final List<VerseModel>? verses;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final chapter = verses;
    if (chapter == null || chapter.isEmpty) return const SizedBox.shrink();

    final readiness = ref.watch(ttsReadinessProvider).valueOrNull;
    // Hide only where the voice cannot work at all. A missing voice model is
    // an offer to download, not a reason to hide the feature.
    if (readiness == null || readiness == TtsReadiness.unsupported) {
      return const SizedBox.shrink();
    }

    final playback = ref.watch(ttsControllerProvider);

    if (!playback.isActive) {
      return FloatingActionButton(
        heroTag: 'read-aloud',
        tooltip: 'Read chapter aloud',
        onPressed: () => _start(context, ref, chapter, readiness),
        child: const Icon(Icons.volume_up_rounded),
      );
    }

    return _TransportBar(
      isPaused: playback.status == TtsStatus.paused,
      rate: ref.watch(speechRateProvider),
      onPrevious: () => ref.read(ttsControllerProvider.notifier).previous(),
      onNext: () => ref.read(ttsControllerProvider.notifier).next(),
      onStop: () => ref.read(ttsControllerProvider.notifier).stop(),
      onPlayPause: () {
        final notifier = ref.read(ttsControllerProvider.notifier);
        playback.status == TtsStatus.paused
            ? notifier.resume()
            : notifier.pause();
      },
      onCycleRate: () => ref.read(speechRateProvider.notifier).cycle(),
    );
  }

  Future<void> _start(
    BuildContext context,
    WidgetRef ref,
    List<VerseModel> chapter,
    TtsReadiness readiness,
  ) async {
    if (readiness == TtsReadiness.needsVoiceModel) {
      final installed = await showVoiceDownloadSheet(context);
      if (!installed || !context.mounted) return;
      // Let the readiness probe re-run against the freshly installed voice.
      final refreshed = await ref.read(ttsReadinessProvider.future);
      if (refreshed != TtsReadiness.ready || !context.mounted) return;
    }

    // Begin at the verse the reader last tapped, if any.
    final from = ref.read(activeVerseProvider);
    await ref
        .read(ttsControllerProvider.notifier)
        .start(chapter, fromVerse: from);
  }
}

// ── Transport bar ──────────────────────────────────────────────────────────

class _TransportBar extends StatelessWidget {
  const _TransportBar({
    required this.isPaused,
    required this.rate,
    required this.onPrevious,
    required this.onPlayPause,
    required this.onNext,
    required this.onStop,
    required this.onCycleRate,
  });

  final bool isPaused;
  final double rate;
  final VoidCallback onPrevious;
  final VoidCallback onPlayPause;
  final VoidCallback onNext;
  final VoidCallback onStop;
  final VoidCallback onCycleRate;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;

    return Material(
      elevation: 6,
      color: cs.primary,
      shape: const StadiumBorder(),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            _BarButton(
              icon: Icons.skip_previous_rounded,
              tooltip: 'Previous verse',
              onPressed: onPrevious,
              color: cs.onPrimary,
            ),
            _BarButton(
              icon: isPaused ? Icons.play_arrow_rounded : Icons.pause_rounded,
              tooltip: isPaused ? 'Resume' : 'Pause',
              onPressed: onPlayPause,
              color: cs.onPrimary,
              size: 26,
            ),
            _BarButton(
              icon: Icons.skip_next_rounded,
              tooltip: 'Next verse',
              onPressed: onNext,
              color: cs.onPrimary,
            ),
            // Speed
            Tooltip(
              message: 'Reading speed',
              child: InkWell(
                onTap: onCycleRate,
                customBorder: const StadiumBorder(),
                child: Padding(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
                  child: Text(
                    '${_formatRate(rate)}x',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                      color: cs.onPrimary,
                    ),
                  ),
                ),
              ),
            ),
            _BarButton(
              icon: Icons.stop_rounded,
              tooltip: 'Stop reading',
              onPressed: onStop,
              color: cs.onPrimary,
            ),
          ],
        ),
      ),
    );
  }

  static String _formatRate(double rate) =>
      rate == rate.roundToDouble() ? '${rate.toInt()}' : '$rate';
}

class _BarButton extends StatelessWidget {
  const _BarButton({
    required this.icon,
    required this.tooltip,
    required this.onPressed,
    required this.color,
    this.size = 20,
  });

  final IconData icon;
  final String tooltip;
  final VoidCallback onPressed;
  final Color color;
  final double size;

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: Icon(icon, size: size, color: color),
      tooltip: tooltip,
      onPressed: onPressed,
      padding: const EdgeInsets.all(8),
      constraints: const BoxConstraints(),
      visualDensity: VisualDensity.compact,
    );
  }
}
