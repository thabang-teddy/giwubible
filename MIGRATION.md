# Migration: React SPA + Laravel API → single Inertia.js app (`laravel/`)

The web frontend (`js/fn.giwu/`, a standalone React SPA) and the JSON API
(`php/api.giwu/`, a Laravel 10 app) were merged into a **single Laravel app at
`laravel/`**, with React served through **Inertia.js** — mirroring the pattern in
the sibling `fiyobra/laravel` project. The Flutter app's JSON API is preserved
unchanged.

## What moved where

| Old location | New location |
|---|---|
| `php/api.giwu/` (whole Laravel app) | `laravel/` |
| `php/data/bible-sqlite.db` | `laravel/database/bible-sqlite.db` (`BIBLE_DB` repointed in `.env`) |
| `js/fn.giwu/src/pages/*` | `laravel/resources/js/Pages/*` (Inertia pages) |
| `js/fn.giwu/src/components/*` | `laravel/resources/js/Components/*` |
| `js/fn.giwu/src/hooks/*`, `api/*`, `data/*` | `laravel/resources/js/{hooks,api,data}/*` |
| `js/fn.giwu/src/index.css` | `laravel/resources/css/app.css` |
| `js/fn.giwu/public/*` (icons, manifest) | `laravel/public/*` |

## Web stack added

- **Server:** `inertiajs/inertia-laravel` v2 + `tightenco/ziggy` v2.
- `app/Http/Middleware/HandleInertiaRequests.php` — shares `auth.user`,
  `bookmarks`, `flash`, `ziggy`; registered on the `web` group in
  `app/Http/Kernel.php` (Laravel 10 style — this app is Laravel 10, not 11).
- `resources/views/app.blade.php` — Inertia root view (`@vite`, `@routes`, `@inertia`).
- **Client:** `@inertiajs/react` v2, `laravel-vite-plugin`, `@vitejs/plugin-react`,
  Vite 6. Entry `resources/js/app.jsx` (`createInertiaApp`). React stays at 19.
- `routes/web.php` → `App\Http\Controllers\Web\*` returning `Inertia::render()`.

## Routing / auth changes

- `react-router-dom` → Inertia routing. `<Link to>` → `<Link href>`,
  `useNavigate` → `router.visit`, `useSearchParams` → `window.location.search`
  (deep-link parsing on `/read`).
- **Web auth switched from Sanctum tokens to session/cookie** (`Auth::attempt`,
  session regeneration). `useAuth` now reads the shared `auth.user` prop; login,
  register and profile updates use Inertia forms. `localStorage` token flow removed
  for the web.
- **Flutter auth is unchanged** — `routes/api.php` still issues Sanctum tokens.

## What stayed XHR (the in-page interactivity exception)

The reader's on-demand data is *not* full Inertia navigation — it stays as small
JSON `fetch`/axios calls to the public `/api` endpoints, because a full page load
per chapter/verse would wreck the reading UX:

- **Chapter load** (`useChapter` → `GET /api/chapter`) — on book/chapter change.
- **Parallel verse comparison** (`useAllVerseComparisons` → `GET /api/verse`) — on
  verse tap.

Everything else (page navigation, initial `bibles`/`books` for `/read`, auth,
bookmarks) goes through Inertia. Bookmarks use Inertia visits with a partial
reload (`only: ['bookmarks','flash']`) so toggling doesn't reload the chapter.

## Shared query layer

`app/Repositories/BibleRepository.php` holds the bible-sqlite queries. Both the
`Web\` (Inertia) and `Api\` (Flutter JSON) controllers call it, so the query logic
isn't duplicated. The `Api\*` controllers keep their exact previous JSON envelopes.

## Preserved quirk: `/api/books` `"t"` field

This `bible-sqlite.db`'s `key_english` table has only `b` and `n` columns. The
original `BookController` selected a non-existent `t` column; SQLite silently
treats the double-quoted identifier `"t"` as the string literal `'t'`, so every
book row carries a junk `"t":"t"` field. This is **preserved byte-for-byte** so the
Flutter payload is identical. `b` and `n` are the fields both clients actually use.

## Verification

- `npm run build` — 799 modules, all six pages compile.
- `php artisan test` — 12 passing (Inertia page renders, session auth
  register/login/logout, bookmark store, `/api` JSON shapes).
- Live browser smoke test: Home + Read render, `/api/chapter` and `/api/verse`
  XHRs return 200, no console errors.
- Flutter endpoints (`/bibles`, `/books`, `/chapter`, `/verse`, `/auth/*`,
  `/bookmarks`, `/bibles/{t}/download`, `/downloads/*`) and their parsed fields are
  unchanged.

## Follow-ups for the operator

- **Point your web server at `laravel/`** (e.g. link the Herd site to this folder).
  The Flutter app expects the API at its configured base URL
  (`lib/api/client.dart`, currently `https://api.giwu.co.za/api/`) — serve the new
  `laravel/` app at that origin so `/api/*` keeps resolving.
- **Old folders `js/fn.giwu/` and `php/api.giwu/` are now superseded** and can be
  removed once the new setup is serving. They remain in git history if needed.
