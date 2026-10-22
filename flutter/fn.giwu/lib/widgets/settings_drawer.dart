import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/bible.dart';
import '../pages/bookmarks_page.dart';
import '../pages/login_page.dart';
import '../pages/settings_page.dart';
import '../providers/auth_provider.dart';
import '../providers/prefs_provider.dart';
import '../theme.dart';

/// The reader's settings, in the drawer that slides in from the right.
///
/// Reset, the theme switch and the translation picker used to sit in the app
/// bar as loose icons. They live here now, in the order a reader asks for
/// them: who you are, how it looks, what you are reading, and the way back to
/// Genesis 1. The full Settings page stays one row away for the rest.
class SettingsDrawer extends ConsumerWidget {
  const SettingsDrawer({
    super.key,
    required this.bibles,
    required this.onSelectBible,
    required this.onReset,
  });

  final List<BibleModel> bibles;
  final void Function(String table) onSelectBible;
  final VoidCallback onReset;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authProvider);
    final isDark = ref.watch(darkModeProvider);
    final primaryBible = ref.watch(primaryBibleProvider);
    final cs = Theme.of(context).colorScheme;

    // Held before the drawer closes: popping it unmounts this widget, so a
    // callback that outlives the pop — the login page's onSuccess — cannot go
    // looking for a Navigator through a dead context.
    final navigator = Navigator.of(context);

    void closeThen(VoidCallback action) {
      navigator.pop();
      action();
    }

    return Drawer(
      child: SafeArea(
        child: ListView(
          padding: EdgeInsets.zero,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 14, 8, 6),
              child: Row(
                children: [
                  const Expanded(
                    child: Text(
                      'SETTINGS',
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                        letterSpacing: 1.4,
                      ),
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close, size: 18, color: kMuted),
                    tooltip: 'Close settings',
                    onPressed: () => navigator.pop(),
                  ),
                ],
              ),
            ),
            const Divider(height: 1),

            // ── Account ──────────────────────────────────────────────────
            const _SectionLabel('ACCOUNT'),
            if (user != null) ...[
              ListTile(
                leading: const Icon(Icons.account_circle_outlined),
                title: Text(user.name),
                subtitle: Text(user.email, overflow: TextOverflow.ellipsis),
              ),
              ListTile(
                leading: const Icon(Icons.bookmark_border),
                title: const Text('Bookmarks'),
                trailing: const Icon(Icons.chevron_right, size: 18),
                onTap: () => closeThen(() => navigator.push(
                      MaterialPageRoute<void>(
                        builder: (_) => const BookmarksPage(),
                      ),
                    )),
              ),
              ListTile(
                leading: const Icon(Icons.logout),
                title: const Text('Sign out'),
                onTap: () => closeThen(
                  () => ref.read(authProvider.notifier).logout(),
                ),
              ),
            ] else ...[
              ListTile(
                leading: const Icon(Icons.login),
                title: const Text('Sign in'),
                subtitle: const Text('Sync your bookmarks across devices'),
                trailing: const Icon(Icons.chevron_right, size: 18),
                onTap: () => closeThen(() => _openLogin(navigator)),
              ),
              ListTile(
                leading: const Icon(Icons.person_add_alt),
                title: const Text('Sign up'),
                subtitle: const Text('Reading itself needs no account'),
                trailing: const Icon(Icons.chevron_right, size: 18),
                onTap: () => closeThen(
                  () => _openLogin(navigator, startInRegister: true),
                ),
              ),
            ],
            const Divider(height: 1),

            // ── Theme ────────────────────────────────────────────────────
            const _SectionLabel('THEME'),
            SwitchListTile(
              secondary: const Icon(Icons.dark_mode_outlined),
              title: const Text('Dark mode'),
              value: isDark,
              onChanged: (_) => ref.read(darkModeProvider.notifier).toggle(),
            ),
            const Divider(height: 1),

            // ── Main bible ───────────────────────────────────────────────
            // Translations that are not on the device are listed but not
            // selectable; seeing what else exists is the point, and the
            // Settings page is where they are fetched.
            const _SectionLabel('MAIN BIBLE'),
            for (final bible in bibles)
              ListTile(
                dense: true,
                enabled: bible.downloaded,
                selected: bible.table == primaryBible,
                leading: Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(
                    color: bible.downloaded
                        ? cs.primaryContainer
                        : cs.surfaceContainerHigh,
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: Text(
                    bible.abbreviation,
                    style: TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.w700,
                      color: bible.downloaded ? cs.onPrimaryContainer : kMuted,
                    ),
                  ),
                ),
                title: Text(
                  bible.version,
                  style: const TextStyle(fontSize: 13),
                ),
                trailing: bible.table == primaryBible
                    ? Icon(Icons.check, color: cs.primary, size: 16)
                    : !bible.downloaded
                        ? const Icon(Icons.download_outlined,
                            size: 14, color: kMuted)
                        : null,
                onTap: bible.downloaded
                    ? () => closeThen(() => onSelectBible(bible.table))
                    : null,
              ),
            const Divider(height: 1),

            // ── Reset ────────────────────────────────────────────────────
            const _SectionLabel('RESET'),
            ListTile(
              leading: const Icon(Icons.refresh_rounded),
              title: const Text('Back to Genesis 1 (KJV)'),
              subtitle: const Text(
                'Clears the verse you have open and starts the reader over',
              ),
              onTap: () => closeThen(onReset),
            ),
            const Divider(height: 1),

            ListTile(
              leading: const Icon(Icons.settings_outlined),
              title: const Text('All settings'),
              subtitle: const Text('Read aloud, server and downloads'),
              trailing: const Icon(Icons.chevron_right, size: 18),
              onTap: () => closeThen(() => navigator.push(
                    MaterialPageRoute<void>(
                      builder: (_) => const SettingsPage(),
                    ),
                  )),
            ),
          ],
        ),
      ),
    );
  }

  void _openLogin(NavigatorState navigator, {bool startInRegister = false}) {
    navigator.push(
      MaterialPageRoute<void>(
        builder: (_) => LoginPage(
          startInRegister: startInRegister,
          onSuccess: navigator.pop,
        ),
      ),
    );
  }
}

class _SectionLabel extends StatelessWidget {
  const _SectionLabel(this.label);

  final String label;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 18, 16, 4),
      child: Text(
        label,
        style: const TextStyle(
          fontSize: 10,
          fontWeight: FontWeight.w700,
          letterSpacing: 1.3,
          color: kMuted,
        ),
      ),
    );
  }
}
