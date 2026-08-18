#!/usr/bin/env bash
#
# Apply an already-checked-out release. Runs ON the cPanel account.
#
#   bash deploy/cpanel-deploy.sh
#
# Invoked by deploy/cpanel-poll.sh after it has fast-forwarded the working
# tree, and safe to run by hand when you want to re-apply the current checkout.
#
# It does NOT fetch, reset or choose a revision — that is the poller's job, and
# keeping the two apart means this script can be run during an incident without
# also moving the tree to whatever the deploy branch now points at.
#
# --------------------------------------------------------------------------
# It locates itself rather than being told where it is.
#
# One account hosts several applications and two stages of each. A path
# templated in from CI decides, in a different repository on a different
# machine, which database this script migrates — and the failure mode of
# getting it wrong is migrating production from a staging deploy. Deriving the
# path from the script's own location cannot disagree with where the script
# actually is.
# --------------------------------------------------------------------------

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP="${REPO_ROOT}/laravel"

# The PHP Selector wrapper, deliberately — NOT a pinned /opt/alt/phpXX path.
#
# On this account the selector has already moved once. A pinned path survives
# that move and becomes the bug it was meant to prevent: cron running one
# engine while web requests run another, with nothing anywhere saying so. The
# wrapper cannot diverge from what serves requests, because it IS what serves
# requests.
#
# MIN_PHP_ID tracks the version CI builds the artefact against. If you change
# one, change the other — that pairing is the whole contract between the
# machine that builds and the machine that runs.
PHP="${GIWU_PHP:-/usr/local/bin/php}"
MIN_PHP_ID=80200

log() { printf '[%s] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*"; }

[ -x "$PHP" ] || { log "FATAL: no PHP interpreter at ${PHP}. Set GIWU_PHP."; exit 1; }
[ -f "${APP}/artisan" ] || { log "FATAL: no artisan at ${APP}. Wrong repository root?"; exit 1; }

# Written by hand, once, per stage. Never deployed, never in the artefact.
[ -f "${APP}/.env" ] || { log "FATAL: no .env at ${APP}. See GO-LIVE.md §4."; exit 1; }

php_version="$("$PHP" -r 'echo PHP_VERSION;')"
php_version_id="$("$PHP" -r 'echo PHP_VERSION_ID;')"
if [ "$php_version_id" -lt "$MIN_PHP_ID" ]; then
    log "FATAL: PHP ${php_version} is below the ${MIN_PHP_ID} this artefact was built for."
    exit 1
fi

sha="$(git -C "$REPO_ROOT" rev-parse --short HEAD 2>/dev/null || echo unknown)"
log "Deploying ${sha} into ${APP} using PHP ${php_version} (${PHP})"

# ---------------------------------------------------------------------------
# The SQLite pre-flight. This application's users, bookmarks and sessions are a
# FILE, and the deploy runs `git reset --hard` over the tree that file used to
# live in. The .env must therefore point DB_DATABASE at an absolute path
# OUTSIDE the repository — see GO-LIVE.md §1. Checking it here, before
# migrations run, turns "silently migrated a throwaway copy and lost every
# user" into a refusal.
# ---------------------------------------------------------------------------
db_path="$(grep -E '^DB_DATABASE=' "${APP}/.env" | tail -1 | cut -d= -f2- | tr -d '"'"'"'')"

case "$db_path" in
    /*) : ;;
    *)  log "FATAL: DB_DATABASE must be an ABSOLUTE path outside the repository."
        log "       Found: '${db_path:-<unset>}'. A relative path resolves inside"
        log "       the tree this deploy overwrites. See GO-LIVE.md §1."
        exit 1 ;;
esac

case "$db_path" in
    "${REPO_ROOT}"/*)
        log "FATAL: DB_DATABASE (${db_path}) is INSIDE the repository."
        log "       git reset --hard would destroy it. Move it to ~/shared/."
        exit 1 ;;
esac

[ -f "$db_path" ] || { log "FATAL: DB_DATABASE (${db_path}) does not exist. Create it first: touch it, chmod 660."; exit 1; }
[ -w "$db_path" ] || { log "FATAL: DB_DATABASE (${db_path}) is not writable by this user."; exit 1; }

# SQLite writes a journal beside the database, so the DIRECTORY must be
# writable too — a read-only directory produces "attempt to write a readonly
# database" on a file that is plainly writable.
db_dir="$(dirname "$db_path")"
[ -w "$db_dir" ] || { log "FATAL: ${db_dir} is not writable. SQLite needs to write its journal there."; exit 1; }

# The read-only source data ships with the artefact. Its absence means a
# truncated checkout, and the symptom would be an empty Bible rather than an
# error.
[ -f "${APP}/database/bible-sqlite.db" ] || { log "FATAL: bible-sqlite.db missing from the checkout."; exit 1; }

cd "$APP"

# Maintenance mode FIRST. A git reset over a live tree is not an atomic swap,
# and this is the whole mitigation: visitors get a 503 for a few seconds
# instead of new PHP running against a stale route cache.
#
# `|| true` because `down` fails when already down, and a redeploy after a
# failed deploy must not be blocked by its own safety measure.
log 'Entering maintenance mode'
"$PHP" artisan down --retry=15 || true

# Always report the failure, and leave maintenance mode ON when there is one:
# a half-applied release must not serve traffic.
finish() {
    status=$?
    if [ "$status" -ne 0 ]; then
        log "FAILED at exit ${status}. Leaving maintenance mode ON deliberately:"
        log '  a half-applied release must not serve traffic. Fix, then re-run'
        log "  this script, or clear it by hand with: ${PHP} artisan up"
    fi
    exit "$status"
}
trap finish EXIT

# Clear before caching: a cache left by an experiment would otherwise survive.
#
# Unlike fiyobra, this application DOES cache its config. It has no boot guard
# reading env() behind config()'s back — the only env() call outside config/ is
# in resources/views/landing.blade.php, which no route renders. If that ever
# changes, or you add an env() call in application code, this line becomes a
# `config:clear` and the reason goes here.
log 'Caching configuration'
"$PHP" artisan config:clear
"$PHP" artisan config:cache

log 'Running migrations'
"$PHP" artisan migrate --force

log 'Caching routes and views'
"$PHP" artisan route:cache
"$PHP" artisan view:cache

log 'Ensuring the storage symlink exists'
"$PHP" artisan storage:link || true

log 'Leaving maintenance mode'
"$PHP" artisan up

log "Deployed ${sha} successfully"

# storage/ is NEVER deleted by this script, and neither is the database. The
# database lives outside the tree (checked above). storage/ is inside it but
# git-ignored, so `git reset --hard` leaves it alone — including
# public/downloads/, which holds the built Android APK that the download page
# links to. If anyone adds a clean step above, that is the line that destroys
# both.
