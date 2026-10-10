# Giwu Bible App

Multi-version Bible reader. Users read a primary chapter (KJV by default) and click any verse to see that verse in one other translation side-by-side.

---

## Tech Stack

| Layer              | Technology                                                                     |
|--------------------|--------------------------------------------------------------------------------|
| Web Frontend       | React 19 via **Inertia.js**, served from Laravel (`laravel/resources/js`), Bootstrap 5 |
| Android App        | Kotlin + Jetpack Compose (`kotlin/`) — the shipped APK                         |
| Desktop App        | Flutter (`flutter/fn.giwu`) — Windows only; the Android target was removed     |
| Backend            | Laravel 10 — Inertia (web) **and** a JSON REST API (`/api`, for the apps)      |
| Database           | SQLite (`bible-sqlite.db`, read-only) + `database.sqlite` (users/bookmarks)    |
| Auth               | Sanctum: session/cookie for web (Inertia), personal-access tokens for `/api` (the apps) |

> The web app and the JSON API now live in a **single Laravel app at `laravel/`**.
> React is rendered through Inertia (no standalone SPA); `routes/api.php` stays a
> token-auth JSON API for the Kotlin and Flutter clients only.

---

## Key Directories

```
laravel/             # Single Laravel app: Inertia/React web + JSON API
  app/
    Http/Controllers/
      Web/           # Inertia controllers -> Inertia::render()
                     #   Home, Read, Download, Auth, Bookmark, Profile
      Api/           # JSON controllers for the apps (unchanged shapes)
                     #   Bible, Book, Chapter, Verse, Auth, Bookmark, *Download
    Http/Middleware/HandleInertiaRequests.php   # shares auth.user, bookmarks, flash, ziggy
    Repositories/BibleRepository.php            # shared bible queries (Web + Api)
  resources/
    js/
      app.jsx        # createInertiaApp entry
      Pages/         # Home, Read, Login, Bookmarks, Profile, Download (Inertia pages)
      Components/    # Navbar, Sidebar, MainColumn, VersePanel, BottomBar, ChapterNav
      hooks/         # useAuth, useBookmarks (Inertia) + useChapter, useAllVerseComparisons (XHR)
      api/           # axios client + chapter/verse wrappers (in-page reader XHR)
    css/app.css      # ported stylesheet
    views/app.blade.php   # Inertia root view
  routes/
    web.php          # Inertia pages (session/cookie auth)
    api.php          # JSON API for the apps (Sanctum tokens) — DO NOT change shapes
  database/
    bible-sqlite.db  # Source data (read-only, query directly, never migrate)
    database.sqlite  # App DB: users + bookmarks

flutter/fn.giwu/     # Flutter Windows desktop app (consumes /api — unchanged)
  lib/
    main.dart        # Entry point — ProviderScope + MaterialApp
    api/             # Dio client + API wrappers (base URL -> the laravel/ app's /api)
    providers/ widgets/ models/ pages/
  windows/           # The only platform folder left; android/ was removed

kotlin/              # Native Android app, Android Studio project root (consumes /api)
  app/src/main/java/com/giwu/bible/
    AppContainer.kt  # Hand-rolled DI: prefs, database, HTTP, stores, TTS
    MainActivity.kt  # Compose entry point + navigation graph
    data/            # BibleDatabase (SQLiteOpenHelper), AppPrefs, remote/ (OkHttp)
    repo/            # BibleRepository, AuthStore, BookmarkStore, ServerSettings
    tts/             # TtsEngine + AndroidTtsEngine + TtsController
    ui/              # reader/, welcome/, settings/, auth/, bookmarks/, theme/
  app/src/test/      # JVM unit tests (no device needed)
```

---

## Core API Endpoints

| Method | Path                                                        | Description                                   |
|--------|-------------------------------------------------------------|-----------------------------------------------|
| GET    | `/api/bibles`                                               | List all versions from `bible_version_key`    |
| GET    | `/api/books`                                                | List all books from `key_english`             |
| GET    | `/api/chapter?bible={table}&book={b}&chapter={c}`           | All verses for a chapter in one translation   |
| GET    | `/api/verse?book={b}&chapter={c}&verse={v}&bible={table}`   | One verse in the selected comparison version  |

> Comparison is 1 version at a time. The `bible` param on `/api/verse` is a single table name, not an array.

---

## Build & Run Commands

### Web app + API (`laravel/`)
```bash
composer install
npm install
cp .env.example .env        # then set BIBLE_DB to the absolute bible-sqlite.db path
php artisan key:generate

# Development (two terminals)
php artisan serve           # http://localhost:8000 (serves Inertia web + /api)
npm run dev                 # Vite HMR for React

npm run build               # Production asset build (public/build)
php artisan test            # PHPUnit suite
```

> Web pages are Inertia/React (`resources/js/Pages`). The reader still fetches
> chapters and verse comparisons on-demand via `/api` XHR (the in-page
> interactivity exception) — those endpoints are public. `routes/api.php` is the
> apps' JSON API and its response shapes must not change.

### Flutter desktop app (`flutter/fn.giwu/`)
```bash
flutter pub get
flutter run -d windows        # Windows desktop
flutter test
flutter build windows         # Release build (CI wraps it in an Inno installer)
```

> Windows is the only platform this project targets. The `android/` folder was
> removed when the Android app moved to Kotlin — `flutter build apk` no longer
> works here, by design.

> API base URL is configured in `lib/api/client.dart` via a `const baseUrl` constant.
> Default: `https://api.giwu.test/`

> The backend lives in the same `laravel/` folder as the web app (see above).
> There is no longer a separate `php/api.giwu/` app.

### Native Android app (`kotlin/`)
```bash
./gradlew :app:installDebug          # Build and install on a device/emulator
./gradlew :app:testDebugUnitTest     # JVM unit tests
./gradlew :app:lintDebug             # Android lint
./gradlew :app:assembleRelease       # Release APK — this is what CI publishes
```

> Open `kotlin/` (not the repo root) in Android Studio. API base URL default is
> `DEFAULT_BASE_URL` in `data/remote/ApiClient.kt`; the reader can override it
> in Settings, and the chosen URL is stored in `app_settings.server_url`.
> Application ID is `com.giwu.bible.kt`. Read-aloud uses Android's own
> `TextToSpeech` rather than the Flutter build's downloaded sherpa-onnx voice —
> see `kotlin/README.md` for the full Flutter-to-Kotlin mapping and the
> deliberate differences.

> The released APK comes from here. CI stamps the version from the Flutter
> `pubspec.yaml` (so the APK, the tag and the Windows installer agree) via
> `-PgiwuVersionName` and `-PgiwuVersionCode`.

---

## Data Model (SQLite — read-only)

- **`bible_version_key`** — one row per translation; `table` column is the query target (e.g. `t_kjv`)
- **`key_english`** — book list (`b` = book ID, `n` = name). NB: this data file has **no `t` column**; a legacy `select('b','n','t')` degrades to a junk `"t":"t"` field via a SQLite quoted-identifier quirk, preserved for API compatibility (see `MIGRATION.md`)
- **`t_{abbreviation}`** — per-translation verse tables; columns: `b` (book), `c` (chapter), `v` (verse), `t` (text)

> Do not run migrations against `bible-sqlite.db`. It is a static data file.

---

## Confirmed Decisions

| Decision                          | Value                               |
|-----------------------------------|-------------------------------------|
| Default primary Bible version     | **KJV** (`t_kjv`)                   |
| Simultaneous comparison versions  | **1** (user picks one from sidebar) |
| Database driver                   | **SQLite** (direct file connection) |

### Flutter-specific decisions

| Decision                          | Value                                                                 |
|-----------------------------------|-----------------------------------------------------------------------|
| Target platforms                  | Windows only (sqflite via FFI; Inno Setup installer). Android moved to `kotlin/` |
| State management                  | **Riverpod** (`flutter_riverpod`)                                     |
| HTTP client                       | **Dio** (mirrors Axios usage in JS frontend)                          |
| Local persistence                 | **shared_preferences** — saves primaryBible, comparisonBible, book, chapter |
| Verse comparison UX               | Draggable **bottom sheet** appears on verse tap (one version at a time) |
| Navigation                        | Single `ReadPage` screen; bible/book/chapter picked from drawer/sheet |
| API base URL                      | `https://api.giwu.test/`                                               |

### Kotlin-specific decisions

| Decision                          | Value                                                                 |
|-----------------------------------|-----------------------------------------------------------------------|
| Target platform                   | Android only, minSdk 21 / targetSdk 36                                |
| UI                                | **Jetpack Compose** + Material 3; drawer on a phone, three columns at 768dp |
| State management                  | `StateFlow` + `ViewModel`, hand-rolled DI in `AppContainer`           |
| HTTP client                       | **OkHttp** + kotlinx.serialization (tolerant readers for the API's shapes) |
| Local persistence                 | `SQLiteOpenHelper` (verses), SharedPreferences (same keys as Flutter), `EncryptedSharedPreferences` (token) |
| Read aloud                        | Android's own `TextToSpeech` — no downloaded voice model              |
| API base URL                      | `https://giwu.co.za/api/`, overridable in Settings                    |

---

## Open Decisions

- [x] Persist user selections (version, book, chapter) in `localStorage`?
- [x] Home page — links to external app store, or IS the web reader?

---

## Additional Documentation

| Topic                        | File                                                                   |
|------------------------------|------------------------------------------------------------------------|
| Architecture & patterns      | `.claude/docs/architectural_patterns.md`                               |
| UI layout & responsive rules | `.claude/docs/ui_layout.md` *(create when building ReadPage)*          |
| API design conventions       | `.claude/docs/api_conventions.md` *(create when building controllers)* |
| Database query patterns      | `.claude/docs/db_patterns.md` *(create when building models/queries)*  |
| Flutter architecture         | `.claude/docs/flutter_architecture.md` *(create when building Flutter app)* |
