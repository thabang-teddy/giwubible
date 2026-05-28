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
        b: _toInt(json['b']),
        c: _toInt(json['c']),
        v: _toInt(json['v']),
        t: json['t'] as String,
      );

  /// Safely converts a JSON value to int.
  /// Handles both native JSON numbers (int/double) and string-encoded integers
  /// that some PHP/PDO/SQLite combinations produce (e.g. `"1"` instead of `1`).
  static int _toInt(dynamic value) {
    if (value is int) return value;
    if (value is num) return value.toInt();
    if (value is String) return int.parse(value);
    throw FormatException('Cannot convert $value (${value.runtimeType}) to int');
  }
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
