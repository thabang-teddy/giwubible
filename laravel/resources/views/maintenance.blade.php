<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta http-equiv="refresh" content="60">
    <title>Down for maintenance — Giwu Bible</title>
    <style>
        :root {
            --primary: #E30613;
            --primary-light: #FEF2F2;
            --gray-50: #F9FAFB;
            --gray-200: #E5E7EB;
            --gray-500: #6D6E71;
            --gray-700: #374151;
            --gray-900: #0a0a0a;
            --surface: #ffffff;
            --page-bg: #F3F4F6;
        }

        @media (prefers-color-scheme: dark) {
            :root {
                --primary-light: #2d0505;
                --gray-50: #171717;
                --gray-200: #2a2a2a;
                --gray-500: #9a9a9a;
                --gray-700: #cfcfcf;
                --gray-900: #f2f2f2;
                --surface: #141414;
                --page-bg: #0a0a0a;
                color-scheme: dark;
            }
        }

        * { box-sizing: border-box; }

        body {
            margin: 0;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 24px;
            background: var(--page-bg);
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            color: var(--gray-900);
        }

        .card {
            width: 100%;
            max-width: 600px;
            background: var(--surface);
            border-radius: 16px;
            box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04), 0 16px 40px rgba(0, 0, 0, 0.08);
            padding: 48px 40px 36px;
            text-align: center;
        }

        .wordmark {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            font-weight: 700;
            font-size: 15px;
            letter-spacing: 0.02em;
            padding-bottom: 10px;
            border-bottom: 3px solid var(--primary);
        }

        .wordmark-mark { font-size: 18px; }

        figure {
            margin: 36px 0 28px;
        }

        h1 {
            margin: 0 0 10px;
            font-family: Georgia, 'Times New Roman', serif;
            font-size: clamp(1.6rem, 1.3rem + 1.2vw, 2.1rem);
            font-weight: 700;
            line-height: 1.25;
        }

        p.lede {
            margin: 0 auto;
            max-width: 40ch;
            color: var(--gray-700);
            font-size: 15px;
            line-height: 1.6;
        }

        .footnote {
            margin-top: 30px;
            padding-top: 20px;
            border-top: 1px solid var(--gray-200);
            color: var(--gray-500);
            font-size: 12.5px;
        }
    </style>
</head>
<body>
    <main class="card" role="main">
        <span class="wordmark">
            <span class="wordmark-mark" aria-hidden="true">&#128214;</span>
            <span>GIWU BIBLE</span>
        </span>

        <figure aria-hidden="true">
            <svg width="200" height="130" viewBox="0 0 200 130" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M14 22c26-10 48-10 72 2v78c-24-11-46-11-72-2V22z" fill="var(--primary-light)" stroke="var(--primary)" stroke-width="3" stroke-linejoin="round"/>
                <path d="M186 22c-26-10-48-10-72 2v78c24-11 46-11 72-2V22z" fill="var(--primary-light)" stroke="var(--primary)" stroke-width="3" stroke-linejoin="round"/>
                <path d="M28 34c16-6 32-6 46 2M28 50c16-6 32-6 46 2M28 66c16-6 32-6 46 2" stroke="var(--gray-500)" stroke-width="2.5" stroke-linecap="round"/>
                <path d="M126 36c14-8 30-8 46-2M126 52c14-8 30-8 46-2M126 68c14-8 30-8 46-2" stroke="var(--gray-500)" stroke-width="2.5" stroke-linecap="round"/>
                <g transform="translate(78 90)">
                    <circle cx="22" cy="22" r="22" fill="var(--surface)" stroke="var(--primary)" stroke-width="3"/>
                    <path d="M28.5 14a8 8 0 0 0-10.9 9.4L11 30l3.5 3.5 6.6-6.6A8 8 0 0 0 30.6 16.9l-4.1 4.1-3.5-3.5 4.1-4.1z" fill="var(--primary)"/>
                </g>
            </svg>
        </figure>

        <h1>The site is down for maintenance</h1>
        <p class="lede">
            {{ $message ?: "We're making some improvements to Giwu Bible and will be back online shortly. Thanks for your patience." }}
        </p>

        <p class="footnote">This page refreshes automatically — no need to reload.</p>
    </main>
</body>
</html>
