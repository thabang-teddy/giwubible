import { Link } from '@inertiajs/react'
import { useAuth } from '../hooks/useAuth'

/**
 * The reader's top bar.
 *
 * Reset, the theme switch and the version picker used to sit here as loose
 * icons; they live in the settings drawer now, so the bar carries only the
 * book list, the logo, bookmarks and the way into that drawer.
 */
export default function Navbar({ onMenuOpen, onSettingsOpen, bookmarkCount = 0 }) {
  const { user } = useAuth()

  return (
    <header className="app-navbar">
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <button
          className="navbar-hamburger"
          onClick={onMenuOpen}
          aria-label="Open book list"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
            <line x1="3" y1="6" x2="21" y2="6"/>
            <line x1="3" y1="12" x2="21" y2="12"/>
            <line x1="3" y1="18" x2="21" y2="18"/>
          </svg>
        </button>

        <Link href="/" className="navbar-logo">
          <img src="/app-icon.png" alt="Giwu Bible" className="navbar-logo-img" />
          <span className="navbar-logo-text">Giwu Bible</span>
        </Link>
      </div>

      <div className="navbar-actions">
        {/* Bookmarks — signed-in readers only */}
        {user && (
          <Link
            href="/bookmarks"
            className="navbar-icon-btn"
            title="Bookmarks"
            aria-label="Bookmarks"
            style={{ position: 'relative', textDecoration: 'none' }}
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"/>
            </svg>
            {bookmarkCount > 0 && (
              <span className="bookmark-badge">{bookmarkCount > 9 ? '9+' : bookmarkCount}</span>
            )}
          </Link>
        )}

        {/* Settings — opens the drawer that holds everything else */}
        <button
          className="navbar-icon-btn"
          onClick={onSettingsOpen}
          title={user ? `Settings — signed in as ${user.name}` : 'Settings'}
          aria-label="Settings"
          aria-haspopup="dialog"
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
            style={{ color: user ? 'var(--primary)' : 'currentColor' }}
          >
            <circle cx="12" cy="12" r="3"/>
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.6a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
          </svg>
        </button>
      </div>
    </header>
  )
}
