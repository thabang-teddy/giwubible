import '../models/bible.dart';

/// Canonical list of Bible translations that ship with the app.
/// Used to seed [bible_version_key] on first launch and as an offline
/// fallback so the WelcomePage always shows Bible cards — even when the
/// device has no network and an empty local DB.
const List<({String table, String abbreviation, String version})>
    kBibleVersions = [
  (table: 't_asv', abbreviation: 'ASV',    version: 'American Standard-ASV1901'),
  (table: 't_bbe', abbreviation: 'BBE',    version: 'Bible in Basic English'),
  (table: 't_dby', abbreviation: 'DARBY',  version: 'Darby English Bible'),
  (table: 't_kjv', abbreviation: 'KJV',    version: 'King James Version'),
  (table: 't_wbt', abbreviation: 'WBT',    version: "Webster's Bible"),
  (table: 't_web', abbreviation: 'WEB',    version: 'World English Bible'),
  (table: 't_ylt', abbreviation: 'YLT',    version: "Young's Literal Translation"),
];

/// Converts [kBibleVersions] to [BibleModel] instances (downloaded = false).
List<BibleModel> get defaultBibles => kBibleVersions
    .map((e) => BibleModel(
          table: e.table,
          abbreviation: e.abbreviation,
          version: e.version,
        ))
    .toList();
