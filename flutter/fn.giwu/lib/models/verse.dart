class VerseModel {
  final int b;
  final int c;
  final int v;
  final String t;

  const VerseModel({
    required this.b,
    required this.c,
    required this.v,
    required this.t,
  });

  factory VerseModel.fromJson(Map<String, dynamic> json) => VerseModel(
        b: (json['b'] as num).toInt(),
        c: (json['c'] as num).toInt(),
        v: (json['v'] as num).toInt(),
        t: json['t'] as String,
      );
}

class ComparisonResult {
  final String bible;
  final String version;
  final String abbreviation;
  final String? text;

  const ComparisonResult({
    required this.bible,
    required this.version,
    required this.abbreviation,
    this.text,
  });

  factory ComparisonResult.fromJson(Map<String, dynamic> json) =>
      ComparisonResult(
        bible: json['bible'] as String,
        version: json['version'] as String,
        abbreviation: json['abbreviation'] as String,
        text: json['text'] as String?,
      );
}
