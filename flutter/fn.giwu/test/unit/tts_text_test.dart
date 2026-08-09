import 'package:flutter_test/flutter_test.dart';
import 'package:fn_giwu/services/tts_service.dart';

void main() {
  group('prepareVerseText', () {
    test('keeps translator-supplied words but drops the brackets', () {
      // Arrange
      const raw = 'And God said, Let there be light: and [there] was light.';

      // Act
      final result = prepareVerseText(raw);

      // Assert
      expect(
        result,
        'And God said, Let there be light: and there was light.',
      );
    });

    test('drops brace delimiters used by other public-domain exports', () {
      expect(
          prepareVerseText('In the {beginning} God'), 'In the beginning God');
    });

    test('collapses newlines and repeated spaces into single spaces', () {
      expect(prepareVerseText('the  LORD\n\tGod'), 'the LORD God');
    });

    test('trims surrounding whitespace', () {
      expect(prepareVerseText('   Jesus wept.  '), 'Jesus wept.');
    });

    test('handles brackets attached to a word without splitting it', () {
      expect(prepareVerseText('wor[d]'), 'word');
    });

    test('returns an empty string for whitespace-only input', () {
      expect(prepareVerseText('   \n '), '');
    });

    test('returns an empty string for empty input', () {
      expect(prepareVerseText(''), '');
    });
  });
}
