# Giwu Bible App

Multi-version Bible reader. Users read a primary chapter (KJV by default) and click any verse to see that verse in one other translation side-by-side.

---

## Tech Stack

| Layer     | Technology                                                      |
|-----------|-----------------------------------------------------------------|
| Frontend  | React.js + Bootstrap 5                                          |
| Backend   | Laravel 11 (REST API)                                           |
| Database  | SQLite (`bible-sqlite.db`) — Laravel `sqlite` driver, read-only |
| Auth      | None (v1 is stateless)                                          |

---

## Key Directories

```
js/fn.giwu/          # React frontend
  src/
    pages/           # HomePage, ReadPage
    components/      # Sidebar, MainColumn, VersePanel, BookSelector, BibleSelector
    hooks/           # useBible, useChapter, useVerseComparison
    api/             # Axios client & API wrappers

php/api.giwu/        # Laravel backend
  app/Http/Controllers/Api/
    BibleController   # /bibles list
    BookController    # /books list
    ChapterController # /chapter verses
    VerseController   # /verse in comparison version
  routes/api.php
  database/
    bible-sqlite.db   # Source data — do not migrate, query directly
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

### Frontend (`js/fn.giwu/`)
```bash
npm install
npm run dev        # Vite dev server
npm run build      # Production build
npm run test
```

### Backend (`php/api.giwu/`)
```bash
composer install
cp .env.example .env
# Set DB_CONNECTION=sqlite
# Set DB_DATABASE=/absolute/path/to/bible-sqlite.db
php artisan key:generate
php artisan serve  # http://localhost:8000
php artisan test
```

---

## Data Model (SQLite — read-only)

- **`bible_version_key`** — one row per translation; `table` column is the query target (e.g. `t_kjv`)
- **`key_english`** — book list (`b` = book ID, `n` = name, `t` = OT/NT)
- **`t_{abbreviation}`** — per-translation verse tables; columns: `b` (book), `c` (chapter), `v` (verse), `t` (text)

> Do not run migrations against `bible-sqlite.db`. It is a static data file.

---

## Confirmed Decisions

| Decision                          | Value                               |
|-----------------------------------|-------------------------------------|
| Default primary Bible version     | **KJV** (`t_kjv`)                   |
| Simultaneous comparison versions  | **1** (user picks one from sidebar) |
| Database driver                   | **SQLite** (direct file connection) |

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
