# Giwu Bible

A multi-version Bible reader. Read a primary chapter (KJV by default) and tap any verse to compare it side-by-side in one other translation.

**Repository:** [https://github.com/thabang-teddy/giwubible](https://github.com/thabang-teddy/giwubible)

---

## Project Structure

```
giwubible/
├── js/fn.giwu/        # React frontend (Vite + Bootstrap 5)
├── php/api.giwu/      # Laravel 10 REST API
└── flutter/fn.giwu/   # Flutter mobile/desktop app
```

---

## Local Development

### Prerequisites

- PHP 8.1+
- Composer
- Node.js 18+
- npm

### 1. Clone the repository

```bash
git clone https://github.com/thabang-teddy/giwubible.git
cd giwubible
```

### 2. Set up the Laravel API

```bash
cd php/api.giwu
composer install
cp .env.example .env
php artisan key:generate
```

Edit `.env` and set the path to the SQLite database:

```env
DB_CONNECTION=sqlite
BIBLE_DB=/absolute/path/to/database/bible-sqlite.db
APP_URL=http://localhost:8000
FRONTEND_URL=http://localhost:5173
```

Start the dev server:

```bash
php artisan serve
# API available at http://localhost:8000/api
```

### 3. Set up the React frontend

```bash
cd js/fn.giwu
npm install
cp .env.example .env
```

Edit `.env`:

```env
VITE_API_URL=http://localhost:8000/api
VITE_SERVER_URL=http://localhost:8000
```

Start the dev server:

```bash
npm run dev
# Frontend available at http://localhost:5173
```

### 4. Build the React frontend for production

```bash
cd js/fn.giwu
npm run build
# Output is in js/fn.giwu/dist/
```

---

## cPanel Deployment

### Overview

| Part | Domain / Subdomain | Folder on server |
|------|--------------------|-----------------|
| React frontend | `yourdomain.com` | `public_html/` |
| Laravel API | `api.yourdomain.com` | `public_html/api.giwu/` (document root → `public/`) |

---

### Step 1 — Upload the Laravel API

1. Upload the entire `php/api.giwu/` folder to your server, e.g. `public_html/api.giwu/`.
2. **Do not** place the `vendor/` folder contents inside `public_html` directly — keep them inside `api.giwu/`.

Via Git (recommended — SSH into the server):

```bash
cd ~/public_html
git clone https://github.com/thabang-teddy/giwubible.git
# Then work from public_html/giwubible/php/api.giwu/
```

---

### Step 2 — Create the API subdomain in cPanel

1. Log in to **cPanel → Subdomains**.
2. Create a new subdomain:
   - **Subdomain:** `api`
   - **Domain:** `yourdomain.com`
   - **Document Root:** `public_html/giwubible/php/api.giwu/public`
3. Click **Create**.

cPanel will point `api.yourdomain.com` directly to the Laravel `public/` folder, which already contains the correct `.htaccess` for URL rewriting.

---

### Step 3 — Configure the Laravel `.env` on the server

SSH into your server or use cPanel **File Manager** to edit `public_html/giwubible/php/api.giwu/.env`:

```env
APP_ENV=production
APP_DEBUG=false
APP_URL=https://api.yourdomain.com
FRONTEND_URL=https://yourdomain.com

DB_CONNECTION=sqlite
BIBLE_DB=/home/cpanelusername/public_html/giwubible/php/api.giwu/database/bible-sqlite.db
```

> Replace `cpanelusername` with your actual cPanel username.

---

### Step 4 — Install Composer dependencies on the server

SSH into the server:

```bash
cd ~/public_html/giwubible/php/api.giwu
composer install --no-dev --optimize-autoloader
php artisan key:generate
php artisan config:cache
php artisan route:cache
```

If Composer is not available via SSH, use the **cPanel → Terminal** or ask your host to install it.

---

### Step 5 — Build and upload the React frontend

On your local machine, build the frontend pointing at the live API:

```bash
cd js/fn.giwu
```

Edit `.env` (or create `.env.production`):

```env
VITE_API_URL=https://api.yourdomain.com/api
VITE_SERVER_URL=https://api.yourdomain.com
```

Build:

```bash
npm run build
```

Upload the **contents** of `js/fn.giwu/dist/` (not the folder itself) to `public_html/` on your server using cPanel **File Manager** or FTP/SFTP.

---

### Step 6 — Add the `.htaccess` for the React frontend

The file `js/fn.giwu/.htaccess` in this repository must be placed in `public_html/` alongside `index.html`. It handles SPA client-side routing so that page refreshes and direct URL access work correctly.

If you upload via FTP, make sure hidden files (`.htaccess`) are visible and transferred.

---

### Verify the deployment

| URL | Expected result |
|-----|-----------------|
| `https://yourdomain.com` | React app loads |
| `https://api.yourdomain.com/api/bibles` | JSON list of Bible versions |
| `https://api.yourdomain.com/api/books` | JSON list of books |

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/bibles` | List all Bible versions |
| GET | `/api/books` | List all books |
| GET | `/api/chapter?bible={table}&book={b}&chapter={c}` | All verses for a chapter |
| GET | `/api/verse?book={b}&chapter={c}&verse={v}&bible={table}` | Single verse in comparison version |

---

## Flutter App

```bash
cd flutter/fn.giwu
flutter pub get

# Run on device/emulator
flutter run -d android
flutter run -d windows

# Production builds
flutter build apk
flutter build windows
```

The API base URL is set in `lib/api/client.dart`. Update `baseUrl` to point at your live API before building for production.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, Vite, Bootstrap 5 |
| API | Laravel 10, SQLite (read-only) |
| Mobile / Desktop | Flutter, Riverpod, Dio |
| Database | `bible-sqlite.db` — static data file, no migrations |
