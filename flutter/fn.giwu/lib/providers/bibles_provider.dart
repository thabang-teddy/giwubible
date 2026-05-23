import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/bibles.dart';
import '../api/books.dart';
import '../models/bible.dart';
import '../models/book.dart';

final biblesProvider = FutureProvider<List<BibleModel>>((ref) => getBibles());

final booksProvider = FutureProvider<List<BookModel>>((ref) => getBooks());
