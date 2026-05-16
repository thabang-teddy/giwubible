# Deployment Guide

Two stacks to deploy:

| Part | Source | Output |
|------|--------|--------|
| **Frontend** | `js/fn.giwu/` (React/Vite) | Static files — serve from any web server |
| **Backend** | `php/api.giwu/` (Laravel 11) | PHP app — needs PHP 8.2+ and a web server |

The `bible-sqlite.db` file must be present on the server. Its absolute path goes in `BIBLE_DB` inside the backend `.env`.

---

## Option A — cPanel

### 1. Build the frontend locally

```bash
cd js/fn.giwu
npm install
npm run build        # produces js/fn.giwu/dist/
```

### 2. Create two subdomains in cPanel

| Subdomain | Document Root |
|-----------|---------------|
| `giwu.co.za` | `public_html/` |
| `api.giwu.co.za` | `public_html/api/public/` |

> In cPanel → **Domains** → **Create a New Domain**, set the document root exactly as above for the API subdomain. This means Laravel's `public/index.php` is the entry point.

### 3. Upload the frontend

Upload everything inside `js/fn.giwu/dist/` to `public_html/`.

Add a `.htaccess` file in `public_html/` for React Router:

```apache
Options -MultiViews
RewriteEngine On
RewriteCond %{REQUEST_FILENAME} !-f
RewriteRule ^ index.html [QSA,L]
```

### 4. Upload the backend

Upload the entire `php/api.giwu/` folder contents to `public_html/api/`.

The folder layout on the server should be:

```
public_html/
  api/
    app/
    bootstrap/
    config/
    public/          ← subdomain document root points here
    routes/
    storage/
    vendor/          ← generated in step 5
    .env             ← created in step 6
    ...
```

> **Do not** upload `vendor/` or `node_modules/` — generate them on the server.

### 5. Install Composer dependencies via cPanel Terminal

In cPanel → **Terminal** (or SSH):

```bash
cd ~/public_html/api
php8.2 /usr/local/bin/composer install --no-dev --optimize-autoloader
```

> Check which PHP version your host provides. Replace `php8.2` with the correct binary (e.g. `php81`, `ea-php82`). Must be PHP 8.2 or higher.

### 6. Configure `.env`

```bash
cp .env.example .env
php8.2 artisan key:generate
```

Then edit `.env` with your real values:

```dotenv
APP_ENV=production
APP_DEBUG=false
APP_URL=https://api.giwu.co.za

FRONTEND_URL=https://giwu.co.za

# Absolute path — find it with: pwd inside the api folder
BIBLE_DB=/home/youraccount/public_html/api/database/bible-sqlite.db

SESSION_DRIVER=file
CACHE_STORE=file
QUEUE_CONNECTION=sync
```

Upload `bible-sqlite.db` to `public_html/api/database/bible-sqlite.db`.

### 7. Fix storage permissions

```bash
cd ~/public_html/api
mkdir -p storage/framework/{views,cache/data,sessions} storage/logs
chmod -R 755 storage bootstrap/cache
php artisan storage:link
php artisan config:cache
php artisan route:cache
```

### 8. Update the frontend API URL

Before building in step 1, create `js/fn.giwu/.env.production`:

```dotenv
VITE_API_URL=https://api.giwu.co.za/api
```

Then rebuild and re-upload `dist/`.

### 9. Verify

```
https://api.giwu.co.za/api/bibles   → JSON list of versions
https://giwu.co.za                  → React app loads
```

---

## Option B — VPS (Ubuntu 22.04 / Nginx)

### 1. Install dependencies

```bash
sudo apt update && sudo apt upgrade -y

# Nginx
sudo apt install -y nginx

# PHP 8.2 + extensions
sudo apt install -y software-properties-common
sudo add-apt-repository ppa:ondrej/php -y
sudo apt install -y php8.2-fpm php8.2-cli php8.2-sqlite3 \
  php8.2-mbstring php8.2-xml php8.2-curl php8.2-zip

# Composer
curl -sS https://getcomposer.org/installer | php
sudo mv composer.phar /usr/local/bin/composer

# Node (for building the frontend)
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
```

### 2. Clone the repo

```bash
cd /var/www
sudo git clone https://github.com/youruser/giwubible.git
sudo chown -R $USER:$USER /var/www/giwubible
```

### 3. Build the frontend

```bash
cd /var/www/giwubible/js/fn.giwu

# Set API URL before building
echo "VITE_API_URL=https://api.giwu.co.za/api" > .env.production

npm install
npm run build        # dist/ is created here
```

### 4. Install backend dependencies

```bash
cd /var/www/giwubible/php/api.giwu
composer install --no-dev --optimize-autoloader
```

### 5. Configure `.env`

```bash
cp .env.example .env
php artisan key:generate
```

Edit `.env`:

```dotenv
APP_ENV=production
APP_DEBUG=false
APP_URL=https://api.giwu.co.za

FRONTEND_URL=https://giwu.co.za

BIBLE_DB=/var/www/giwubible/php/data/bible-sqlite.db

SESSION_DRIVER=file
CACHE_STORE=file
QUEUE_CONNECTION=sync

LOG_CHANNEL=stack
LOG_LEVEL=error
```

### 6. Fix permissions and cache

```bash
cd /var/www/giwubible/php/api.giwu

mkdir -p storage/framework/{views,cache/data,sessions} storage/logs
sudo chown -R www-data:www-data storage bootstrap/cache
sudo chmod -R 775 storage bootstrap/cache

php artisan storage:link
php artisan config:cache
php artisan route:cache
```

### 7. Configure Nginx

Create `/etc/nginx/sites-available/giwubible`:

```nginx
# ── Frontend (React SPA) ────────────────────────────
server {
    listen 80;
    server_name giwu.co.za www.giwu.co.za;
    root /var/www/giwubible/js/fn.giwu/dist;
    index index.html;

    # React Router — fall back to index.html for all routes
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache static assets aggressively
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2?)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}

# ── Backend (Laravel API) ───────────────────────────
server {
    listen 80;
    server_name api.giwu.co.za;
    root /var/www/giwubible/php/api.giwu/public;
    index index.php;

    location / {
        try_files $uri $uri/ /index.php?$query_string;
    }

    location ~ \.php$ {
        fastcgi_pass unix:/run/php/php8.2-fpm.sock;
        fastcgi_index index.php;
        fastcgi_param SCRIPT_FILENAME $realpath_root$fastcgi_script_name;
        include fastcgi_params;
    }

    location ~ /\.(?!well-known).* {
        deny all;
    }
}
```

Enable and reload:

```bash
sudo ln -s /etc/nginx/sites-available/giwubible /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 8. Add SSL with Certbot

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d giwu.co.za -d www.giwu.co.za -d api.giwu.co.za
```

Certbot patches both server blocks automatically and sets up auto-renewal.

### 9. Verify

```bash
curl https://api.giwu.co.za/api/bibles   # JSON response
curl https://giwu.co.za                  # HTML page
```

---

## Updating after code changes

### Frontend only

```bash
cd js/fn.giwu
npm run build
# cPanel: re-upload dist/ contents
# VPS: dist/ is already in place — Nginx serves it immediately
```

### Backend only

```bash
cd php/api.giwu
git pull                          # or re-upload changed files
composer install --no-dev -o      # if composer.json changed
php artisan config:cache
php artisan route:cache
```

### Both

Run both blocks above in order.

---

## Quick reference

| Task | Command |
|------|---------|
| Build frontend | `cd js/fn.giwu && npm run build` |
| Install backend deps | `cd php/api.giwu && composer install --no-dev -o` |
| Regenerate app key | `php artisan key:generate` |
| Clear & rebuild cache | `php artisan config:cache && php artisan route:cache` |
| Fix storage permissions | `chown -R www-data:www-data storage bootstrap/cache` |
| Test API | `curl https://api.giwu.co.za/api/bibles` |
