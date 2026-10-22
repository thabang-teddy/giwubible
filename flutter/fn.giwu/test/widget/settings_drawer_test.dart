import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:fn_giwu/models/bible.dart';
import 'package:fn_giwu/models/user.dart';
import 'package:fn_giwu/pages/login_page.dart';
import 'package:fn_giwu/providers/auth_provider.dart';
import 'package:fn_giwu/providers/prefs_provider.dart';
import 'package:fn_giwu/widgets/settings_drawer.dart';
import 'package:shared_preferences/shared_preferences.dart';

const _reader = UserModel(id: 1, name: 'Thabang', email: 'reader@example.test');

/// Auth without a token or a server: signing in just flips the state.
class _FakeAuth extends AuthNotifier {
  _FakeAuth(super.prefs, {UserModel? initial}) {
    if (initial != null) state = initial;
  }

  @override
  Future<UserModel> login(String email, String password) async {
    state = _reader;
    return _reader;
  }

  @override
  Future<void> logout() async => state = null;
}

const _bibles = [
  BibleModel(
    table: 't_kjv',
    abbreviation: 'KJV',
    version: 'King James Version',
    downloaded: true,
  ),
  BibleModel(
    table: 't_web',
    abbreviation: 'WEB',
    version: 'World English Bible',
    downloaded: true,
  ),
  BibleModel(
    table: 't_ylt',
    abbreviation: 'YLT',
    version: "Young's Literal Translation",
  ),
];

/// The drawer as the reader meets it: closed, behind a Settings action.
///
/// The surface is deliberately tall — every group has to be laid out at once
/// or the lazy list leaves the ones below the fold unbuilt.
Future<Widget> _harness(
  WidgetTester tester, {
  void Function(String table)? onSelectBible,
  VoidCallback? onReset,
  Map<String, Object> prefs = const {},
  bool signedIn = false,
}) async {
  tester.view.physicalSize = const Size(800, 2000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);

  SharedPreferences.setMockInitialValues(prefs);
  final stored = await SharedPreferences.getInstance();
  final key = GlobalKey<ScaffoldState>();

  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(stored),
      authProvider.overrideWith(
        (ref) => _FakeAuth(stored, initial: signedIn ? _reader : null),
      ),
    ],
    child: MaterialApp(
      home: Scaffold(
        key: key,
        endDrawer: SettingsDrawer(
          bibles: _bibles,
          onSelectBible: onSelectBible ?? (_) {},
          onReset: onReset ?? () {},
        ),
        appBar: AppBar(
          actions: [
            IconButton(
              icon: const Icon(Icons.settings_outlined),
              onPressed: () => key.currentState?.openEndDrawer(),
            ),
          ],
        ),
        body: const Text('Genesis 1'),
      ),
    ),
  );
}

Future<void> _openDrawer(WidgetTester tester) async {
  await tester.tap(find.byIcon(Icons.settings_outlined));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('stays shut until the settings action is tapped', (tester) async {
    await tester.pumpWidget(await _harness(tester));
    await tester.pumpAndSettle();

    expect(find.text('SETTINGS'), findsNothing);

    await _openDrawer(tester);

    expect(find.text('SETTINGS'), findsOneWidget);
  });

  testWidgets('offers a guest both ways into an account', (tester) async {
    await tester.pumpWidget(await _harness(tester));
    await _openDrawer(tester);

    expect(find.text('Sign in'), findsOneWidget);
    expect(find.text('Sign up'), findsOneWidget);
    expect(find.text('Sign out'), findsNothing);
    expect(find.text('Bookmarks'), findsNothing);
  });

  testWidgets('shows every group the reader came for', (tester) async {
    await tester.pumpWidget(await _harness(tester));
    await _openDrawer(tester);

    expect(find.text('ACCOUNT'), findsOneWidget);
    expect(find.text('THEME'), findsOneWidget);
    expect(find.text('MAIN BIBLE'), findsOneWidget);
    expect(find.text('RESET'), findsOneWidget);
  });

  testWidgets('dark mode switch reflects and flips the stored preference',
      (tester) async {
    await tester.pumpWidget(await _harness(tester, prefs: {'giwu_dark': true}));
    await _openDrawer(tester);

    final switchFinder = find.byType(SwitchListTile);
    expect(tester.widget<SwitchListTile>(switchFinder).value, isTrue);

    await tester.tap(switchFinder);
    await tester.pumpAndSettle();

    expect(tester.widget<SwitchListTile>(switchFinder).value, isFalse);
  });

  testWidgets('selecting a downloaded translation reports it and closes',
      (tester) async {
    String? selected;
    await tester.pumpWidget(
      await _harness(tester, onSelectBible: (table) => selected = table),
    );
    await _openDrawer(tester);

    await tester.tap(find.text('World English Bible'));
    await tester.pumpAndSettle();

    expect(selected, 't_web');
    expect(find.text('SETTINGS'), findsNothing);
  });

  testWidgets('a translation that is not downloaded cannot be picked',
      (tester) async {
    String? selected;
    await tester.pumpWidget(
      await _harness(tester, onSelectBible: (table) => selected = table),
    );
    await _openDrawer(tester);

    await tester.tap(find.text("Young's Literal Translation"));
    await tester.pumpAndSettle();

    expect(selected, isNull);
    expect(find.text('SETTINGS'), findsOneWidget);
  });

  testWidgets('sign in opens the login page on its sign-in form',
      (tester) async {
    await tester.pumpWidget(await _harness(tester));
    await _openDrawer(tester);

    await tester.tap(find.text('Sign in'));
    await tester.pumpAndSettle();

    expect(find.byType(LoginPage), findsOneWidget);
    expect(find.text('Create account'), findsOneWidget); // the other tab
    expect(find.widgetWithText(FilledButton, 'Sign in'), findsOneWidget);
  });

  testWidgets('sign up opens the login page on its create-account form',
      (tester) async {
    await tester.pumpWidget(await _harness(tester));
    await _openDrawer(tester);

    await tester.tap(find.text('Sign up'));
    await tester.pumpAndSettle();

    expect(find.byType(LoginPage), findsOneWidget);
    expect(
      find.widgetWithText(FilledButton, 'Create account'),
      findsOneWidget,
    );
  });

  // The drawer is gone by the time the login page reports success, so the
  // callback it hands over must not reach for a Navigator through the
  // drawer's own context.
  testWidgets('signing in closes the login page it opened', (tester) async {
    await tester.pumpWidget(await _harness(tester));
    await _openDrawer(tester);

    await tester.tap(find.text('Sign in'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Sign in'));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byType(LoginPage), findsNothing);
    expect(find.text('Genesis 1'), findsOneWidget);
  });

  testWidgets('a signed-in reader gets their identity and a way out',
      (tester) async {
    await tester.pumpWidget(await _harness(tester, signedIn: true));
    await _openDrawer(tester);

    expect(find.text('Thabang'), findsOneWidget);
    expect(find.text('reader@example.test'), findsOneWidget);
    expect(find.text('Bookmarks'), findsOneWidget);
    expect(find.text('Sign out'), findsOneWidget);
    expect(find.text('Sign in'), findsNothing);
    expect(find.text('Sign up'), findsNothing);
  });

  testWidgets('reset reports once and closes the drawer', (tester) async {
    var resets = 0;
    await tester.pumpWidget(await _harness(tester, onReset: () => resets++));
    await _openDrawer(tester);

    await tester.tap(find.text('Back to Genesis 1 (KJV)'));
    await tester.pumpAndSettle();

    expect(resets, 1);
    expect(find.text('SETTINGS'), findsNothing);
  });
}
