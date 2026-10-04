import { Head, Link } from '@inertiajs/react'

function formatSize(bytes) {
  return bytes >= 1048576 ? `${(bytes / 1048576).toFixed(1)} MB` : `${Math.round(bytes / 1024)} KB`
}

function formatDate(iso) {
  return iso ? new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' }) : null
}

const URL_RE = /(https?:\/\/[^\s)]+)/g

// Release notes are GitHub-flavoured markdown; headings, bullets and links are
// all the generated notes use, so render those without pulling in a parser.
function linkify(text) {
  return text.split(URL_RE).map((part, i) =>
    i % 2 === 1
      ? <a key={i} href={part} target="_blank" rel="noopener noreferrer">{part.replace(/^https?:\/\/(www\.)?github\.com\//, '')}</a>
      : part
  )
}

function ReleaseNotes({ notes }) {
  // Drop **bold** markers (e.g. "**Full Changelog**") rather than show raw asterisks.
  const lines = notes.split(/\r?\n/).map(l => l.trim().replace(/\*\*(.+?)\*\*/g, '$1')).filter(Boolean)
  if (lines.length === 0) return <p className="release-notes-empty">No release notes for this build.</p>

  return (
    <div className="release-notes">
      {lines.map((line, i) => {
        if (/^#+\s/.test(line)) return <h3 key={i}>{line.replace(/^#+\s*/, '')}</h3>
        if (/^[*-]\s/.test(line)) return <p key={i} className="release-notes-item">{linkify(line.replace(/^[*-]\s*/, ''))}</p>
        return <p key={i}>{linkify(line)}</p>
      })}
    </div>
  )
}

function GithubLinks({ repoUrl }) {
  return (
    <div className="release-github">
      <p className="release-github-head">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path d="M12 .5C5.65.5.5 5.65.5 12c0 5.08 3.29 9.39 7.86 10.91.58.1.79-.25.79-.56v-2c-3.2.7-3.87-1.37-3.87-1.37-.52-1.33-1.28-1.69-1.28-1.69-1.04-.71.08-.7.08-.7 1.16.08 1.76 1.19 1.76 1.19 1.03 1.76 2.7 1.25 3.36.96.1-.75.4-1.25.73-1.54-2.55-.29-5.24-1.28-5.24-5.69 0-1.26.45-2.28 1.19-3.09-.12-.29-.52-1.46.11-3.05 0 0 .97-.31 3.17 1.18a11 11 0 0 1 5.77 0c2.2-1.49 3.17-1.18 3.17-1.18.63 1.59.23 2.76.11 3.05.74.81 1.19 1.83 1.19 3.09 0 4.42-2.69 5.39-5.25 5.68.41.36.78 1.06.78 2.14v3.17c0 .31.21.67.8.56A11.5 11.5 0 0 0 23.5 12C23.5 5.65 18.35.5 12 .5z" />
        </svg>
        Open source on GitHub
      </p>
      <div className="release-github-actions">
        <a className="download-btn download-btn--secondary" href={repoUrl} target="_blank" rel="noopener noreferrer">Repository</a>
        <a className="download-btn download-btn--secondary" href={`${repoUrl}/releases`} target="_blank" rel="noopener noreferrer">All releases</a>
      </div>
    </div>
  )
}

function ReleasePanel({ release, repoUrl }) {
  const date = release ? formatDate(release.published_at) : null

  return (
    <aside className="release-panel" aria-labelledby="release-title">
      <p className="release-panel-label">Latest release</p>
      {release ? (
        <>
          <h2 id="release-title" className="release-panel-title">
            {release.tag}
            {release.prerelease && <span className="release-badge">Pre-release</span>}
          </h2>
          {date && <p className="release-panel-date">Released {date}</p>}
          <ReleaseNotes notes={release.notes} />
          <a className="release-notes-link" href={release.url} target="_blank" rel="noopener noreferrer">
            Full notes on GitHub →
          </a>
        </>
      ) : (
        <p id="release-title" className="release-notes-empty">Couldn't load the latest release.</p>
      )}
      <GithubLinks repoUrl={repoUrl} />
    </aside>
  )
}

const DOWNLOADS = [
  {
    id: 'android',
    label: 'Android',
    badge: 'APK',
    description: 'For Android phones and tablets (Android 5.0+)',
    icon: (
      <svg width="36" height="36" viewBox="0 0 24 24" fill="currentColor">
        <path d="M6 18c0 .55.45 1 1 1h1v3.5c0 .83.67 1.5 1.5 1.5s1.5-.67 1.5-1.5V19h2v3.5c0 .83.67 1.5 1.5 1.5s1.5-.67 1.5-1.5V19h1c.55 0 1-.45 1-1V8H6v10zM3.5 8C2.67 8 2 8.67 2 9.5v7c0 .83.67 1.5 1.5 1.5S5 17.33 5 16.5v-7C5 8.67 4.33 8 3.5 8zm17 0c-.83 0-1.5.67-1.5 1.5v7c0 .83.67 1.5 1.5 1.5s1.5-.67 1.5-1.5v-7c0-.83-.67-1.5-1.5-1.5zm-4.97-5.84l1.3-1.3c.2-.2.2-.51 0-.71-.2-.2-.51-.2-.71 0l-1.48 1.48C14.15 1.23 13.1 1 12 1c-1.1 0-2.15.23-3.12.63L7.4.15c-.2-.2-.51-.2-.71 0-.2.2-.2.51 0 .71l1.31 1.3C6.1 3.26 5 5.01 5 7h14c0-1.99-1.1-3.74-2.47-4.84zM10 5H9V4h1v1zm5 0h-1V4h1v1z"/>
      </svg>
    ),
    primary: true,
  },
  {
    id: 'windows',
    label: 'Windows',
    badge: 'EXE',
    description: 'For Windows 10 and 11 (64-bit)',
    icon: (
      <svg width="36" height="36" viewBox="0 0 24 24" fill="currentColor">
        <path d="M0 3.449L9.75 2.1v9.451H0m10.949-9.602L24 0v11.4H10.949M0 12.6h9.75v9.451L0 20.699M10.949 12.6H24V24l-12.9-1.801"/>
      </svg>
    ),
    primary: false,
  },
]

function DownloadButton({ item, asset }) {
  if (!asset) {
    return (
      <div className="download-btn-wrap">
        <span className="download-btn download-btn--secondary download-btn--unavailable" aria-disabled="true">
          Not published yet
        </span>
      </div>
    )
  }

  // /download/{platform} redirects to the GitHub asset, which is served as an
  // attachment: the browser downloads it in place and the page stays open.
  return (
    <div className="download-btn-wrap">
      <a
        className={`download-btn ${item.primary ? 'download-btn--primary' : 'download-btn--secondary'}`}
        href={`/download/${item.id}`}
        aria-label={`Download ${item.label} (${asset.name})`}
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
          <polyline points="7 10 12 15 17 10"/>
          <line x1="12" y1="15" x2="12" y2="3"/>
        </svg>
        Download · {formatSize(asset.size)}
      </a>
    </div>
  )
}

export default function Download({ release, repoUrl }) {
  return (
    <div className="download-page">
      <Head title="Download the App" />
      {/* ── Simple top nav ─────────────────────────────────────── */}
      <header className="download-header">
        <Link href="/" className="download-logo">
          <img src="/app-icon.png" alt="Giwu Bible" className="download-logo-img" />
          <span>Giwu Bible</span>
        </Link>
        <Link href="/read" className="download-nav-link">Read online →</Link>
      </header>

      {/* ── Hero ───────────────────────────────────────────────── */}
      <section className="download-hero">
        <h1 className="download-hero-title">Download the App</h1>
        <p className="download-hero-sub">
          Read the Bible offline on your device — no internet required after setup.
        </p>
      </section>

      {/* ── Cards | release sidebar ────────────────────────────── */}
      <div className="download-layout">
        <section className="download-cards">
          {DOWNLOADS.map((d) => (
            <div key={d.id} className={`download-card${d.primary ? ' download-card--primary' : ''}`}>
              <div className="download-card-icon">{d.icon}</div>
              <div className="download-card-body">
                <div className="download-card-top">
                  <span className="download-card-label">{d.label}</span>
                  <span className="download-card-badge">{d.badge}</span>
                </div>
                <p className="download-card-desc">{d.description}</p>
              </div>
              <DownloadButton item={d} asset={release?.assets?.[d.id]} />
            </div>
          ))}

          {/* Web reader card */}
          <div className="download-card download-card--web">
            <div className="download-card-icon">
              <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
                <circle cx="12" cy="12" r="10"/>
                <path d="M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/>
              </svg>
            </div>
            <div className="download-card-body">
              <div className="download-card-top">
                <span className="download-card-label">Web</span>
                <span className="download-card-badge download-card-badge--free">FREE</span>
              </div>
              <p className="download-card-desc">Use the browser reader — no download needed.</p>
            </div>
            <Link href="/read" className="download-btn download-btn--secondary">
              Open reader
            </Link>
          </div>
        </section>

        <ReleasePanel release={release} repoUrl={repoUrl} />
      </div>

      {/* ── Footer note ────────────────────────────────────────── */}
      <p className="download-footer-note">
        On first launch the app prompts you to download Bible translations.
        An internet connection is only required for that initial step.
      </p>
    </div>
  )
}
