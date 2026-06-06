import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../api/auth.dart' as auth_api;
import '../api/client.dart';
import '../models/user.dart';
import 'prefs_provider.dart';

const _kTokenKey = 'giwu_auth_token';

class AuthNotifier extends StateNotifier<UserModel?> {
  AuthNotifier(SharedPreferences prefs)
      : _prefs = prefs,
        super(null) {
    _restoreSession();
  }

  final SharedPreferences _prefs;

  Future<void> _restoreSession() async {
    final token = _prefs.getString(_kTokenKey);
    if (token == null) return;
    setAuthToken(token);
    try {
      final data = await auth_api.me();
      state = UserModel.fromJson(data);
    } on Exception {
      await _clearToken();
    }
  }

  Future<UserModel> login(String email, String password) async {
    final data = await auth_api.login(email, password);
    await _setToken(data['token'] as String);
    final user = UserModel.fromJson(data['user'] as Map<String, dynamic>);
    state = user;
    return user;
  }

  Future<UserModel> register(String name, String email, String password) async {
    final data = await auth_api.register(name, email, password);
    await _setToken(data['token'] as String);
    final user = UserModel.fromJson(data['user'] as Map<String, dynamic>);
    state = user;
    return user;
  }

  Future<void> logout() async {
    try {
      await auth_api.logout();
    } on Exception {
      // best-effort
    }
    await _clearToken();
    state = null;
  }

  Future<void> _setToken(String token) async {
    await _prefs.setString(_kTokenKey, token);
    setAuthToken(token);
  }

  Future<void> _clearToken() async {
    await _prefs.remove(_kTokenKey);
    setAuthToken(null);
  }
}

final authProvider = StateNotifierProvider<AuthNotifier, UserModel?>(
  (ref) => AuthNotifier(ref.read(sharedPreferencesProvider)),
);
