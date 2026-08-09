import 'package:flutter_test/flutter_test.dart';
import 'package:fn_giwu/models/verse.dart';
import 'package:fn_giwu/providers/tts_provider.dart';

import 'fake_tts_engine.dart';

List<VerseModel> _chapter(int count) => [
      for (var i = 1; i <= count; i++)
        VerseModel(b: 1, c: 1, v: i, t: 'Verse $i text.'),
    ];

void main() {
  late FakeTtsEngine engine;
  late TtsController controller;

  setUp(() {
    engine = FakeTtsEngine();
    controller = TtsController(engine: engine);
  });

  tearDown(() => controller.dispose());

  group('start', () {
    test('speaks the first verse and reports it as speaking', () async {
      // Arrange
      final verses = _chapter(3);

      // Act
      unawaitedStart(controller, verses);
      await pump();

      // Assert
      expect(controller.state.status, TtsStatus.speaking);
      expect(controller.state.verse, 1);
      expect(engine.spoken, ['Verse 1 text.']);
    });

    test('starts from the requested verse when one is given', () async {
      unawaitedStart(controller, _chapter(5), fromVerse: 3);
      await pump();

      expect(controller.state.verse, 3);
      expect(engine.spoken, ['Verse 3 text.']);
    });

    test('falls back to the first verse when fromVerse is not in the chapter',
        () async {
      unawaitedStart(controller, _chapter(3), fromVerse: 99);
      await pump();

      expect(controller.state.verse, 1);
    });

    test('ignores an empty chapter', () async {
      await controller.start(const []);

      expect(controller.state.status, TtsStatus.idle);
      expect(engine.spoken, isEmpty);
    });

    test('advances to the next verse when an utterance completes', () async {
      unawaitedStart(controller, _chapter(3));
      await pump();

      await engine.finishUtterance();

      expect(controller.state.verse, 2);
      expect(engine.spoken, ['Verse 1 text.', 'Verse 2 text.']);
    });

    test('flags reachedEnd and goes idle after the last verse', () async {
      unawaitedStart(controller, _chapter(2));
      await pump();

      await engine.finishUtterance(); // verse 1 -> verse 2
      await engine.finishUtterance(); // verse 2 -> end

      expect(controller.state.status, TtsStatus.idle);
      expect(controller.state.reachedEnd, isTrue);
      expect(controller.state.verse, isNull);
    });
  });

  group('pause and resume', () {
    test('pause stops the engine but keeps the current verse', () async {
      unawaitedStart(controller, _chapter(3));
      await pump();

      await controller.pause();

      expect(controller.state.status, TtsStatus.paused);
      expect(controller.state.verse, 1);
      expect(engine.stopCount, greaterThan(0));
    });

    test('resume replays the verse playback was paused on', () async {
      unawaitedStart(controller, _chapter(3));
      await pump();
      await engine.finishUtterance(); // now on verse 2
      await controller.pause();

      unawaited(controller.resume());
      await pump();

      expect(controller.state.status, TtsStatus.speaking);
      expect(controller.state.verse, 2);
      expect(engine.spoken.last, 'Verse 2 text.');
    });

    test('pause is a no-op when nothing is playing', () async {
      await controller.pause();

      expect(controller.state.status, TtsStatus.idle);
    });

    test('resume is a no-op when not paused', () async {
      await controller.resume();

      expect(controller.state.status, TtsStatus.idle);
      expect(engine.spoken, isEmpty);
    });

    test('a paused utterance does not keep advancing', () async {
      unawaitedStart(controller, _chapter(3));
      await pump();
      await controller.pause();
      final spokenAfterPause = engine.spoken.length;

      // The platform resolving the cancelled utterance must not advance state.
      await pump();

      expect(engine.spoken.length, spokenAfterPause);
      expect(controller.state.status, TtsStatus.paused);
      expect(controller.state.verse, 1);
    });
  });

  group('stop', () {
    test('resets to idle without the end-of-chapter flag', () async {
      unawaitedStart(controller, _chapter(3));
      await pump();

      await controller.stop();

      expect(controller.state.status, TtsStatus.idle);
      expect(controller.state.verse, isNull);
      expect(controller.state.reachedEnd, isFalse);
    });
  });

  group('skipping', () {
    test('next moves forward one verse', () async {
      unawaitedStart(controller, _chapter(4));
      await pump();

      unawaited(controller.next());
      await pump();

      expect(controller.state.verse, 2);
      expect(engine.spoken.last, 'Verse 2 text.');
    });

    test('next on the last verse stops playback', () async {
      unawaitedStart(controller, _chapter(2), fromVerse: 2);
      await pump();

      await controller.next();

      expect(controller.state.status, TtsStatus.idle);
      expect(controller.state.reachedEnd, isFalse);
    });

    test('previous moves back one verse', () async {
      unawaitedStart(controller, _chapter(4), fromVerse: 3);
      await pump();

      unawaited(controller.previous());
      await pump();

      expect(controller.state.verse, 2);
    });

    test('previous on the first verse restarts it rather than underflowing',
        () async {
      unawaitedStart(controller, _chapter(3));
      await pump();

      unawaited(controller.previous());
      await pump();

      expect(controller.state.verse, 1);
      expect(engine.spoken, ['Verse 1 text.', 'Verse 1 text.']);
    });

    test('skipping is ignored while idle', () async {
      await controller.next();
      await controller.previous();

      expect(controller.state.status, TtsStatus.idle);
      expect(engine.spoken, isEmpty);
    });
  });

  group('settings', () {
    test('applies the rate multiplier when playback starts', () async {
      final rated = TtsController(engine: engine, rate: 1.5);
      addTearDown(rated.dispose);

      unawaitedStart(rated, _chapter(1));
      await pump();

      expect(engine.rates, contains(1.5));
    });

    test('changing rate mid-playback re-issues the current verse', () async {
      unawaitedStart(controller, _chapter(3));
      await pump();

      unawaited(controller.setRate(2.0));
      await pump();

      expect(engine.rates.last, 2.0);
      expect(controller.state.verse, 1);
    });

    test('announces the verse number when the setting is on', () async {
      final announcing = TtsController(engine: engine, announceNumbers: true);
      addTearDown(announcing.dispose);

      unawaitedStart(announcing, _chapter(1));
      await pump();

      expect(engine.spoken.single, 'Verse 1. Verse 1 text.');
    });

    test('skips verses whose text is empty after cleaning', () async {
      final verses = [
        const VerseModel(b: 1, c: 1, v: 1, t: '   '),
        const VerseModel(b: 1, c: 1, v: 2, t: 'Real text.'),
      ];

      unawaitedStart(controller, verses);
      await pump();

      expect(engine.spoken, ['Real text.']);
      expect(controller.state.verse, 2);
    });
  });

  group('failures', () {
    test('a speak error stops playback instead of hanging', () async {
      engine.speakError = Exception('engine died');

      await controller.start(_chapter(3));
      await pump();

      expect(controller.state.status, TtsStatus.idle);
    });
  });

  group('clearReachedEnd', () {
    test('clears the flag so it is only acted on once', () async {
      unawaitedStart(controller, _chapter(1));
      await pump();
      await engine.finishUtterance();
      expect(controller.state.reachedEnd, isTrue);

      controller.clearReachedEnd();

      expect(controller.state.reachedEnd, isFalse);
    });
  });
}

// ── Helpers ────────────────────────────────────────────────────────────────

/// [TtsController.start] only completes when the whole chapter has been read,
/// so tests kick it off and then drive the fake engine.
void unawaitedStart(
  TtsController controller,
  List<VerseModel> verses, {
  int? fromVerse,
}) {
  unawaited(controller.start(verses, fromVerse: fromVerse));
}

void unawaited(Future<void> future) {
  future.ignore();
}

/// Lets pending microtasks in the speak loop run.
Future<void> pump() => Future<void>.delayed(Duration.zero);
