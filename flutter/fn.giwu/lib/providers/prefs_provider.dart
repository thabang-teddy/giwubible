import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

// Overridden at app startup with the real SharedPreferences instance.
final sharedPreferencesProvider = Provider<SharedPreferences>(
  (ref) => throw UnimplementedError('SharedPreferences not initialised'),
);

// ── Dark mode ──────────────────────────────────────────────────────────────

class DarkModeNotifier extends StateNotifier<bool> {
  DarkModeNotifier(SharedPreferences prefs)
      : _prefs = prefs,
        super(prefs.getBool('giwu_dark') ?? false);

  final SharedPreferences _prefs;

  void toggle() {
    state = !state;
    _prefs.setBool('giwu_dark', state);
  }
}

final darkModeProvider = StateNotifierProvider<DarkModeNotifier, bool>(
  (ref) => DarkModeNotifier(ref.read(sharedPreferencesProvider)),
);

// ── Primary bible ──────────────────────────────────────────────────────────

class PrimaryBibleNotifier extends StateNotifier<String> {
  PrimaryBibleNotifier(SharedPreferences prefs)
      : _prefs = prefs,
        super(prefs.getString('giwu_bible') ?? 't_kjv');

  final SharedPreferences _prefs;

  void set(String table) {
    state = table;
    _prefs.setString('giwu_bible', table);
  }
}

final primaryBibleProvider =
    StateNotifierProvider<PrimaryBibleNotifier, String>(
  (ref) => PrimaryBibleNotifier(ref.read(sharedPreferencesProvider)),
);

// ── Selected book ──────────────────────────────────────────────────────────

class SelectedBookNotifier extends StateNotifier<int> {
  SelectedBookNotifier(SharedPreferences prefs)
      : _prefs = prefs,
        super(prefs.getInt('giwu_book') ?? 1);

  final SharedPreferences _prefs;

  void set(int book) {
    state = book;
    _prefs.setInt('giwu_book', book);
  }
}

final selectedBookProvider = StateNotifierProvider<SelectedBookNotifier, int>(
  (ref) => SelectedBookNotifier(ref.read(sharedPreferencesProvider)),
);

// ── Selected chapter ───────────────────────────────────────────────────────

class SelectedChapterNotifier extends StateNotifier<int> {
  SelectedChapterNotifier(SharedPreferences prefs)
      : _prefs = prefs,
        super(prefs.getInt('giwu_chapter') ?? 1);

  final SharedPreferences _prefs;

  void set(int chapter) {
    state = chapter;
    _prefs.setInt('giwu_chapter', chapter);
  }
}

final selectedChapterProvider =
    StateNotifierProvider<SelectedChapterNotifier, int>(
  (ref) => SelectedChapterNotifier(ref.read(sharedPreferencesProvider)),
);

// ── Active verse (not persisted) ───────────────────────────────────────────

final activeVerseProvider = StateProvider<int?>((ref) => null);

// ── Parallel bibles selection ──────────────────────────────────────────────

class ParallelBiblesNotifier extends StateNotifier<List<String>> {
  ParallelBiblesNotifier(SharedPreferences prefs)
      : _prefs = prefs,
        super(prefs.getStringList('giwu_parallel_bibles') ?? []);

  final SharedPreferences _prefs;

  bool isSelected(String table, List<String> allComparable) =>
      state.isEmpty || state.contains(table);

  void toggle(String table, List<String> allComparable) {
    final List<String> next;
    if (state.isEmpty) {
      next = allComparable.where((t) => t != table).toList();
    } else if (state.contains(table)) {
      final without = state.where((t) => t != table).toList();
      next = without.length == allComparable.length ? [] : without;
    } else {
      final added = [...state, table];
      next = added.length == allComparable.length ? [] : added;
    }
    state = next;
    _prefs.setStringList('giwu_parallel_bibles', state);
  }
}

final parallelBiblesProvider =
    StateNotifierProvider<ParallelBiblesNotifier, List<String>>(
  (ref) => ParallelBiblesNotifier(ref.read(sharedPreferencesProvider)),
);
