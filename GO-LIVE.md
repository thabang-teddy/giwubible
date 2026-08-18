# Go-live plan — Giwu Bible on cPanel

Target: two deployed stages on the **existing** cPanel account, deployed from
GitHub without GitHub ever holding a credential for the server.

| Stage | Branch | Host | Deployed by |
|---|---|---|---|
| Development | feature branches | `giwubible.test` | nobody — runs on your machine |
| Staging | `staging` | `https://staging.giwu.co.za` | the account, pulling |
| Production | `master` | `https://giwu.co.za` | the account, pulling |

---

## 0. What changed from the previous version of this plan

The earlier plan built the release in CI and **pushed** it to the server over
SSH, with the `.env` written from a GitHub secret. This one inverts the
direction. The reason is specific rather than theoretical: this is a **shared
account that also hosts fiyobra**, and an SSH key with write access to it is not
scoped to a deploy — it is every application on the account and every backup. A
compromised repository, or a workflow anyone can trigger, reaches all of them.

So: **CI builds an artefact branch and pushes only to its own repository. The
account pulls it with a read-only deploy key.** Neither side holds anything that
can damage the other. The cost is up to five minutes of deploy latency, and a
failed deploy surfaces in the server's log rather than in the Actions run.

Two things from the previous plan survive unchanged, because they were right:

- **Persistent state lives outside the deploy target.** `.env`, the user
  database and `storage/` must not sit in a directory the deploy overwrites.
- **Only `laravel/public` is ever a document root.** Everything else stays above
  it and unreachable over HTTP.

What made "`git pull` on the server" look impossible before was the absence of
Node on shared hosting. The artefact branch removes that: CI ships `vendor/` and
a freshly built `public/build` inside the branch the server pulls, so the server
needs neither Node nor Composer — only `git`.

---

## 1. Target topology

### Hosts

Two, both on the existing account (`ikysqf4z615e`), with `giwu.co.za` added as an
**addon domain** and `staging.giwu.co.za` as a subdomain of it.

### Directories

```
/home/ikysqf4z615e/
├── production/giwubible/          git working tree, branch deploy/production
│   └── laravel/
│       ├── .env                   0600, written by hand, ABOVE the docroot
│       ├── public/                the document root for giwu.co.za
│       ├── storage/               logs, cache, compiled views
│       └── database/
│           └── bible-sqlite.db    42 MB, read-only, ships in the artefact
├── staging/giwubible/             the same, branch deploy/staging
├── shared/giwubible/
│   ├── production/database.sqlite users and bookmarks. NOT in any git tree
│   └── staging/database.sqlite
├── bin/                           the account-level cron dispatchers (§5)
├── backups/
└── tmp/                           cron lock files
```

**The one rule that matters:** `DB_DATABASE` is an **absolute path into
`shared/`**, never a path inside the repository. The deploy runs `git reset
--hard` over the tree, and a user database inside it is a database waiting to be
deleted. `deploy/cpanel-deploy.sh` refuses to run if `DB_DATABASE` is relative
or resolves inside the repository — that check is the whole safety net, so do
not weaken it.

`storage/` stays inside the tree. It is git-ignored, and `git reset --hard` does
not touch ignored files. That includes `public/downloads/`, which holds the
built APK the download page links to.

### Branches

| Branch | Role | Who writes it |
|---|---|---|
| `master` | Source of truth for production. Protected | You, via PR |
| `staging` | Source of truth for staging | You, via PR or merge |
| `deploy/production` | **Build artefact.** Carries `vendor/` and `public/build` | GitHub Actions only. Force-pushed |
| `deploy/staging` | Build artefact for staging | GitHub Actions only. Force-pushed |

Never branch off a `deploy/*` branch — its history is rewritten every build.
Two long-lived branches rather than the previous plan's three: `dev` earned
nothing that a feature branch and a PR do not.

---

## 2. Phase 0 — verify the host before writing anything

Most of this is already answered, because fiyobra measured the same account on
2026-08-15. Carried forward:

| | Found | Consequence here |
|---|---|---|
| Platform | CloudLinux + PHP Selector | PHP lives under `/opt/alt/`, but **do not pin that path** — see the next row |
| PHP binary | `/usr/local/bin/php` → **8.4.22** | The selector wrapper is what serves requests, so it is what cron must use. A pinned `/opt/alt/php83` path silently diverged once already |
| `git` | present | The whole deploy mechanism rests on this one binary |
| Node / npm | **absent** | Why the artefact branch exists |
| `flock` | `/bin/flock` | Real locks for the cron dispatchers |
| `symlink()` | works | `storage:link` works |
| Apache runs as | `nobody` | §4.3. This is the one that cost hours on fiyobra |
| Disk | 907 GB free | The 42 MB bible database × 2 stages is nothing |
| Cron jobs | **7 maximum** | **The binding constraint.** See §5 |
| Databases | 2 maximum | Irrelevant — this app is SQLite |

### 2.1 PHP 8.4 — answered, by upgrading the framework

**Resolved on 2026-08-18.** This section originally read: `composer.json`
requires `^8.1` and the framework is **Laravel 10.48**, which predates full PHP
8.4 support — a go/no-go gate.

The gate is gone, because the framework is no longer Laravel 10. `composer
audit` (a blocking CI step) reported three advisories against laravel/framework
10.50.2, including a CRLF injection in the default `email` validation rule that
registration uses. There is **no patched Laravel 10** — the fixes ship in
12.60.0+ — and Laravel 10's security support had already lapsed. So the
framework was upgraded rather than the advisories being suppressed: first to
12.67, then to **Laravel 13.26**, the current major. `composer.json` now
requires `^8.3` (Laravel 13's floor), and `MIN_PHP_ID` in
`deploy/cpanel-deploy.sh` was raised to `80300` to match. The account serves
8.4.22, clear of it.

The legacy application skeleton was kept — `app/Http/Kernel.php`,
`RouteServiceProvider`, the full `config/` directory — because Laravel 11, 12
and 13 all still support it. That is what kept a two-major jump to a dependency
upgrade rather than a rewrite. Adopting the slim skeleton is optional, separate,
and owed nothing.

**One behaviour change needed application code, and it is worth knowing about.**
Laravel 13 removed the implicit `?? route('login')` fallback in Foundation's
exception handler. With no redirect callback registered, a non-JSON request to a
route behind `auth` no longer redirects — it returns a **bodyless 401**. On this
app that is `/bookmarks` and `/profile`: a logged-out visitor would have got a
blank page instead of the login form. `AppServiceProvider::boot()` now registers
`Authenticate::redirectUsing(fn () => route('login'))`, restoring the old
behaviour exactly. JSON requests never reach that branch, so the API is
untouched — verified below.

The API contract was verified rather than assumed: every public `/api` response,
both error envelopes, and the token-authenticated routes were captured on
Laravel 10 / Sanctum 3 and again on Laravel 13 / Sanctum 4 and compared. **Byte
identical**, including the `{"\"t\"":"t"}` quoted-identifier artefact in
`/api/books` that the Flutter app has always received. The unauthenticated
edges were compared across 10, 12 and 13 too, with and without an `Accept:
application/json` header — identical in every combination. Route count is 28 on
10 and on 13.

The suite still runs on 8.4 in CI permanently:

```bash
cd laravel && php artisan test
```

13 tests, 75 assertions, green on 8.4 against Laravel 13 — and green from a
clean clone, not just a working copy. A regression is now caught by the pipeline
rather than by production.

---

## 3. Phase 1 — repository changes

Already committed by this change:

| File | What it does |
|---|---|
| `deploy/cpanel-deploy.sh` | Applies a checked-out release: maintenance mode, migrate, caches, up. Refuses to run if the database is inside the tree |
| `deploy/cpanel-poll.sh` | Fetches the artefact branch, resets to it, calls the above. Locked, and quiet when idle |
| `.github/workflows/ci.yml` | `php` / `js` / `flutter` checks, plus the `cpanel` job that publishes the artefact |

Still to do, all small, all before the first deploy:

1. **Create the `staging` branch** from `master`, and set branch protection on
   both requiring the `php` and `js` checks.
2. **`.env.example`: the session and cache drivers.** It says
   `SESSION_DRIVER=database` and `CACHE_STORE=database` against a SQLite
   database. Every request would then take a write lock on the same file that
   holds your users, and the symptom under any concurrency is
   `database is locked` — an error that reads like corruption and is not. Use
   `file` for both. `QUEUE_CONNECTION=sync`, because the app has no queued jobs
   and no `Kernel::schedule()` entries; that is also why this application needs
   **no queue worker and no scheduler cron**.
3. **`.env.example`: the stale `BIBLE_DB` line.** It still points at
   `/absolute/path/to/bible-sqlite.db` from before the merge into one Laravel
   app. The default is now correct, so comment the line out — as written it
   breaks `php artisan test` on a fresh clone, CI included.
4. **Run `./vendor/bin/pint` once and commit the result.** CI runs
   `pint --test` as a blocking check, and the first run on an unformatted
   codebase will fail it.
5. **Delete `resources/views/landing.blade.php` or fix its `env()` call.** It is
   the only `env()` outside `config/`, it links to `http://localhost:5173`, and
   no route renders it — dead code from before the React and API apps merged.
   It matters because the deploy caches config, and cached config makes `env()`
   return null: if that view is ever rendered, the button goes to localhost.
6. **Either add `eslint` with a config, or drop the `lint` script from
   `package.json`.** Today the script exists and cannot run, which is why CI has
   no lint step. A check that passes because the tool is missing is worse than
   no check.
7. **`DEPLOYMENT.md` is stale** — it documents `php/api.giwu/` and
   `js/fn.giwu/`, which no longer exist. Delete it, or replace it with a pointer
   to this file.
8. **Consider re-ignoring `public/build/`.** It is committed today (the
   `/public/build` line in `laravel/.gitignore` is commented out), and CI now
   rebuilds it into the artefact anyway. Leaving it committed means every build
   produces merge noise from regenerated hashed filenames. The tradeoff is that
   a by-hand deploy would then need a local `npm run build` first.

---

## 4. Phase 2 — cPanel setup

### 4.1 Domains

cPanel → Domains → Create:

| Domain | Document root |
|---|---|
| `giwu.co.za` (addon) | `/home/ikysqf4z615e/production/giwubible/laravel/public` |
| `staging.giwu.co.za` | `/home/ikysqf4z615e/staging/giwubible/laravel/public` |

Point DNS at the account before creating them, or AutoSSL cannot validate.

### 4.2 The deploy key and the clones

1. cPanel → SSH Access → Manage SSH Keys → generate a key, no passphrase.
2. GitHub → repository → Settings → Deploy keys → Add, **read-only**.
3. cPanel → Git Version Control → Create, twice:

| Path | Branch |
|---|---|
| `/home/ikysqf4z615e/staging/giwubible` | `deploy/staging` |
| `/home/ikysqf4z615e/production/giwubible` | `deploy/production` |

The artefact branches must exist before you can clone them, so push to `staging`
once and let CI publish `deploy/staging` first. Create staging first and prove
the whole chain on it before production exists at all: a production tree that
exists but has never deployed is a thing you will eventually mistake for one
that has.

### 4.3 The permissions trap

cPanel's Git Version Control creates the intermediate directories `0700`. Apache
runs as `nobody` on this account and cannot traverse them, and the symptom is a
**403 on a correctly configured document root** — which sends you debugging the
document root, the `.htaccess` and the certificate, all of which are fine. Fix
it immediately after cloning:

```bash
chmod 711 ~/staging ~/staging/giwubible ~/production ~/production/giwubible
```

`711` matches the home directory: traverse-through, no listing. Verify every
level has an `x` for "other":

```bash
namei -l ~/production/giwubible/laravel/public/index.php
```

### 4.4 SSL, and the trap behind it

cPanel → SSL/TLS Status → tick both hosts → **Run AutoSSL**.

Then verify HTTPS **separately from HTTP**. On this account the SSL vhost has
been observed serving a stale document root while the plain-HTTP vhost served
the correct one: the site works on `http://` and 403s on `https://`, which is
the only symptom users ever see, because `.htaccess` redirects them there. The
two commands that tell those apart:

```bash
curl -sI -H "Host: giwu.co.za" http://127.0.0.1/ | head -1
```

```bash
curl -sI https://giwu.co.za/ | head -1
```

If the first is 200 and the second is 403, the SSL vhost is stale. Reinstall the
certificate from SSL/TLS → Manage SSL Sites, or change the document root to
something else and back to force a rebuild. If neither works it is a support
ticket asking them to rebuild the Apache configuration for that host — say that
HTTP serves the correct root and HTTPS does not, and it becomes a two-minute
ticket.

### 4.5 The `.env` files

Written by hand, once, per stage. **Never committed, never in the artefact,
never a CI secret.** `chmod 600`. They sit at `<tree>/laravel/.env`, above the
document root.

```dotenv
APP_NAME="Giwu Bible"
APP_ENV=production
APP_KEY=                         # php artisan key:generate --show
APP_DEBUG=false
APP_URL=https://giwu.co.za

# ABSOLUTE, and outside the repository. cpanel-deploy.sh refuses anything else.
DB_CONNECTION=sqlite
DB_DATABASE=/home/ikysqf4z615e/shared/giwubible/production/database.sqlite

# NOT database — see §3.2. Sessions and cache on SQLite take a write lock on
# the same file that holds your users, on every request.
SESSION_DRIVER=file
CACHE_STORE=file
QUEUE_CONNECTION=sync

SESSION_SECURE_COOKIE=true       # only once the certificate is live, or nobody
                                 # can log in and nothing says why

LOG_CHANNEL=daily                # rotates itself; §5's nightly job is a backstop
LOG_LEVEL=warning

MAIL_MAILER=log                  # until a real transport is chosen
```

Create the shared directories first — SQLite writes its journal beside the
database, so the **directory** must be writable, not just the file:

```bash
mkdir -p ~/shared/giwubible/production ~/shared/giwubible/staging
```

For a new database, `migrate` creates the schema but the file must exist first:

```bash
touch ~/shared/giwubible/production/database.sqlite && chmod 660 ~/shared/giwubible/production/database.sqlite
```

If you are carrying over the existing `laravel/database/database.sqlite`, copy
it there instead — and take a copy of the original before you do.

---

## 5. Phase 3 — cron, and the account-wide budget

**The plan allows seven cron jobs for the whole account, and fiyobra alone wants
eight.** Adding giwubible naively takes it to eleven. The answer is not to drop
work; it is to stop giving every job its own cron entry.

Three account-level dispatchers in `~/bin/`, covering **every application and
every stage**:

| Cron | Runs | Does |
|---|---|---|
| `*/5 * * * * ~/bin/poll-all.sh` | every 5 min | every application's `cpanel-poll.sh` |
| `* * * * * ~/bin/minute-all.sh` | every minute | fiyobra's queue and scheduler, both stages. **giwubible contributes nothing here** |
| `30 2 * * * ~/bin/nightly-all.sh` | nightly | backups and log pruning for everything |

Three entries, four to spare. `~/bin/poll-all.sh`:

```bash
#!/bin/bash
# Deliberately NOT `set -e`: one application failing to deploy must not stop the
# others from trying. Each poll script takes its own lock and reports its own
# tree.
for tree in ~/staging/giwubible ~/production/giwubible ~/staging/fiyobra ~/production/fiyobra; do
    [ -x "$tree/deploy/cpanel-poll.sh" ] || continue
    bash "$tree/deploy/cpanel-poll.sh" || echo "!!! $tree deploy FAILED"
done
```

Cron it once, into one log:

```bash
*/5 * * * * /bin/bash /home/ikysqf4z615e/bin/poll-all.sh >> /home/ikysqf4z615e/logs/deploy.log 2>&1
```

That log is the **only** report of a deploy. A green tick in Actions means the
artefact was built, not that it is live.

---

## 6. Phase 4 — validate on staging

Every row green on staging before production is created.

| # | Check | How |
|---|---|---|
| S.1 | The site serves over **HTTPS**, not just HTTP | `curl -sI https://staging.giwu.co.za/` → 200 |
| S.2 | `.env` is not web-reachable | `curl -sI https://staging.giwu.co.za/.env` → 404 |
| S.3 | `.git` is not web-reachable | `curl -sI https://staging.giwu.co.za/.git/config` → 404 |
| S.4 | `vendor/` is not web-reachable | `curl -sI https://staging.giwu.co.za/vendor/autoload.php` → 404 |
| S.5 | The reader actually reads | load a chapter, click a verse, confirm the second translation appears |
| S.6 | Registration and login work | this is what proves the session driver |
| S.7 | A bookmark survives a redeploy | deploy twice, confirm it is still there. **This is the check that proves the database is outside the tree** |
| S.8 | A release lands | push to `staging`, wait for CI, watch `~/logs/deploy.log` |
| S.9 | A failed deploy fails safe | break the `.env` deliberately, redeploy, confirm the site stays in maintenance mode rather than serving a half-applied release |
| S.10 | The Flutter app still talks to it | point a debug build at staging's `/api` and confirm the response shapes are unchanged |

S.7 and S.9 are the two worth doing properly. The rest is `curl`.

### Troubleshooting, from the last time this was done

- **500 straight after the first deploy** — `APP_KEY` empty, `storage/` not
  writable, or `DB_DATABASE` pointing at a file that does not exist. Read
  `laravel/storage/logs/laravel.log`; set `LOG_LEVEL=debug` temporarily if it is
  empty.
- **Logins fail silently** — `SESSION_SECURE_COOKIE=true` before the certificate
  is live.
- **Old code still served after a deploy** — OPcache. Most cPanel builds ship
  `opcache.validate_timestamps=1` and clear themselves within seconds; if yours
  does not, set it in Select PHP Version → Options.
- **`database is locked`** — the session or cache driver is still `database`.

---

## 7. Phase 5 — backups

Two things, and the second is the one people forget:

1. `shared/giwubible/*/database.sqlite` — every user and every bookmark.
2. `storage/app/` and `public/downloads/` — anything uploaded or built in place.

`bible-sqlite.db` needs no backup: it is read-only source data and ships in
every artefact.

SQLite must be backed up with its own tooling, not `cp`. A copy taken mid-write
is a corrupt copy; `.backup` is atomic where `cp` is not:

```bash
sqlite3 ~/shared/giwubible/production/database.sqlite ".backup '$HOME/backups/giwu-$(date -u +%Y%m%dT%H%M%SZ).sqlite'"
```

Put that in `~/bin/nightly-all.sh`, gzip it, and **copy it off the account** —
rclone to a B2/R2 bucket with a key that can write and list but not delete. A
backup on the same disk, in the same account, is inside the blast radius of
whatever destroys the original. Then restore one into staging and confirm the
site comes up: a backup nobody has restored is a hypothesis.

---

## 8. Phase 6 — cutover and rollback

1. Staging green, every row of §6.
2. Back up the existing production data, wherever it currently lives.
3. Create the production tree, `.env`, database file, document root and
   certificate (§4).
4. Merge `staging` → `master`. Actions publishes `deploy/production`.
5. Watch `~/logs/deploy.log` to completion. A failure leaves the site in
   maintenance mode — that is the design, not a bug.
6. Walk §6 against production.
7. Point the Flutter app's base URL at `https://giwu.co.za`, rebuild the APK,
   and put it in `public/downloads/`.
8. Announce it.

**Rollback is roll-forward.** There are no release directories, and the artefact
branches are force-pushed, so their history is not a rollback mechanism.
Reverting means `git revert` on `master` and letting CI rebuild — roughly four
minutes. That is only safe if the migration in step 4 was backward-compatible;
if it was not, say so *before* the deploy rather than during the incident. Your
data is in `~/shared/`, so no rollback can touch it.

---

## 9. Accepted risks

| Item | Why it is accepted | Revisit when |
|---|---|---|
| Deploys are not atomic | `git reset` over a live tree, mitigated by maintenance mode | Downtime becomes visible to users |
| Rollback takes ~4 minutes | No release directories, no registry | You want instant rollback — then it is `releases/<sha>` plus a `current` symlink, and the 42 MB bible database must move to `shared/` first or every release costs 42 MB |
| Up to 5 minutes of deploy latency | The price of the account pulling instead of GitHub holding a key to it | Never, on this hosting |
| SQLite, single writer | The read path is the whole product and it is read-only; writes are logins and bookmarks | Concurrent writes start timing out — then it is MySQL, and the account has a database slot free |
| Shared account with fiyobra | One quota, one PHP version, one mail reputation, one cron budget | Either application's traffic threatens the other |
| ~~Laravel 10 on PHP 8.4~~ **Resolved** | Upgraded to Laravel 13.26 on 2026-08-18 — see §2.1. The owed upgrade was paid rather than deferred, because `composer audit` had no other honest outcome, and taken to the current major rather than stopping at 12 | Laravel 13's support window lapses — but this is now the newest major, not two behind |
| No `TrustProxies` middleware | Correct on plain cPanel Apache | You put Cloudflare in front with flexible SSL — then URL generation emits `http://` and you get mixed content |
| Android app not in this pipeline | It ships through the Play Store; CI checks it but does not gate the web release on it | The app and the API need to release together |

---

## 10. Order of work

**Stage A — decide.** Run the PHP 8.4 gate (§2.1). Everything below assumes it
passed.

**Stage B — repository.** The eight items in §3: `staging` branch and
protection, the two `.env.example` fixes, `pint` once, the dead landing view,
the `lint` script, `DEPLOYMENT.md`, and the `public/build` decision. Merge
behind green CI.

**Stage C — staging, end to end.** Addon domain and subdomain; deploy key; clone
`deploy/staging`; `chmod 711`; `.env` and the shared database; AutoSSL and the
HTTPS-versus-HTTP check; the `poll-all.sh` cron; then every row of §6.

**Stage D — production.** Backups working and one restore proved; production
tree, `.env`, database, document root, SSL; merge `staging` → `master`; walk §6
again; watch it for an hour.

Nothing in a later stage is worth starting before the earlier one is done. The
two steps that can invalidate the rest are the PHP gate and the first staging
deploy — do both before believing any of this.

### The steady state

```
feature branch → PR → staging → CI → staging.giwu.co.za → you verify
                                                              │
                                    PR staging → master → CI → giwu.co.za
```
