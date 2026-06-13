import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/database_provider.dart';
import '../providers/prefs_provider.dart';
import '../providers/server_url_provider.dart';
import 'welcome_page.dart';

class SettingsPage extends ConsumerWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isDark = ref.watch(darkModeProvider);
    final serverUrl = ref.watch(serverUrlProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
        leading: const BackButton(),
      ),
      body: ListView(
        children: [
          // ── Appearance ───────────────────────────────────────────────────
          const _SectionHeader(label: 'APPEARANCE'),
          SwitchListTile(
            secondary: Icon(
              isDark ? Icons.dark_mode_outlined : Icons.wb_sunny_outlined,
            ),
            title: const Text('Dark mode'),
            value: isDark,
            onChanged: (_) => ref.read(darkModeProvider.notifier).toggle(),
          ),
          const Divider(height: 1),

          // ── Connection ───────────────────────────────────────────────────
          const _SectionHeader(label: 'CONNECTION'),
          ListTile(
            leading: const Icon(Icons.dns_outlined),
            title: const Text('Server URL'),
            subtitle: Text(
              serverUrl,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 12),
            ),
            trailing: const Icon(Icons.edit_outlined, size: 18),
            onTap: () => _editServerUrl(context, ref, serverUrl),
          ),
          const Divider(height: 1),

          // ── Data ─────────────────────────────────────────────────────────
          const _SectionHeader(label: 'DATA'),
          ListTile(
            leading: const Icon(Icons.download_for_offline_outlined),
            title: const Text('Manage translations'),
            subtitle: const Text('Download or remove Bible versions'),
            trailing: const Icon(Icons.chevron_right, size: 18),
            onTap: () => _openWelcomePage(context),
          ),
          const Divider(height: 1),

          // ── Danger zone ───────────────────────────────────────────────────
          const _SectionHeader(label: 'DANGER ZONE'),
          ListTile(
            leading: Icon(
              Icons.delete_forever_outlined,
              color: Theme.of(context).colorScheme.error,
            ),
            title: Text(
              'Reset app',
              style: TextStyle(
                color: Theme.of(context).colorScheme.error,
              ),
            ),
            subtitle: const Text(
              'Removes all downloaded Bibles and returns to setup.',
            ),
            onTap: () => _confirmReset(context, ref),
          ),
        ],
      ),
    );
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  void _openWelcomePage(BuildContext context) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => const WelcomePage()),
    );
  }

  Future<void> _editServerUrl(
    BuildContext context,
    WidgetRef ref,
    String current,
  ) async {
    final result = await showDialog<String>(
      context: context,
      builder: (ctx) => _ServerUrlDialog(initial: current),
    );

    if (result == null) return;
    if (!context.mounted) return;

    await ref.read(serverUrlProvider.notifier).save(result);

    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Server URL updated.')),
    );
  }

  Future<void> _confirmReset(BuildContext context, WidgetRef ref) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Reset app?'),
        content: const Text(
          'This will delete all downloaded Bibles from this device. '
          'You will need to download them again.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: Theme.of(ctx).colorScheme.error,
              foregroundColor: Theme.of(ctx).colorScheme.onError,
            ),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Reset'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;
    if (!context.mounted) return;

    final db = ref.read(bibleDatabaseProvider);
    await db.resetDatabase();

    if (!context.mounted) return;

    // Return to WelcomePage, clearing the entire navigation stack.
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute<void>(builder: (_) => const WelcomePage()),
      (_) => false,
    );
  }
}

// ── Server URL edit dialog ─────────────────────────────────────────────────

class _ServerUrlDialog extends StatefulWidget {
  const _ServerUrlDialog({required this.initial});

  final String initial;

  @override
  State<_ServerUrlDialog> createState() => _ServerUrlDialogState();
}

class _ServerUrlDialogState extends State<_ServerUrlDialog> {
  late final TextEditingController _controller;
  final _formKey = GlobalKey<FormState>();

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.initial);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  String? _validate(String? value) {
    if (value == null || value.trim().isEmpty) {
      return 'URL cannot be empty.';
    }
    final uri = Uri.tryParse(value.trim());
    if (uri == null || !uri.isAbsolute) {
      return 'Enter a valid URL.';
    }
    if (uri.scheme != 'http' && uri.scheme != 'https') {
      return 'URL must start with http:// or https://';
    }
    return null;
  }

  void _submit() {
    if (_formKey.currentState?.validate() != true) return;
    Navigator.of(context).pop(_controller.text.trim());
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Server URL'),
      content: Form(
        key: _formKey,
        child: TextFormField(
          controller: _controller,
          autofocus: true,
          keyboardType: TextInputType.url,
          decoration: const InputDecoration(
            labelText: 'Base URL',
            hintText: 'https://api.giwu.test/api/',
            helperText: 'Include the trailing slash.',
          ),
          validator: _validate,
          onFieldSubmitted: (_) => _submit(),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: _submit,
          child: const Text('Save'),
        ),
      ],
    );
  }
}

// ── Section header ─────────────────────────────────────────────────────────

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 20, 16, 6),
      child: Text(
        label,
        style: TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.w700,
          letterSpacing: 0.8,
          color: Theme.of(context).colorScheme.primary,
        ),
      ),
    );
  }
}
