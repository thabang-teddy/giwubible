class BibleModel {
  final String table;
  final String abbreviation;
  final String version;
  final bool downloaded;

  const BibleModel({
    required this.table,
    required this.abbreviation,
    required this.version,
    this.downloaded = false,
  });

  factory BibleModel.fromJson(Map<String, dynamic> json) => BibleModel(
        table: json['table'] as String,
        abbreviation: json['abbreviation'] as String,
        version: json['version'] as String,
        downloaded: (json['downloaded'] as int? ?? 0) == 1,
      );

  BibleModel copyWith({bool? downloaded}) => BibleModel(
        table: table,
        abbreviation: abbreviation,
        version: version,
        downloaded: downloaded ?? this.downloaded,
      );
}
