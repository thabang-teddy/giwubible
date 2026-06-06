import '../models/bookmark.dart';
import 'client.dart';

Future<List<BookmarkModel>> listBookmarks() async {
  final response = await dio.get('/bookmarks');
  final list = response.data as List<dynamic>;
  return list
      .map((e) => BookmarkModel.fromJson(e as Map<String, dynamic>))
      .toList();
}

Future<BookmarkModel> saveBookmark({
  required String bible,
  required int book,
  required int chapter,
  required int verse,
  required String text,
}) async {
  final response = await dio.post('/bookmarks', data: {
    'bible': bible,
    'book': book,
    'chapter': chapter,
    'verse': verse,
    'text': text,
  });
  return BookmarkModel.fromJson(response.data as Map<String, dynamic>);
}

Future<void> deleteBookmark(int id) async {
  await dio.delete('/bookmarks/$id');
}
