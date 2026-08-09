import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:fn_giwu/models/verse.dart';
import 'package:fn_giwu/providers/prefs_provider.dart';
import 'package:fn_giwu/providers/tts_provider.dart';
import 'package:fn_giwu/services/tts_service.dart';
import 'package:fn_giwu/widgets/read_aloud_fab.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../unit/fake_tts_engine.dart';

final _verses = [
  for (var i = 1; i <= 3; i++)
    VerseModel(b: 1, c: 1, v: i, t: 'Verse $i text.'),
];

Future<Widget> _harness({
  required FakeTtsEngine engine,
  required List<VerseModel>? verses,
}) async {
  SharedPreferences.setMockInitialValues({});
  final prefs = await SharedPreferences.getInstance();

  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      ttsEngineProvider.overrideWithValue(engine),
      // Bypass the voice-install probe; readiness comes from the fake engine.
      ttsReadinessProvider.overrideWith((ref) => engine.prepare()),
    ],
    child: MaterialApp(
      home: Scaffold(floatingActionButton: ReadAloudFab(verses: verses)),
    ),
  );
}

void main() {
  testWidgets('shows the read-aloud button once a chapter has loaded',
      (tester) async {
    await tester.pumpWidget(
      await _harness(engine: FakeTtsEngine(), verses: _verses),
    );
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.volume_up_rounded), findsOneWidget);
  });

  testWidgets('stays hidden while the chapter is still loading',
      (tester) async {
    await tester.pumpWidget(
      await _harness(engine: FakeTtsEngine(), verses: null),
    );
    await tester.pumpAndSettle();

    expect(find.byType(FloatingActionButton), findsNothing);
  });

  testWidgets('stays hidden when the chapter has no verses', (tester) async {
    await tester.pumpWidget(
      await _harness(engine: FakeTtsEngine(), verses: const []),
    );
    await tester.pumpAndSettle();

    expect(find.byType(FloatingActionButton), findsNothing);
  });

  testWidgets('stays hidden when the voice model cannot be loaded',
      (tester) async {
    await tester.pumpWidget(
      await _harness(
        engine: FakeTtsEngine(readiness: TtsReadiness.unsupported),
        verses: _verses,
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byType(FloatingActionButton), findsNothing);
  });

  testWidgets('still offers the button when the voice needs downloading',
      (tester) async {
    await tester.pumpWidget(
      await _harness(
        engine: FakeTtsEngine(readiness: TtsReadiness.needsVoiceModel),
        verses: _verses,
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.volume_up_rounded), findsOneWidget);
  });

  testWidgets('tapping starts playback and swaps in the transport controls',
      (tester) async {
    final engine = FakeTtsEngine();
    await tester.pumpWidget(await _harness(engine: engine, verses: _verses));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.volume_up_rounded));
    await tester.pumpAndSettle();

    expect(engine.spoken, ['Verse 1 text.']);
    expect(find.byIcon(Icons.pause_rounded), findsOneWidget);
    expect(find.byIcon(Icons.skip_next_rounded), findsOneWidget);
    expect(find.byIcon(Icons.stop_rounded), findsOneWidget);
    expect(find.byIcon(Icons.volume_up_rounded), findsNothing);
  });

  testWidgets('pause swaps the transport button back to play', (tester) async {
    await tester.pumpWidget(
      await _harness(engine: FakeTtsEngine(), verses: _verses),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byIcon(Icons.volume_up_rounded));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.pause_rounded));
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.play_arrow_rounded), findsOneWidget);
  });

  testWidgets('stop returns to the collapsed button', (tester) async {
    await tester.pumpWidget(
      await _harness(engine: FakeTtsEngine(), verses: _verses),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byIcon(Icons.volume_up_rounded));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.stop_rounded));
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.volume_up_rounded), findsOneWidget);
  });

  testWidgets('the speed control cycles through the presets', (tester) async {
    await tester.pumpWidget(
      await _harness(engine: FakeTtsEngine(), verses: _verses),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byIcon(Icons.volume_up_rounded));
    await tester.pumpAndSettle();
    expect(find.text('1x'), findsOneWidget);

    await tester.tap(find.text('1x'));
    await tester.pumpAndSettle();

    expect(find.text('1.25x'), findsOneWidget);
  });
}
