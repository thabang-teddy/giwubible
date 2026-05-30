import { useState, useRef, useCallback } from 'react'
import { Link } from 'react-router-dom'

const SERVER_URL = import.meta.env.VITE_SERVER_URL || 'http://localhost:8000'

const DOWNLOADS = [
  {
    id: 'android',
    label: 'Android',
    badge: 'APK',
    description: 'For Android phones and tablets (Android 5.0+)',
    href: `${SERVER_URL}/api/downloads/giwu-bible-android.apk`,
    filename: 'giwu-bible-android.apk',
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
    href: `${SERVER_URL}/api/downloads/giwu-bible-windows-setup.exe`,
    filename: 'giwu-bible-windows-setup.exe',
    icon: (
      <svg width="36" height="36" viewBox="0 0 24 24" fill="currentColor">
        <path d="M0 3.449L9.75 2.1v9.451H0m10.949-9.602L24 0v11.4H10.949M0 12.6h9.75v9.451L0 20.699M10.949 12.6H24V24l-12.9-1.801"/>
      </svg>
    ),
    primary: false,
  },
]

const IDLE = { status: 'idle', pct: null, error: null }

function triggerSave(chunks, filename) {
  const blob = new Blob(chunks)
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

function DownloadButton({ item, state, onDownload }) {
  const busy = state.status === 'downloading'
  const done = state.status === 'done'
  const errored = state.status === 'error'
  const indeterminate = busy && state.pct === null

  const btnClass = [
    'download-btn',
    item.primary ? 'download-btn--primary' : 'download-btn--secondary',
    busy ? 'download-btn--busy' : '',
    done ? 'download-btn--done' : '',
    errored ? 'download-btn--error' : '',
  ].filter(Boolean).join(' ')

  return (
    <div className="download-btn-wrap">
      <button
        className={btnClass}
        disabled={busy}
        onClick={() => onDownload(item)}
        aria-label={`Download ${item.label}`}
      >
        {done && (
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="20 6 9 17 4 12"/>
          </svg>
        )}
        {errored && (
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
        )}
        {!busy && !done && !errored && (
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="7 10 12 15 17 10"/>
            <line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
        )}
        {done ? 'Saved!' : errored ? 'Failed' : busy && state.pct !== null ? `${state.pct}%` : 'Download'}
      </button>

      {busy && (
        <div className="download-progress" aria-label={`Downloading ${item.label}`}>
          <div
            className={`download-progress-bar${indeterminate ? ' download-progress-bar--indeterminate' : ''}`}
            style={indeterminate ? undefined : { width: `${state.pct}%` }}
          />
        </div>
      )}

      {errored && state.error && (
        <p className="download-error-msg">{state.error}</p>
      )}
    </div>
  )
}

export default function DownloadPage() {
  const [dlState, setDlState] = useState({})
  const inProgress = useRef(new Set())

  const getState = (id) => dlState[id] ?? IDLE

  const update = (id, patch) => {
    setDlState(prev => ({ ...prev, [id]: { ...(prev[id] ?? IDLE), ...patch } }))
  }

  const resetAfter = (id, delay = 3500) => {
    setTimeout(() => setDlState(prev => ({ ...prev, [id]: IDLE })), delay)
  }

  const handleDownload = useCallback(async (item) => {
    if (inProgress.current.has(item.id)) return
    inProgress.current.add(item.id)

    update(item.id, { status: 'downloading', pct: null, error: null })

    try {
      const response = await fetch(item.href)

      if (!response.ok) {
        throw new Error(`Server error (${response.status})`)
      }

      const contentLength = response.headers.get('content-length')
      const total = contentLength ? Number(contentLength) : 0

      const reader = response.body.getReader()
      const chunks = []
      let received = 0

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        chunks.push(value)
        received += value.length
        const pct = total ? Math.min(99, Math.round((received / total) * 100)) : null
        update(item.id, { pct })
      }

      triggerSave(chunks, item.filename)
      update(item.id, { status: 'done', pct: 100 })
      resetAfter(item.id)
    } catch (err) {
      const message = err instanceof TypeError
        ? 'Could not reach the server. Check your connection.'
        : err.message
      update(item.id, { status: 'error', error: message })
      resetAfter(item.id, 5000)
    } finally {
      inProgress.current.delete(item.id)
    }
  }, [])

  return (
    <div className="download-page">
      {/* ── Simple top nav ─────────────────────────────────────── */}
      <header className="download-header">
        <Link to="/" className="download-logo">
          <img src="/app-icon.png" alt="Giwu Bible" className="download-logo-img" />
          <span>Giwu Bible</span>
        </Link>
        <Link to="/read" className="download-nav-link">Read online →</Link>
      </header>

      {/* ── Hero ───────────────────────────────────────────────── */}
      <section className="download-hero">
        <h1 className="download-hero-title">Download the App</h1>
        <p className="download-hero-sub">
          Read the Bible offline on your device — no internet required after setup.
        </p>
      </section>

      {/* ── Cards ──────────────────────────────────────────────── */}
      <section className="download-cards">
        {DOWNLOADS.map((d) => {
          const state = getState(d.id)
          return (
            <div key={d.id} className={`download-card${d.primary ? ' download-card--primary' : ''}`}>
              <div className="download-card-icon">{d.icon}</div>
              <div className="download-card-body">
                <div className="download-card-top">
                  <span className="download-card-label">{d.label}</span>
                  <span className="download-card-badge">{d.badge}</span>
                </div>
                <p className="download-card-desc">{d.description}</p>
              </div>
              <DownloadButton item={d} state={state} onDownload={handleDownload} />
            </div>
          )
        })}

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
          <Link to="/read" className="download-btn download-btn--secondary">
            Open reader
          </Link>
        </div>
      </section>

      {/* ── Footer note ────────────────────────────────────────── */}
      <p className="download-footer-note">
        On first launch the app prompts you to download Bible translations.
        An internet connection is only required for that initial step.
      </p>
    </div>
  )
}
