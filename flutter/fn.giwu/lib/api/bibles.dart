import '../models/bible.dart';
import 'client.dart';

Future<List<BibleModel>> getBibles() async {
  final res = await dio.get('bibles');
  final data = res.data['data'] as List;
  return data.map((e) => BibleModel.fromJson(e as Map<String, dynamic>)).toList();
}
