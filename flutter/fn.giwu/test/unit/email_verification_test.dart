import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:fn_giwu/api/client.dart';
import 'package:fn_giwu/models/user.dart';

/// How the desktop app decides an address is unconfirmed.
///
/// Two independent signals, both of which have to keep working: the 403 the
/// bookmark endpoints answer with, and the `email_verified_at` field on the
/// account payload.
void main() {
  group('UserModel.fromJson', () {
    test('reads a null verified timestamp as unconfirmed', () {
      final user = UserModel.fromJson({
        'id': 1,
        'name': 'Reader',
        'email': 'a@example.com',
        'email_verified_at': null,
      });

      expect(user.emailVerified, isFalse);
    });

    test('reads a verified timestamp as confirmed', () {
      final user = UserModel.fromJson({
        'id': 1,
        'name': 'Reader',
        'email': 'a@example.com',
        'email_verified_at': '2026-10-25T09:14:22.000000Z',
      });

      expect(user.emailVerified, isTrue);
    });

    test('treats a payload without the field as unconfirmed', () {
      final user = UserModel.fromJson({
        'id': 1,
        'name': 'Reader',
        'email': 'a@example.com',
      });

      expect(user.emailVerified, isFalse);
    });
  });

  /// The account is re-read on every window focus, and `BookmarksNotifier`
  /// reloads the bookmark list whenever the signed-in user changes. Without
  /// value equality, each re-read would be a new object, so every focus would
  /// refetch the list and blank it to a spinner.
  group('UserModel equality', () {
    const payload = {
      'id': 1,
      'name': 'Reader',
      'email': 'a@example.com',
      'email_verified_at': null,
    };

    test('two reads of an unchanged account are equal', () {
      expect(UserModel.fromJson(payload), equals(UserModel.fromJson(payload)));
      expect(
        UserModel.fromJson(payload).hashCode,
        equals(UserModel.fromJson(payload).hashCode),
      );
    });

    test('confirming the address makes it a different value', () {
      final confirmed = UserModel.fromJson({
        ...payload,
        'email_verified_at': '2026-10-25T09:14:22.000000Z',
      });

      expect(UserModel.fromJson(payload), isNot(equals(confirmed)));
    });

    test('a changed name or address makes it a different value', () {
      expect(
        UserModel.fromJson(payload),
        isNot(equals(UserModel.fromJson({...payload, 'name': 'Someone'}))),
      );
      expect(
        UserModel.fromJson(payload),
        isNot(equals(
            UserModel.fromJson({...payload, 'email': 'b@example.com'}))),
      );
    });
  });

  group('isEmailUnverifiedError', () {
    DioException responseWith(int status) => DioException(
          requestOptions: RequestOptions(path: '/bookmarks'),
          response: Response<dynamic>(
            requestOptions: RequestOptions(path: '/bookmarks'),
            statusCode: status,
            data: const {'message': 'Your email address is not verified.'},
          ),
        );

    test('recognises the 403 from the bookmark endpoints', () {
      expect(isEmailUnverifiedError(responseWith(403)), isTrue);
    });

    test('does not mistake other failures for it', () {
      expect(isEmailUnverifiedError(responseWith(401)), isFalse);
      expect(isEmailUnverifiedError(responseWith(404)), isFalse);
      expect(isEmailUnverifiedError(responseWith(422)), isFalse);
      expect(isEmailUnverifiedError(responseWith(500)), isFalse);
    });

    test('a connection failure carries no response to read', () {
      final offline = DioException(
        requestOptions: RequestOptions(path: '/bookmarks'),
        type: DioExceptionType.connectionError,
      );

      expect(isEmailUnverifiedError(offline), isFalse);
    });

    test('an unrelated error is not a verification problem', () {
      expect(isEmailUnverifiedError(Exception('something else')), isFalse);
      expect(isEmailUnverifiedError(StateError('no element')), isFalse);
    });
  });
}
