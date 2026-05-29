import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:sqflite/sqflite.dart';

import '../api/bibles.dart' as api;
import '../api/books.dart' as api;
import '../api/download.dart';
import '../data/bible_versions.dart';
import '../models/bible.dart';
import '../providers/database_provider.dart';
import 'read_page.dart';
import 'settings_page.dart';

class WelcomePage extends ConsumerStatefulWidget {
  const WelcomePage({super.key});

  @override
  ConsumerState<WelcomePage> createState() => _WelcomePageState();
}

class _WelcomePageState extends ConsumerState<WelcomePage> {
  List<BibleModel> _bibles = [];
  final Set<String> _selected = {};
  bool _loadingBibles = true;

  // Download / sync progress state
  bool _working = false;
  String _status = '';
  int _workDone = 0;
  int _workTotal = 0;

  @override
  void initState() {
    super.initState();
    _loadBiblesList();
  }

  // ── Load available bibles ─────────────────────────────────────────────────
  //
  // Priority order (each step is non-blocking for the user):
  //   1. Bundled constant  → cards appear instantly, zero async wait.
  //   2. Local DB          → overrides with real downloaded flags.
  //   3. Remote API        → refreshes list; failures are silent when we
  //                          already have something to show.

  Future<void> _loadBiblesList() async {
    setState(() => _loadingBibles = true);

    final db = ref.read(bibleDatabaseProvider);

    // ── Step 1: Show bundled list instantly (no async, always available) ──
    setState(() {
      _bibles = defaultBibles;
      _loadingBibles = false;
    });

    // ── Step 2: Overlay with local DB (adds downloaded flags) ─────────────
    try {
      final local = await db.getBibles();
      if (local.isNotEmpty && mounted) {
        setState(() {
          _bibles = local;
          _selected.addAll(
            local.where((b) => b.downloaded).map((b) => b.table),
          );
        });
      }
    } catch (_) {}

    // ── Step 3: Refresh from API (picks up new translations) ──────────────
    try {
      final apiBibles = await api.getBibles();
      await db.saveBiblesList(apiBibles);

      // Also refresh books while we have connectivity.
      try {
        final books = await api.getBooks();
        await db.saveBooks(books);
      } catch (_) {}

      // Reload from local to merge API data with existing downloaded flags.
      final updated = await db.getBibles();
      if (mounted) {
        setState(() {
          _bibles = updated;
          _selected.addAll(
            updated.where((b) => b.downloaded).map((b) => b.table),
          );
        });
      }
    } catch (_) {
      // API failed — cards are already shown from step 1/2, so stay silent.
    }
  }

  // ── Download selected bibles ───────────────────────────────────────────────

  Future<void> _downloadSelected() async {
    if (_selected.isEmpty) return;
    final db = ref.read(bibleDatabaseProvider);
    final toDownload =
        _selected.where((t) => !_bibles.any((b) => b.table == t && b.downloaded)).toList();

    if (toDownload.isEmpty) {
      _openReadPage();
      return;
    }

    setState(() {
      _working = true;
      _workTotal = toDownload.length;
      _workDone = 0;
    });

    for (final table in toDownload) {
      final bibleName = _bibles.firstWhere((b) => b.table == table).abbreviation;

      setState(() => _status = 'Downloading $bibleName…');

      try {
        final payload = await downloadBibleFromApi(table);
        setState(() => _status = 'Storing $bibleName…');
        // Upsert the bible metadata first so markBibleDownloaded (called
        // inside saveBibleVerses) always finds an existing row to UPDATE.
        // bible_version_key may be empty when the /api/bibles list call
        // failed or was never made (offline first launch).
        await db.saveBiblesList([payload.bible]);
        await db.saveBibleVerses(table, payload.verses);
        setState(() {
          _workDone++;
          _bibles = _bibles
              .map((b) => b.table == table ? b.copyWith(downloaded: true) : b)
              .toList();
        });
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Failed to download $bibleName: $e')),
          );
        }
      }
    }

    setState(() {
      _working = false;
      _status = '';
    });

    if (await db.isSetupComplete() && mounted) {
      _openReadPage();
    }
  }

  // ── Upload SQLite database ─────────────────────────────────────────────────

  Future<void> _uploadDatabase() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.any,
      allowMultiple: false,
    );
    if (result == null || result.files.single.path == null) return;

    final path = result.files.single.path!;
    final db = ref.read(bibleDatabaseProvider);

    // Quick validation: try opening it.
    Database? source;
    try {
      source = await openDatabase(path, readOnly: true);
      await source.rawQuery('SELECT 1 FROM bible_version_key LIMIT 1');
    } on Exception {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Invalid file — expected a Giwu Bible SQLite database.'),
          ),
        );
      }
      await source?.close();
      return;
    }
    await source.close();

    setState(() {
      _working = true;
      _status = 'Reading database…';
      _workDone = 0;
      _workTotal = 0;
    });

    try {
      final synced = await db.syncFromSqliteFile(
        path,
        onProgress: (table, current, total) {
          if (!mounted) return;
          setState(() {
            _status = 'Syncing ${table.replaceFirst('t_', '').toUpperCase()}…';
            _workDone = current;
            _workTotal = total;
          });
        },
      );

      // Refresh local list.
      final updated = await db.getBibles();
      setState(() {
        _bibles = updated;
        _selected.addAll(synced);
        _working = false;
        _status = '';
      });

      if (synced.isNotEmpty && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Synced ${synced.length} Bible(s) from file.'),
          ),
        );
      }
    } catch (e) {
      setState(() {
        _working = false;
        _status = '';
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Sync failed: $e')),
        );
      }
    }

    if (await db.isSetupComplete() && mounted) {
      _openReadPage();
    }
  }

  void _openReadPage() {
    Navigator.of(context).pushReplacement(
      MaterialPageRoute<void>(builder: (_) => const ReadPage()),
    );
  }

  // ── Build ──────────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      body: Stack(
        children: [
          _buildContent(theme, colorScheme),
          if (_working) _buildProgressOverlay(colorScheme),
        ],
      ),
    );
  }

  Widget _buildContent(ThemeData theme, ColorScheme cs) {
    return SafeArea(
      child: Column(
        children: [
          _buildHeader(theme, cs),
          Expanded(child: _buildBibleList(theme, cs)),
          _buildFooter(theme, cs),
        ],
      ),
    );
  }

  Widget _buildHeader(ThemeData theme, ColorScheme cs) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 32, 24, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.menu_book_rounded, size: 48, color: cs.primary),
          const SizedBox(height: 12),
          Text(
            'Giwu Bible',
            style: theme.textTheme.headlineMedium
                ?.copyWith(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 4),
          Text(
            'Choose Bible translations to download.',
            style: theme.textTheme.bodyMedium
                ?.copyWith(color: cs.onSurface.withValues(alpha: 0.6)),
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }

  Widget _buildBibleList(ThemeData theme, ColorScheme cs) {
    if (_loadingBibles) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_bibles.isEmpty) {
      return const Center(child: Text('No Bible versions found.'));
    }

    return GridView.builder(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
        maxCrossAxisExtent: 200,
        mainAxisSpacing: 12,
        crossAxisSpacing: 12,
        childAspectRatio: 1.4,
      ),
      itemCount: _bibles.length,
      itemBuilder: (_, i) => _BibleCard(
        bible: _bibles[i],
        selected: _selected.contains(_bibles[i].table),
        onTap: _working ? null : () => _toggleSelection(_bibles[i].table),
      ),
    );
  }

  Widget _buildFooter(ThemeData theme, ColorScheme cs) {
    final hasUndownloaded =
        _selected.any((t) => !_bibles.any((b) => b.table == t && b.downloaded));
    final canProceed =
        _selected.isNotEmpty && _bibles.any((b) => b.downloaded);
    final buttonLabel = hasUndownloaded
        ? 'Download${_selected.isNotEmpty ? ' (${_selected.length})' : ''}'
        : 'Continue';

    return Container(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Divider(),
          const SizedBox(height: 8),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: (_working || _selected.isEmpty) && !canProceed
                  ? null
                  : _downloadSelected,
              icon: const Icon(Icons.download_rounded),
              label: Text(buttonLabel),
            ),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              const Expanded(child: Divider()),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 12),
                child: Text(
                  'or',
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: cs.onSurface.withValues(alpha: 0.5)),
                ),
              ),
              const Expanded(child: Divider()),
            ],
          ),
          const SizedBox(height: 8),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: _working ? null : _uploadDatabase,
              icon: const Icon(Icons.upload_file_rounded),
              label: const Text('Upload SQLite Database'),
            ),
          ),
          const SizedBox(height: 4),
          Center(
            child: TextButton.icon(
              onPressed: _working
                  ? null
                  : () => Navigator.of(context).push(
                        MaterialPageRoute<void>(
                          builder: (_) => const SettingsPage(),
                        ),
                      ),
              icon: const Icon(Icons.settings_outlined, size: 14),
              label: const Text('Settings'),
              style: TextButton.styleFrom(
                foregroundColor: Theme.of(context)
                    .colorScheme
                    .onSurface
                    .withValues(alpha: 0.55),
                textStyle: const TextStyle(fontSize: 12),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildProgressOverlay(ColorScheme cs) {
    final percent = _workTotal > 0 ? _workDone / _workTotal : null;
    return Container(
      color: Colors.black54,
      child: Center(
        child: Card(
          margin: const EdgeInsets.symmetric(horizontal: 40),
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                CircularProgressIndicator(value: percent),
                const SizedBox(height: 16),
                Text(
                  _status.isEmpty ? 'Working…' : _status,
                  textAlign: TextAlign.center,
                ),
                if (_workTotal > 0) ...[
                  const SizedBox(height: 8),
                  Text(
                    '$_workDone / $_workTotal',
                    style: const TextStyle(
                      fontSize: 12,
                      color: Colors.grey,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }

  void _toggleSelection(String table) {
    setState(() {
      if (_selected.contains(table)) {
        _selected.remove(table);
      } else {
        _selected.add(table);
      }
    });
  }
}

// ── Bible selection card ─────────────────────────────────────────────────────

class _BibleCard extends StatelessWidget {
  const _BibleCard({
    required this.bible,
    required this.selected,
    required this.onTap,
  });

  final BibleModel bible;
  final bool selected;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final isDownloaded = bible.downloaded;

    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(12),
          color: selected ? cs.primaryContainer : cs.surface,
          border: Border.all(
            color: selected ? cs.primary : cs.outline.withValues(alpha: 0.4),
            width: selected ? 2 : 1,
          ),
        ),
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    bible.abbreviation,
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 18,
                      color:
                          selected ? cs.onPrimaryContainer : cs.onSurface,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                if (isDownloaded)
                  Icon(
                    Icons.check_circle_rounded,
                    size: 16,
                    color: selected ? cs.primary : cs.outline,
                  )
                else if (selected)
                  Icon(
                    Icons.download_rounded,
                    size: 16,
                    color: cs.primary,
                  ),
              ],
            ),
            const SizedBox(height: 4),
            Expanded(
              child: Text(
                bible.version,
                style: TextStyle(
                  fontSize: 11,
                  color: selected
                      ? cs.onPrimaryContainer.withValues(alpha: 0.8)
                      : cs.onSurface.withValues(alpha: 0.6),
                ),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
