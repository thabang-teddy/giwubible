import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/voice_model_provider.dart';
import '../theme.dart';

/// Approximate download size, shown before the user commits to it.
const String kVoiceDownloadSize = '63 MB';

/// Offers the one-time voice download and reports whether it succeeded.
Future<bool> showVoiceDownloadSheet(BuildContext context) async {
  final installed = await showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    isDismissible: false,
    enableDrag: false,
    builder: (_) => const VoiceDownloadSheet(),
  );
  return installed ?? false;
}

class VoiceDownloadSheet extends ConsumerWidget {
  const VoiceDownloadSheet({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final install = ref.watch(voiceModelProvider);
    final notifier = ref.read(voiceModelProvider.notifier);

    // Close as soon as the voice is ready.
    ref.listen<VoiceInstallState>(voiceModelProvider, (_, next) {
      if (next.status == VoiceInstallStatus.installed &&
          Navigator.of(context).canPop()) {
        Navigator.of(context).pop(true);
      }
    });

    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 20, 24, 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  Icons.record_voice_over_outlined,
                  color: Theme.of(context).colorScheme.primary,
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    'Download the reading voice',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              'Reading aloud runs entirely on your device. The voice is a '
              '$kVoiceDownloadSize one-time download — after that it works '
              'with no internet connection at all.',
              style: const TextStyle(fontSize: 13, height: 1.5, color: kMuted),
            ),
            const SizedBox(height: 20),
            if (install.isBusy)
              _Progress(install: install)
            else if (install.status == VoiceInstallStatus.failed)
              _Failure(message: install.error),
            const SizedBox(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                TextButton(
                  onPressed: () {
                    if (install.isBusy) notifier.cancel();
                    Navigator.of(context).pop(false);
                  },
                  child: Text(install.isBusy ? 'Cancel' : 'Not now'),
                ),
                const SizedBox(width: 8),
                FilledButton.icon(
                  onPressed: install.isBusy ? null : notifier.download,
                  icon: const Icon(Icons.download_rounded, size: 18),
                  label: Text(
                    install.status == VoiceInstallStatus.failed
                        ? 'Try again'
                        : 'Download',
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _Progress extends StatelessWidget {
  const _Progress({required this.install});

  final VoiceInstallState install;

  @override
  Widget build(BuildContext context) {
    final isExtracting = install.status == VoiceInstallStatus.extracting;
    final progress = install.progress;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        LinearProgressIndicator(
          // Extraction has no measurable progress, so it stays indeterminate.
          value: isExtracting ? null : progress,
        ),
        const SizedBox(height: 8),
        Text(
          isExtracting
              ? 'Unpacking the voice…'
              : progress == null
                  ? 'Downloading…'
                  : 'Downloading… ${(progress * 100).round()}%',
          style: const TextStyle(fontSize: 12, color: kMuted),
        ),
      ],
    );
  }
}

class _Failure extends StatelessWidget {
  const _Failure({required this.message});

  final String? message;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: cs.errorContainer,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.error_outline, size: 18, color: cs.onErrorContainer),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              message ?? 'The voice could not be installed.',
              style: TextStyle(fontSize: 12, color: cs.onErrorContainer),
            ),
          ),
        ],
      ),
    );
  }
}
