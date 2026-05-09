# Architectural Patterns — Giwu Bible App

Patterns that appear across multiple parts of the codebase. Reference this before adding new features.

---

## 1. API: Table-as-parameter Pattern

Bible translation tables (`t_kjv`, `t_asv`, etc.) are dynamic — the table name is a runtime value sourced from `bible_version_key.table`.

**Rule:** Controllers must whitelist valid table names by querying `bible_version_key` before interpolating into raw queries. Never accept a table name directly from user input.

```
BibleVersionKey::pluck('table') → validate → DB::table($validatedName)
```

Relevant files: `BibleController`, `ChapterController`, `VerseController`

---

## 2. API: Single-version Verse Fetch

When a user clicks a verse, the frontend requests that verse from exactly one comparison translation:

```
GET /api/verse?book=1&chapter=1&verse=1&bible=t_asv
```

The controller queries the single validated table and returns:

```json
{
  "data": {
    "bible": "t_asv",
    "abbreviation": "ASV",
    "version": "American Standard-ASV1901",
    "text": "In the beginning God created the heavens and the earth."
  }
}
```

The user switches comparison version via the sidebar selector; each switch triggers a new `/api/verse` call.

---

## 3. Frontend: Controlled Navigation State

Book, chapter, primary Bible (always KJV), and active comparison Bible are lifted to `ReadPage` level. All child components receive these as props or consume from context — they do not manage navigation state independently.

```
ReadPage (state: comparisonBible, book, chapter, activeVerse)
  ├── Sidebar       (props: book, chapter, comparisonBible → setters)
  ├── MainColumn    (props: book, chapter → setActiveVerse)
  └── VersePanel    (props: activeVerse, comparisonBible)
```

Primary Bible (KJV) is a constant, not state — it never changes.

---

## 4. Frontend: Comparison Bible Selection

The sidebar holds a `<select>` of available Bible versions sourced from `GET /api/bibles`. The selected value is a single `table` string (e.g. `"t_asv"`). It is passed directly as the `bible` query param to `/api/verse`.

Changing the comparison Bible while a verse is active immediately re-fetches that verse in the new version.

---

## 5. Responsive Layout: Sidebar Collapse

The sidebar (book/chapter navigation + comparison version selector) is hidden on `md` and below using Bootstrap's `d-none d-lg-block` utilities. On small screens, a hamburger/drawer (Bootstrap offcanvas) surfaces it.

**Column grid (desktop, Bootstrap):**
```
| Sidebar col-2 | KJV Chapter col-6 | Verse Comparison col-4 |
```

**Mobile:** sidebar off-canvas; verse comparison appears as a bottom sheet or inline below the tapped verse.

---

## 6. Backend: Read-only SQLite

The Laravel app connects to `bible-sqlite.db` as a read-only data source. No Eloquent migrations run against it. Models targeting this DB use:

```php
protected $connection = 'bible_sqlite';
public $timestamps = false;
```

A separate writable connection handles any future app data (user prefs, bookmarks).

---

## 7. API Response Envelope

All API responses use a consistent shape:

```json
{
  "data": { ... },
  "meta": { ... }
}
```

Errors:

```json
{
  "error": "Human-readable message",
  "code": "MACHINE_CODE"
}
```
