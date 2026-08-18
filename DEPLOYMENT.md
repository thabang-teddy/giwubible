# Deployment

This file used to describe deploying two separate applications, `php/api.giwu/`
and `js/fn.giwu/`, to two subdomains. Neither directory exists any more: the
React frontend and the Laravel API were merged into a single Inertia app under
`laravel/`, and everything it said about document roots, build output and the
`BIBLE_DB` path was wrong as a result.

**The current plan is [GO-LIVE.md](GO-LIVE.md).** It covers the topology, the
cPanel setup, the artefact-branch pipeline in
[`.github/workflows/ci.yml`](.github/workflows/ci.yml), and the scripts in
[`deploy/`](deploy/) that the account runs to pull a release.

Local development is in [CLAUDE.md](CLAUDE.md) under "Build & Run Commands".
