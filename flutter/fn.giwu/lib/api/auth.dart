import 'client.dart';

Future<Map<String, dynamic>> register(
    String name, String email, String password) async {
  final response = await dio.post('/auth/register', data: {
    'name': name,
    'email': email,
    'password': password,
    'password_confirmation': password,
  });
  return response.data as Map<String, dynamic>;
}

Future<Map<String, dynamic>> login(String email, String password) async {
  final response = await dio.post('/auth/login', data: {
    'email': email,
    'password': password,
  });
  return response.data as Map<String, dynamic>;
}

Future<void> logout() async {
  await dio.post('/auth/logout');
}

Future<Map<String, dynamic>> me() async {
  final response = await dio.get('/auth/me');
  return response.data as Map<String, dynamic>;
}
