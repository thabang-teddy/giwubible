import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';

import 'data/bible_database.dart';
import 'pages/read_page.dart';
import 'pages/welcome_page.dart';
import 'providers/database_provider.dart';
import 'providers/prefs_provider.dart';
import 'theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  if (!kIsWeb && (Platform.isWindows || Platform.isLinux || Platform.isMacOS)) {
    sqfliteFfiInit();
    databaseFactory = databaseFactoryFfi;
  }

  final prefs = await SharedPreferences.getInstance();
  final bibleDb = await BibleDatabase.open();
  final setupDone = await bibleDb.isSetupComplete();

  runApp(
    ProviderScope(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        bibleDatabaseProvider.overrideWithValue(bibleDb),
      ],
      child: GiwuApp(showWelcome: !setupDone),
    ),
  );
}

class GiwuApp extends ConsumerWidget {
  const GiwuApp({super.key, required this.showWelcome});

  final bool showWelcome;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isDark = ref.watch(darkModeProvider);
    return MaterialApp(
      title: 'Giwu Bible',
      debugShowCheckedModeBanner: false,
      theme: buildTheme(Brightness.light),
      darkTheme: buildTheme(Brightness.dark),
      themeMode: isDark ? ThemeMode.dark : ThemeMode.light,
      home: showWelcome ? const WelcomePage() : const ReadPage(),
    );
  }
}
