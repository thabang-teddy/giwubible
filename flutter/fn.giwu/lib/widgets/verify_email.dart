import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/auth_provider.dart';
import '../theme.dart';

/// What came of asking for another verification link.
enum ResendOutcome { sent, alreadyVerified, failed }

/// Asks the server for another link and reports what happened.
///
/// The link itself always opens the web app — signing a URL is the server's
/// business — so nothing here can confirm an address. What it can do is get a
/// fresh link sent and notice when the reader has already followed one
/// somewhere else.
Future<ResendOutcome> _resend(WidgetRef ref) async {
  try {
    final alreadyVerified =
        await ref.read(authProvider.notifier).resendVerificationEmail();
    return alreadyVerified ? ResendOutcome.alreadyVerified : ResendOutcome.sent;
  } on Exception {
    return ResendOutcome.failed;
  }
}

/// The status line under a resend button: what just happened, if anything.
class _ResendStatus extends StatelessWidget {
  const _ResendStatus(this.outcome);

  final ResendOutcome? outcome;

  @override
  Widget build(BuildContext context) {
    return switch (outcome) {
      ResendOutcome.sent => const Padding(
          padding: EdgeInsets.only(top: 12),
          child: Text(
            'Sent. It can take a minute to arrive.',
            style: TextStyle(fontSize: 12, color: kMuted),
          ),
        ),
      ResendOutcome.failed => Padding(
          padding: const EdgeInsets.only(top: 12),
          child: Text(
            'Could not send the link. Please try again.',
            style: TextStyle(
              fontSize: 12,
              color: Theme.of(context).colorScheme.error,
            ),
          ),
        ),
      _ => const SizedBox.shrink(),
    };
  }
}

/// Explains why a bookmark was refused, and offers another link.
/// Note the unused [ref]: the dialog deliberately does not keep it. The
/// widget that opens this can be disposed while the dialog is still up — the
/// bookmarks page swaps its unverified state for the list the moment the
/// address is confirmed — and a `ref` from a disposed element throws on use.
/// The dialog is a consumer in its own right instead.
Future<void> showVerifyEmailDialog(
  BuildContext context,
  WidgetRef ref, {
  required String email,
}) {
  return showDialog<void>(
    context: context,
    builder: (_) => _VerifyEmailDialog(email: email),
  );
}

class _VerifyEmailDialog extends ConsumerStatefulWidget {
  const _VerifyEmailDialog({required this.email});

  final String email;

  @override
  ConsumerState<_VerifyEmailDialog> createState() => _VerifyEmailDialogState();
}

class _VerifyEmailDialogState extends ConsumerState<_VerifyEmailDialog> {
  bool _sending = false;
  ResendOutcome? _outcome;

  Future<void> _send() async {
    setState(() {
      _sending = true;
      _outcome = null;
    });
    final outcome = await _resend(ref);
    if (!mounted) return;
    setState(() {
      _sending = false;
      _outcome = outcome;
    });
  }

  @override
  Widget build(BuildContext context) {
    final settled = _outcome == ResendOutcome.alreadyVerified;

    return AlertDialog(
      title: const Text('Confirm your email'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            settled
                ? 'Your email is already confirmed — try that bookmark again.'
                : 'Bookmarks are saved to your Giwu account, so we need to '
                    'know the address is yours. Open the link we sent to '
                    '${widget.email}.',
          ),
          _ResendStatus(_outcome),
          const SizedBox(height: 12),
          const Text(
            'Reading needs no account, so nothing else is held up by this.',
            style: TextStyle(fontSize: 12, color: kMuted),
          ),
        ],
      ),
      actions: settled
          ? [
              TextButton(
                onPressed: () => Navigator.of(context).pop(),
                child: const Text('Done'),
              ),
            ]
          : [
              TextButton(
                onPressed: () => Navigator.of(context).pop(),
                child: const Text('Not now'),
              ),
              TextButton(
                onPressed: _sending ? null : _send,
                child: Text(_sending ? 'Sending…' : 'Send the link again'),
              ),
            ],
    );
  }
}

/// The notice that replaces the sign-up form once an account exists.
///
/// Carrying on into the reader is the primary action: the account is created
/// and usable, and only bookmarks wait on the link.
class RegisteredNotice extends ConsumerStatefulWidget {
  const RegisteredNotice({
    super.key,
    required this.email,
    required this.onContinue,
  });

  final String email;
  final VoidCallback onContinue;

  @override
  ConsumerState<RegisteredNotice> createState() => _RegisteredNoticeState();
}

class _RegisteredNoticeState extends ConsumerState<RegisteredNotice> {
  bool _sending = false;
  ResendOutcome? _outcome;

  Future<void> _send() async {
    setState(() {
      _sending = true;
      _outcome = null;
    });
    final outcome = await _resend(ref);
    if (!mounted) return;
    setState(() {
      _sending = false;
      _outcome = outcome;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          'Confirm your email',
          style: Theme.of(context).textTheme.titleMedium,
        ),
        const SizedBox(height: 8),
        Text(
          'Your account is ready. Open the link we sent to ${widget.email} '
          'and your bookmarks will follow you to any device.',
        ),
        const SizedBox(height: 6),
        const Text(
          'Reading needs no account at all, so you can start now and confirm '
          'later.',
          style: TextStyle(fontSize: 12, color: kMuted),
        ),
        _ResendStatus(_outcome),
        const SizedBox(height: 20),
        FilledButton(
          onPressed: widget.onContinue,
          child: const Text('Start reading'),
        ),
        const SizedBox(height: 4),
        TextButton(
          // Nothing left to send once the server has told us the address is
          // confirmed, so the button stops being an action.
          onPressed: _sending || _outcome == ResendOutcome.alreadyVerified
              ? null
              : _send,
          child: Text(
            _outcome == ResendOutcome.alreadyVerified
                ? 'Already confirmed'
                : _sending
                    ? 'Sending…'
                    : 'Send the link again',
          ),
        ),
      ],
    );
  }
}

/// Stands in for the bookmark list while the address is unconfirmed — the
/// list request itself is refused, so without this the page would claim there
/// are no bookmarks.
class UnverifiedBookmarksState extends ConsumerWidget {
  const UnverifiedBookmarksState({
    super.key,
    required this.email,
    required this.onBack,
  });

  final String email;
  final VoidCallback onBack;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.bookmark_border, size: 48, color: kMuted),
            const SizedBox(height: 16),
            const Text(
              'Confirm your email to save bookmarks',
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 6),
            const Text(
              'We sent you a link. Opening it lets your bookmarks follow you '
              'to any device.',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 12, color: kMuted),
            ),
            const SizedBox(height: 20),
            FilledButton(
              onPressed: () =>
                  showVerifyEmailDialog(context, ref, email: email),
              child: const Text('Send the link again'),
            ),
            const SizedBox(height: 4),
            TextButton(
              onPressed: onBack,
              child: const Text('Back to reading'),
            ),
          ],
        ),
      ),
    );
  }
}
