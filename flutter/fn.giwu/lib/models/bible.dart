class BibleModel {
  final String table;
  final String abbreviation;
  final String version;

  const BibleModel({
    required this.table,
    required this.abbreviation,
    required this.version,
  });

  factory BibleModel.fromJson(Map<String, dynamic> json) => BibleModel(
        table: json['table'] as String,
        abbreviation: json['abbreviation'] as String,
        version: json['version'] as String,
      );
}
