#!/usr/bin/env bash
#
# Pull a new release, if there is one, and apply it. Runs ON the cPanel
# account. It is invoked by the account-level poller that runs every five
# minutes (GO-LIVE.md §6), not by its own cron entry — the plan is capped at
# seven cron jobs and several applications share them.
#
# Safe to run by hand:
#
#   bash /home/<user>/production/giwubible/deploy/cpanel-poll.sh
#
# --------------------------------------------------------------------------
# Why cPanel pulls instead of GitHub pushing
#
# The alternative is an SSH key for the account held as a GitHub Actions
# secret. On shared hosting that key is not scoped to a deploy — it is the
# account: every application on it, every database, every backup. A compromised
# repository, or a workflow anyone can trigger, reaches all of them.
#
# Inverting the direction removes the credential entirely. GitHub pushes only
# to its own repository; cPanel authenticates to GitHub with a READ-ONLY deploy
# key. Neither side holds anything that can damage the other.
#
# The cost is honest: up to five minutes of latency, and a failed deploy
# surfaces in this log rather than in the Actions run. A green tick in Actions
# means the artefact was built, NOT that it is live.
# --------------------------------------------------------------------------

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Named after the tree, not the application. Two stages of giwubible run from
# one account and must not share a lock: staging deploying is no reason for
# production to skip its release.
LOCK_FILE="${GIWU_LOCK:-${HOME}/tmp/$(echo "$REPO_ROOT" | tr '/' '-' | sed 's/^-//').lock}"

log() { printf '[%s] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*"; }

# Take the lock or leave. A deploy that overruns the interval must not have the
# next run start a second one on top of it — two `git reset --hard`s and two
# `migrate`s interleaving over one tree is how a half-migrated database ends up
# serving traffic.
#
# Held on a file descriptor for the life of the process, rather than by
# re-exec'ing under flock. `exec flock ... || fallback` is a common shape and a
# broken one: exec REPLACES this process, so the fallback after it can never
# run, and a busy lock exits non-zero — which for a job that is contended by
# design means cron mails a failure every time the previous deploy is merely
# still working.
#
# A busy lock is a normal outcome here, so it exits 0 and says so.
mkdir -p "$(dirname "$LOCK_FILE")"

if command -v flock > /dev/null 2>&1; then
    exec 9> "$LOCK_FILE"
    if ! flock --nonblock 9; then
        log 'Another deploy holds the lock. Leaving it to finish.'
        exit 0
    fi
else
    # mkdir is atomic on every POSIX filesystem, which is the whole reason it
    # is the fallback rather than `[ -f lock ]`.
    LOCK_DIR="${LOCK_FILE}.d"
    if ! mkdir "$LOCK_DIR" 2> /dev/null; then
        log 'Another deploy holds the lock (mkdir fallback). Leaving it to finish.'
        exit 0
    fi
    # Released on ANY exit, including a failed deploy. Without this a single
    # crash wedges every subsequent run, silently, until someone finds the
    # directory by hand.
    trap 'rmdir "$LOCK_DIR" 2> /dev/null || true' EXIT
fi

cd "$REPO_ROOT"

branch="$(git rev-parse --abbrev-ref HEAD)"
remote_ref="$(git rev-parse --abbrev-ref '@{u}' 2>/dev/null || echo "origin/${branch}")"

git fetch --quiet origin "$branch"

local_sha="$(git rev-parse HEAD)"
remote_sha="$(git rev-parse "$remote_ref")"

if [ "$local_sha" = "$remote_sha" ]; then
    # Deliberately quiet. This runs 288 times a day and all but a few are
    # no-ops; logging each one buries the deploys that matter.
    exit 0
fi

log "New release on ${remote_ref}: ${local_sha:0:7} -> ${remote_sha:0:7}"

# --hard, because the deploy branch is an ARTEFACT that CI force-pushes. Its
# history is rewritten every build, so merging is meaningless and a fast-
# forward is not always possible.
#
# What survives this: .env and storage/ are git-ignored, and the user database
# lives outside the tree entirely (cpanel-deploy.sh refuses to run otherwise).
git reset --hard "$remote_sha"

bash "${REPO_ROOT}/deploy/cpanel-deploy.sh"
