# Giwu Bible App

Multi-version Bible reader. Users read a primary chapter (KJV by default) and click any verse to see that verse in one other translation side-by-side.

---

## Tech Stack

| Layer              | Technology                                                                     |
|--------------------|--------------------------------------------------------------------------------|
| Web Frontend       | React 19 via **Inertia.js**, served from Laravel (`laravel/resources/js`), Bootstrap 5 |
| Mobile/Desktop App | Flutter (Android + Windows/macOS/Linux)                                        |
| Backend            | Laravel 10 — Inertia (web) **and** a JSON REST API (`/api`, for Flutter)       |
| Database           | SQLite (`bible-sqlite.db`, read-only) + `database.sqlite` (users/bookmarks)    |
| Auth               | Sanctum: session/cookie for web (Inertia), personal-access tokens for `/api` (Flutter) |

> The web app and the JSON API now live in a **single Laravel app at `laravel/`**.
> React is rendered through Inertia (no standalone SPA); `routes/api.php` stays a
> token-auth JSON API for the Flutter client only.

---

## Key Directories

```
laravel/             # Single Laravel app: Inertia/React web + JSON API
  app/
    Http/Controllers/
      Web/           # Inertia controllers -> Inertia::render()
                     #   Home, Read, Download, Auth, Bookmark, Profile
      Api/           # JSON controllers for Flutter (unchanged shapes)
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
    api.php          # JSON API for Flutter (Sanctum tokens) — DO NOT change shapes
  database/
    bible-sqlite.db  # Source data (read-only, query directly, never migrate)
    database.sqlite  # App DB: users + bookmarks

flutter/fn.giwu/     # Flutter mobile/desktop app (consumes /api — unchanged)
  lib/
    main.dart        # Entry point — ProviderScope + MaterialApp
    api/             # Dio client + API wrappers (base URL -> the laravel/ app's /api)
    providers/ widgets/ models/ pages/
  android/ windows/ linux/ macos/
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
> Flutter JSON API and its response shapes must not change.

### Flutter app (`flutter/fn.giwu/`)
```bash
flutter pub get
flutter run -d android        # Android emulator/device
flutter run -d windows        # Windows desktop
flutter run -d linux          # Linux desktop
flutter run -d macos          # macOS desktop
flutter test
flutter build apk             # Release APK
flutter build windows         # Release Windows build
```

> API base URL is configured in `lib/api/client.dart` via a `const baseUrl` constant.
> Default: `https://api.giwu.test/`

> The backend lives in the same `laravel/` folder as the web app (see above).
> There is no longer a separate `php/api.giwu/` app.

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
| Target platforms                  | Android (minSdk 21) + Windows / macOS / Linux desktop                 |
| State management                  | **Riverpod** (`flutter_riverpod`)                                     |
| HTTP client                       | **Dio** (mirrors Axios usage in JS frontend)                          |
| Local persistence                 | **shared_preferences** — saves primaryBible, comparisonBible, book, chapter |
| Verse comparison UX               | Draggable **bottom sheet** appears on verse tap (one version at a time) |
| Navigation                        | Single `ReadPage` screen; bible/book/chapter picked from drawer/sheet |
| API base URL                      | `https://api.giwu.test/`                                               |

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
