import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';

import 'api/client.dart';
import 'api/download.dart';
import 'data/bible_database.dart';
import 'pages/read_page.dart';
import 'pages/welcome_page.dart';
import 'providers/database_provider.dart';
import 'providers/prefs_provider.dart';
import 'providers/server_url_provider.dart';
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

  // Load the stored server URL (null means the user has never changed it).
  // Apply it to both Dio clients before the widget tree is built so every
  // API call uses the correct base URL from the first request.
  final storedUrl = await bibleDb.getServerUrl();
  final serverUrl = storedUrl ?? kDefaultBaseUrl;
  setApiBaseUrl(serverUrl);
  setDownloadBaseUrl(serverUrl);

  runApp(
    ProviderScope(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        bibleDatabaseProvider.overrideWithValue(bibleDb),
        serverUrlProvider.overrideWith(
          (ref) => ServerUrlNotifier(bibleDb, serverUrl),
        ),
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
