import 'dart:math';

import 'package:path/path.dart';
import 'package:path_provider/path_provider.dart';
import 'package:sqflite/sqflite.dart';

import '../models/bible.dart';
import '../models/book.dart';
import '../models/verse.dart';
import 'bible_versions.dart';
import 'book_names.dart';

final _tableNameRe = RegExp(r'^t_[a-z0-9]+$');

class BibleDatabase {
  BibleDatabase._(this._db);

  final Database _db;

  // ── Lifecycle ──────────────────────────────────────────────────────────────

  static Future<BibleDatabase> open() async {
    final dir = await getApplicationDocumentsDirectory();
    final dbPath = join(dir.path, 'giwu_bible.db');

    final db = await openDatabase(
      dbPath,
      version: 1,
      onCreate: _createSchema,
    );

    // Ensure app_settings exists on every open — CREATE IF NOT EXISTS is
    // idempotent and backfills existing databases created before this table
    // was introduced (onCreate only fires on first creation).
    await db.execute('''
      CREATE TABLE IF NOT EXISTS app_settings (
        key   TEXT PRIMARY KEY,
        value TEXT NOT NULL
      )
    ''');

    // Run seeds on every open using INSERT OR IGNORE so they are safe to
    // call repeatedly and also backfill existing databases that were created
    // before seeding was introduced (onCreate only fires on first creation).
    await _seedBibles(db);
    await _seedBooks(db);

    return BibleDatabase._(db);
  }

  static Future<void> _createSchema(Database db, int version) async {
    await db.execute('''
      CREATE TABLE IF NOT EXISTS bible_version_key (
        "table"      TEXT PRIMARY KEY,
        abbreviation TEXT NOT NULL,
        version      TEXT NOT NULL,
        downloaded   INTEGER NOT NULL DEFAULT 0
      )
    ''');
    await db.execute('''
      CREATE TABLE IF NOT EXISTS key_english (
        b INTEGER PRIMARY KEY,
        n TEXT    NOT NULL,
        t TEXT
      )
    ''');
    await db.execute('''
      CREATE TABLE IF NOT EXISTS app_settings (
        key   TEXT PRIMARY KEY,
        value TEXT NOT NULL
      )
    ''');
    await _seedBibles(db);
    await _seedBooks(db);
  }

  /// Inserts the canonical bible version list into [bible_version_key].
  /// Uses INSERT OR IGNORE so existing rows (and their downloaded flags) are
  /// kept intact — this is safe to call on both fresh installs and resets.
  static Future<void> _seedBibles(Database db) async {
    final batch = db.batch();
    for (final bible in kBibleVersions) {
      batch.rawInsert(
        'INSERT OR IGNORE INTO bible_version_key ("table", abbreviation, version, downloaded) '
        'VALUES (?, ?, ?, 0)',
        [bible.table, bible.abbreviation, bible.version],
      );
    }
    await batch.commit(noResult: true);
  }

  /// Inserts the canonical 66-book list into [key_english].
  /// Uses INSERT OR IGNORE so existing rows (e.g. from a user sync) are kept.
  static Future<void> _seedBooks(Database db) async {
    final batch = db.batch();
    for (final book in kBibleBooks) {
      batch.rawInsert(
        'INSERT OR IGNORE INTO key_english (b, n, t) VALUES (?, ?, ?)',
        [book.b, book.n, book.t],
      );
    }
    await batch.commit(noResult: true);
  }

  // ── Setup status ───────────────────────────────────────────────────────────

  Future<bool> isSetupComplete() async {
    final rows = await _db.rawQuery(
      'SELECT COUNT(*) AS cnt FROM bible_version_key WHERE downloaded = 1',
    );
    return (rows.first['cnt'] as int) > 0;
  }

  Future<bool> isBibleDownloaded(String table) async {
    final rows = await _db.rawQuery(
      'SELECT downloaded FROM bible_version_key WHERE "table" = ? LIMIT 1',
      [table],
    );
    if (rows.isEmpty) return false;
    return (rows.first['downloaded'] as int) == 1;
  }

  // ── Bibles ─────────────────────────────────────────────────────────────────

  Future<List<BibleModel>> getBibles() async {
    final rows = await _db.rawQuery(
      'SELECT "table", abbreviation, version, downloaded FROM bible_version_key ORDER BY abbreviation ASC',
    );
    return rows.map(_rowToBible).toList();
  }

  Future<void> saveBiblesList(List<BibleModel> bibles) async {
    final batch = _db.batch();
    for (final b in bibles) {
      // INSERT OR IGNORE so we never clobber an existing downloaded flag.
      batch.rawInsert(
        'INSERT OR IGNORE INTO bible_version_key ("table", abbreviation, version, downloaded) VALUES (?, ?, ?, 0)',
        [b.table, b.abbreviation, b.version],
      );
    }
    await batch.commit(noResult: true);
  }

  Future<void> markBibleDownloaded(String table, {required bool downloaded}) async {
    await _db.rawUpdate(
      'UPDATE bible_version_key SET downloaded = ? WHERE "table" = ?',
      [downloaded ? 1 : 0, table],
    );
  }

  // ── Books ──────────────────────────────────────────────────────────────────

  Future<List<BookModel>> getBooks() async {
    final rows = await _db.rawQuery(
      'SELECT b, n, t FROM key_english ORDER BY b ASC',
    );
    return rows
        .map(
          (r) => BookModel(
            b: r['b'] as int,
            n: r['n'] as String,
            t: r['t'] as String?,
          ),
        )
        .toList();
  }

  Future<void> saveBooks(List<BookModel> books) async {
    final batch = _db.batch();
    for (final book in books) {
      batch.rawInsert(
        'INSERT OR REPLACE INTO key_english (b, n, t) VALUES (?, ?, ?)',
        [book.b, book.n, book.t],
      );
    }
    await batch.commit(noResult: true);
  }

  // ── Verse tables ───────────────────────────────────────────────────────────

  Future<List<VerseModel>> getChapter(
    String bibleTable,
    int book,
    int chapter,
  ) async {
    _validateTableName(bibleTable);
    final rows = await _db.rawQuery(
      'SELECT b, c, v, t FROM $bibleTable WHERE b = ? AND c = ? ORDER BY v ASC',
      [book, chapter],
    );
    return rows.map(_rowToVerse).toList();
  }

  Future<ComparisonResult?> getVerse(
    String bibleTable,
    int book,
    int chapter,
    int verse,
  ) async {
    _validateTableName(bibleTable);

    final verseRows = await _db.rawQuery(
      'SELECT t FROM $bibleTable WHERE b = ? AND c = ? AND v = ? LIMIT 1',
      [book, chapter, verse],
    );
    if (verseRows.isEmpty) return null;

    final metaRows = await _db.rawQuery(
      'SELECT "table", abbreviation, version FROM bible_version_key WHERE "table" = ? LIMIT 1',
      [bibleTable],
    );
    if (metaRows.isEmpty) return null;

    final meta = metaRows.first;
    return ComparisonResult(
      bible: meta['table'] as String,
      version: meta['version'] as String,
      abbreviation: meta['abbreviation'] as String,
      text: verseRows.first['t'] as String,
    );
  }

  /// Creates the verse table for [bibleTable] and bulk-inserts [verses].
  /// Existing rows are replaced. Marks the bible as downloaded when done.
  Future<void> saveBibleVerses(
    String bibleTable,
    List<VerseModel> verses,
  ) async {
    _validateTableName(bibleTable);

    await _db.execute('''
      CREATE TABLE IF NOT EXISTS $bibleTable (
        b INTEGER NOT NULL,
        c INTEGER NOT NULL,
        v INTEGER NOT NULL,
        t TEXT    NOT NULL,
        PRIMARY KEY (b, c, v)
      )
    ''');

    const chunkSize = 500;
    for (var i = 0; i < verses.length; i += chunkSize) {
      final end = min(i + chunkSize, verses.length);
      final batch = _db.batch();
      for (final v in verses.sublist(i, end)) {
        batch.rawInsert(
          'INSERT OR REPLACE INTO $bibleTable (b, c, v, t) VALUES (?, ?, ?, ?)',
          [v.b, v.c, v.v, v.t],
        );
      }
      await batch.commit(noResult: true);
    }

    await markBibleDownloaded(bibleTable, downloaded: true);
  }

  // ── App settings ───────────────────────────────────────────────────────────

  /// Returns the stored server URL, or `null` if the user has never changed it.
  Future<String?> getServerUrl() => _getAppSetting('server_url');

  /// Persists [url] as the server URL. Replaces any previously stored value.
  Future<void> saveServerUrl(String url) async {
    await _db.rawInsert(
      "INSERT OR REPLACE INTO app_settings (key, value) VALUES ('server_url', ?)",
      [url],
    );
  }

  /// Resolves which bible to use as the primary bible when resetting.
  ///
  /// Priority:
  ///   1. `app_settings.default_bible` — explicit user preference.
  ///   2. `t_kjv` — if KJV has been downloaded.
  ///   3. First downloaded bible (alphabetical by abbreviation).
  ///   4. `t_kjv` — hard fallback when nothing is downloaded yet.
  Future<String> resolveDefaultBible() async {
    // 1. Explicit default saved in settings.
    final stored = await _getAppSetting('default_bible');
    if (stored != null) return stored;

    // 2. KJV if already downloaded.
    final kjvReady = await isBibleDownloaded('t_kjv');
    if (kjvReady) return 't_kjv';

    // 3. First downloaded bible.
    final rows = await _db.rawQuery(
      'SELECT "table" FROM bible_version_key '
      'WHERE downloaded = 1 ORDER BY abbreviation ASC LIMIT 1',
    );
    if (rows.isNotEmpty) return rows.first['table'] as String;

    // 4. Nothing downloaded — fall back to KJV (chapter shows empty state).
    return 't_kjv';
  }

  // ── Private helpers ── (app settings) ─────────────────────────────────────

  Future<String?> _getAppSetting(String key) async {
    final rows = await _db.rawQuery(
      'SELECT value FROM app_settings WHERE key = ? LIMIT 1',
      [key],
    );
    if (rows.isEmpty) return null;
    return rows.first['value'] as String;
  }

  // ── Reset ──────────────────────────────────────────────────────────────────

  /// Drops all downloaded verse tables and clears metadata.
  /// After this call [isSetupComplete] returns false.
  Future<void> resetDatabase() async {
    final tables = await _db.rawQuery(
      "SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 't_%'",
    );

    for (final row in tables) {
      final name = row['name'] as String;
      if (_tableNameRe.hasMatch(name)) {
        await _db.execute('DROP TABLE IF EXISTS $name');
      }
    }

    await _db.rawDelete('DELETE FROM bible_version_key');
    await _db.rawDelete('DELETE FROM key_english');

    // Restore bundled lists so the WelcomePage and sidebar work immediately
    // without any network call after a reset.
    await _seedBibles(_db);
    await _seedBooks(_db);
  }

  // ── Sync from uploaded SQLite file ─────────────────────────────────────────

  /// Opens [filePath] as a read-only SQLite database and copies all Bible
  /// verse tables it contains into this local database.
  ///
  /// Returns the list of Bible table names that were successfully synced.
  Future<List<String>> syncFromSqliteFile(
    String filePath, {
    void Function(String bibleTable, int current, int total)? onProgress,
  }) async {
    final source = await openDatabase(filePath, readOnly: true);
    final synced = <String>[];

    try {
      // Read bibles list from source.
      final sourceBibles = await _readSourceBiblesList(source);

      // Ensure those bibles are registered in our local metadata table.
      await saveBiblesList(sourceBibles);

      // Also sync book names if the source has them.
      final sourceBooks = await _readSourceBooks(source);
      if (sourceBooks.isNotEmpty) await saveBooks(sourceBooks);

      final total = sourceBibles.length;
      for (var i = 0; i < total; i++) {
        final bible = sourceBibles[i];
        final tableExists = await _tableExistsIn(source, bible.table);
        if (!tableExists) continue;

        onProgress?.call(bible.table, i + 1, total);

        final verseRows = await source.rawQuery(
          'SELECT b, c, v, t FROM ${bible.table} ORDER BY b, c, v',
        );

        final verses = verseRows.map(_rowToVerse).toList();
        await saveBibleVerses(bible.table, verses);
        synced.add(bible.table);
      }
    } finally {
      await source.close();
    }

    return synced;
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  static Future<List<BibleModel>> _readSourceBiblesList(Database db) async {
    try {
      final rows = await db.rawQuery(
        'SELECT "table", abbreviation, version FROM bible_version_key',
      );
      return rows.map(_rowToBible).toList();
    } catch (_) {
      return [];
    }
  }

  static Future<List<BookModel>> _readSourceBooks(Database db) async {
    try {
      final rows = await db.rawQuery(
        'SELECT b, n, t FROM key_english ORDER BY b ASC',
      );
      return rows
          .map(
            (r) => BookModel(
              b: r['b'] as int,
              n: r['n'] as String,
              t: r['t'] as String?,
            ),
          )
          .toList();
    } catch (_) {
      return [];
    }
  }

  static Future<bool> _tableExistsIn(Database db, String tableName) async {
    final rows = await db.rawQuery(
      "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
      [tableName],
    );
    return rows.isNotEmpty;
  }

  static BibleModel _rowToBible(Map<String, Object?> r) => BibleModel(
        table: r['table'] as String,
        abbreviation: r['abbreviation'] as String,
        version: r['version'] as String,
        downloaded: (r['downloaded'] as int? ?? 0) == 1,
      );

  static VerseModel _rowToVerse(Map<String, Object?> r) => VerseModel(
        b: r['b'] as int,
        c: r['c'] as int,
        v: r['v'] as int,
        t: r['t'] as String,
      );

  void _validateTableName(String table) {
    if (!_tableNameRe.hasMatch(table)) {
      throw ArgumentError('Invalid bible table name: $table');
    }
  }
}
