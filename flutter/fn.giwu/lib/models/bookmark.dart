class BookmarkModel {
  const BookmarkModel({
    required this.id,
    required this.bible,
    required this.book,
    required this.chapter,
    required this.verse,
    required this.text,
  });

  final int id;
  final String bible;
  final int book;
  final int chapter;
  final int verse;
  final String text;

  factory BookmarkModel.fromJson(Map<String, dynamic> json) => BookmarkModel(
        id: json['id'] as int,
        bible: json['bible'] as String,
        book: json['book'] as int,
        chapter: json['chapter'] as int,
        verse: json['verse'] as int,
        text: json['text'] as String,
      );

  bool matches(String bible, int book, int chapter, int verse) =>
      this.bible == bible &&
      this.book == book &&
      this.chapter == chapter &&
      this.verse == verse;
}
