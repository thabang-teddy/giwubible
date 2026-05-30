import { Link } from 'react-router-dom'

export default function HomePage() {
  return (
    <div className="home-page">
      {/* ── Nav ────────────────────────────────────────────────── */}
      <header className="home-nav">
        <div className="home-nav-logo">
          <img src="/app-icon.png" alt="Giwu Bible" className="home-nav-logo-img" />
          <span>Giwu Bible</span>
        </div>
        <Link to="/download" className="home-nav-download">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.3" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="7 10 12 15 17 10"/>
            <line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
          Download
        </Link>
      </header>

      {/* ── Hero ───────────────────────────────────────────────── */}
      <section className="home-hero">
        <img
          src="/app-icon.png"
          alt="Giwu Bible"
          className="home-hero-icon"
        />
        <h1 className="home-hero-title">Giwu Bible</h1>
        <p className="home-hero-sub">
          Read the King James Version side-by-side with parallel translations.
          Available on Web, Android, and Windows.
        </p>

        <div className="home-cta-row">
          <Link to="/read" className="home-btn home-btn--primary">
            Start Reading
          </Link>
          <Link to="/download" className="home-btn home-btn--secondary">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.3" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/>
              <line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            Get the App
          </Link>
        </div>
      </section>

      {/* ── Platform badges ─────────────────────────────────────── */}
      <div className="home-platforms">
        <span className="home-platform-badge">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
            <path d="M6 18c0 .55.45 1 1 1h1v3.5c0 .83.67 1.5 1.5 1.5s1.5-.67 1.5-1.5V19h2v3.5c0 .83.67 1.5 1.5 1.5s1.5-.67 1.5-1.5V19h1c.55 0 1-.45 1-1V8H6v10zM3.5 8C2.67 8 2 8.67 2 9.5v7c0 .83.67 1.5 1.5 1.5S5 17.33 5 16.5v-7C5 8.67 4.33 8 3.5 8zm17 0c-.83 0-1.5.67-1.5 1.5v7c0 .83.67 1.5 1.5 1.5s1.5-.67 1.5-1.5v-7c0-.83-.67-1.5-1.5-1.5zm-4.97-5.84l1.3-1.3c.2-.2.2-.51 0-.71-.2-.2-.51-.2-.71 0l-1.48 1.48C14.15 1.23 13.1 1 12 1c-1.1 0-2.15.23-3.12.63L7.4.15c-.2-.2-.51-.2-.71 0-.2.2-.2.51 0 .71l1.31 1.3C6.1 3.26 5 5.01 5 7h14c0-1.99-1.1-3.74-2.47-4.84zM10 5H9V4h1v1zm5 0h-1V4h1v1z"/>
          </svg>
          Android
        </span>
        <span className="home-platform-badge">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
            <path d="M0 3.449L9.75 2.1v9.451H0m10.949-9.602L24 0v11.4H10.949M0 12.6h9.75v9.451L0 20.699M10.949 12.6H24V24l-12.9-1.801"/>
          </svg>
          Windows
        </span>
        <span className="home-platform-badge">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="10"/>
            <path d="M2 12h20"/>
          </svg>
          Web
        </span>
      </div>
    </div>
  )
}
