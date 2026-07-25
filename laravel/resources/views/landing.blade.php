<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>{{ config('app.name') }} — API</title>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

        :root {
            --red:        #E30613;
            --red-dark:   #b0000d;
            --black:      #0a0a0a;
            --gray:       #6D6E71;
            --gray-light: #e8e8e8;
            --gray-bg:    #f5f5f5;
            --white:      #ffffff;
            --sans:       -apple-system, 'Helvetica Neue', Arial, sans-serif;
            --mono:       'Courier New', Courier, monospace;
        }

        html, body {
            min-height: 100vh;
            background: var(--white);
            color: var(--black);
            font-family: var(--sans);
            line-height: 1.5;
            -webkit-font-smoothing: antialiased;
        }

        /* ── Top bar ─────────────────────────────── */
        .topbar {
            background: var(--black);
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 2.5rem;
            height: 52px;
        }

        .topbar-brand {
            font-family: var(--mono);
            font-size: .72rem;
            letter-spacing: .14em;
            text-transform: uppercase;
            color: #fff;
        }

        .topbar-brand span { color: var(--red); }

        .topbar-status {
            display: flex;
            align-items: center;
            gap: .5rem;
            font-family: var(--mono);
            font-size: .68rem;
            letter-spacing: .1em;
            color: #999;
        }

        .status-dot {
            width: .4rem;
            height: .4rem;
            border-radius: 50%;
            background: #4caf76;
            animation: blink 2.2s ease-in-out infinite;
        }

        @keyframes blink {
            0%, 100% { opacity: 1; }
            50%       { opacity: .3; }
        }

        /* ── Hero ────────────────────────────────── */
        .hero {
            border-bottom: 1px solid var(--gray-light);
            padding: 4.5rem 2.5rem 4rem;
            display: grid;
            grid-template-columns: 1fr auto;
            align-items: end;
            gap: 2rem;
            max-width: 1100px;
            margin: 0 auto;
        }

        .hero-kicker {
            font-family: var(--mono);
            font-size: .7rem;
            letter-spacing: .18em;
            text-transform: uppercase;
            color: var(--red);
            margin-bottom: 1.1rem;
        }

        h1 {
            font-size: clamp(2.8rem, 7vw, 5rem);
            font-weight: 900;
            line-height: 1;
            letter-spacing: -.03em;
            color: var(--black);
        }

        h1 .highlight {
            color: var(--red);
        }

        .hero-sub {
            margin-top: 1.25rem;
            font-size: 1.05rem;
            color: var(--gray);
            max-width: 480px;
            line-height: 1.7;
        }

        /* Red number block – Swiss style */
        .hero-number {
            font-size: 7rem;
            font-weight: 900;
            line-height: 1;
            letter-spacing: -.04em;
            color: var(--red);
            opacity: .12;
            user-select: none;
            flex-shrink: 0;
        }

        @media (max-width: 600px) {
            .hero { grid-template-columns: 1fr; }
            .hero-number { display: none; }
        }

        /* ── CTA strip ───────────────────────────── */
        .cta-strip {
            background: var(--red);
            padding: 1.5rem 2.5rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 1.5rem;
            flex-wrap: wrap;
        }

        .cta-strip p {
            font-size: .9rem;
            color: rgba(255,255,255,.85);
            letter-spacing: .01em;
        }

        .cta-strip strong { color: #fff; }

        .btn-open {
            display: inline-flex;
            align-items: center;
            gap: .6rem;
            background: #fff;
            color: var(--red);
            padding: .65rem 1.4rem;
            font-family: var(--sans);
            font-size: .82rem;
            font-weight: 700;
            letter-spacing: .05em;
            text-transform: uppercase;
            text-decoration: none;
            border: 2px solid #fff;
            transition: background .15s, color .15s;
            white-space: nowrap;
        }

        .btn-open:hover {
            background: var(--black);
            border-color: var(--black);
            color: #fff;
        }

        /* ── Content ─────────────────────────────── */
        .content {
            max-width: 1100px;
            margin: 0 auto;
            padding: 3.5rem 2.5rem 4rem;
            display: grid;
            grid-template-columns: 260px 1fr;
            gap: 4rem;
        }

        @media (max-width: 720px) {
            .content { grid-template-columns: 1fr; gap: 2.5rem; }
        }

        /* Sidebar */
        .sidebar-label {
            font-size: .68rem;
            font-weight: 700;
            letter-spacing: .18em;
            text-transform: uppercase;
            color: var(--gray);
            margin-bottom: 1rem;
            padding-bottom: .5rem;
            border-bottom: 2px solid var(--black);
        }

        .sidebar p {
            font-size: .88rem;
            color: var(--gray);
            line-height: 1.75;
        }

        .sidebar-stat {
            margin-top: 1.75rem;
        }

        .stat-number {
            font-size: 2.4rem;
            font-weight: 900;
            line-height: 1;
            color: var(--black);
            letter-spacing: -.03em;
        }

        .stat-number span { color: var(--red); }

        .stat-label {
            font-size: .75rem;
            color: var(--gray);
            letter-spacing: .06em;
            text-transform: uppercase;
            margin-top: .2rem;
        }

        /* Endpoints panel */
        .endpoints-label {
            font-size: .68rem;
            font-weight: 700;
            letter-spacing: .18em;
            text-transform: uppercase;
            color: var(--gray);
            margin-bottom: 1rem;
            padding-bottom: .5rem;
            border-bottom: 2px solid var(--black);
        }

        .endpoints {
            display: flex;
            flex-direction: column;
            gap: 0;
        }

        .endpoint {
            display: grid;
            grid-template-columns: 3rem 1fr auto;
            align-items: center;
            gap: 1rem;
            padding: .95rem 1rem;
            border-bottom: 1px solid var(--gray-light);
            transition: background .1s;
        }

        .endpoint:first-child { border-top: 1px solid var(--gray-light); }

        .endpoint:hover { background: var(--gray-bg); }

        .method {
            font-family: var(--mono);
            font-size: .62rem;
            font-weight: bold;
            letter-spacing: .06em;
            color: var(--white);
            background: var(--black);
            padding: .18rem .35rem;
            text-align: center;
        }

        .path {
            font-family: var(--mono);
            font-size: .8rem;
            color: var(--black);
            word-break: break-all;
        }

        .desc {
            font-size: .78rem;
            color: var(--gray);
            text-align: right;
            white-space: nowrap;
        }

        @media (max-width: 520px) {
            .endpoint { grid-template-columns: 3rem 1fr; }
            .desc { display: none; }
        }

        /* ── Footer ──────────────────────────────── */
        footer {
            background: var(--black);
            padding: 1.25rem 2.5rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 1rem;
            flex-wrap: wrap;
        }

        .footer-left {
            font-family: var(--mono);
            font-size: .68rem;
            color: #555;
            letter-spacing: .06em;
        }

        .footer-left b { color: var(--red); }

        .footer-right {
            font-family: var(--mono);
            font-size: .65rem;
            color: #444;
            letter-spacing: .04em;
        }

        /* ── Red accent rule ─────────────────────── */
        .rule-red {
            height: 3px;
            background: var(--red);
            border: none;
        }
    </style>
</head>
<body>

    {{-- Top navigation bar --}}
    <nav class="topbar">
        <span class="topbar-brand"><span>Giwu</span> Bible — API</span>
        <span class="topbar-status">
            <span class="status-dot"></span>
            Operational &nbsp;·&nbsp; Laravel {{ app()->version() }}
        </span>
    </nav>

    <hr class="rule-red">

    {{-- Hero --}}
    <section class="hero">
        <div>
            <p class="hero-kicker">REST API &mdash; Data Layer</p>
            <h1>Scripture,<br><span class="highlight">verse</span><br>by verse.</h1>
            <p class="hero-sub">
                A stateless Bible API serving 50+ translations.
                Fetch chapters, compare individual verses side-by-side,
                and power readers on any platform.
            </p>
        </div>
        <div class="hero-number" aria-hidden="true">01.</div>
    </section>

    {{-- CTA strip --}}
    <div class="cta-strip">
        <p><strong>Ready to read?</strong> &nbsp;Open the web reader to browse translations side-by-side.</p>
        <a href="{{ env('FRONTEND_URL', 'http://localhost:5173') }}" class="btn-open">
            Open the Reader
            <svg width="13" height="13" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                <path d="M1 7h12M7.5 1.5L13 7l-5.5 5.5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
        </a>
    </div>

    {{-- Main content --}}
    <div class="content">

        {{-- Sidebar --}}
        <aside class="sidebar">
            <p class="sidebar-label">About</p>
            <p>
                Giwu Bible exposes a lightweight read-only API over a curated
                SQLite dataset. No authentication required in v1.
                Primary translation is KJV; any installed version may be used
                for verse comparison.
            </p>

            <div class="sidebar-stat">
                <div class="stat-number">50<span>+</span></div>
                <div class="stat-label">Translations available</div>
            </div>

            <div class="sidebar-stat">
                <div class="stat-number">66</div>
                <div class="stat-label">Books indexed</div>
            </div>

            <div class="sidebar-stat">
                <div class="stat-number">31<span>k</span></div>
                <div class="stat-label">Verses per translation</div>
            </div>
        </aside>

        {{-- Endpoints --}}
        <div>
            <p class="endpoints-label">Endpoints</p>
            <div class="endpoints">
                <div class="endpoint">
                    <span class="method">GET</span>
                    <span class="path">/api/bibles</span>
                    <span class="desc">List all translations</span>
                </div>
                <div class="endpoint">
                    <span class="method">GET</span>
                    <span class="path">/api/books</span>
                    <span class="desc">List all books</span>
                </div>
                <div class="endpoint">
                    <span class="method">GET</span>
                    <span class="path">/api/chapter?bible={table}&amp;book={b}&amp;chapter={c}</span>
                    <span class="desc">Fetch a chapter</span>
                </div>
                <div class="endpoint">
                    <span class="method">GET</span>
                    <span class="path">/api/verse?bible={table}&amp;book={b}&amp;chapter={c}&amp;verse={v}</span>
                    <span class="desc">Single verse lookup</span>
                </div>
            </div>
        </div>

    </div>

    <footer>
        <p class="footer-left"><b>{{ config('app.name') }}</b> &nbsp;·&nbsp; {{ config('app.url') }}</p>
        <p class="footer-right">KJV &middot; ASV &middot; BBE &middot; WEB &middot; YLT &middot; +45 more</p>
    </footer>

</body>
</html>
