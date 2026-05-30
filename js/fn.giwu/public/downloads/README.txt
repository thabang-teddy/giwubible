Giwu Bible — Download Assets
=============================

App binaries are now served from the Laravel backend (php/api.giwu/public/downloads/).
This directory is intentionally empty.

To add download files, place them in:
  php/api.giwu/public/downloads/giwu-bible-android.apk
  php/api.giwu/public/downloads/giwu-bible-windows-setup.exe

The React download page reads VITE_SERVER_URL from the .env file and builds
the full download URLs at build time.
