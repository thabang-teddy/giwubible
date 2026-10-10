import { useState, useEffect, useRef } from 'react'
import { Link, router } from '@inertiajs/react'
import { useAuth } from '../hooks/useAuth'

export default function Navbar({ bibles, primaryBible, onPrimaryBibleChange, onReset, onMenuOpen, bookmarkCount = 0 }) {
  const [versionOpen, setVersionOpen] = useState(false)
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [dark, setDark] = useState(() => {
    try { return localStorage.getItem('giwu_dark') === 'true' } catch { return false }
  })
  const versionRef = useRef(null)
  const settingsRef = useRef(null)
  const { user, logout } = useAuth()

  const current = bibles?.find((b) => b.table === primaryBible)

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
    try { localStorage.setItem('giwu_dark', dark) } catch {}
  }, [dark])

  useEffect(() => {
    if (!versionOpen && !settingsOpen) return
    const onPointerDown = (e) => {
      if (!versionRef.current?.contains(e.target)) setVersionOpen(false)
      if (!settingsRef.current?.contains(e.target)) setSettingsOpen(false)
    }
    const onKeyDown = (e) => {
      if (e.key !== 'Escape') return
      setVersionOpen(false)
      setSettingsOpen(false)
    }
    document.addEventListener('mousedown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('mousedown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [versionOpen, settingsOpen])

  const go = (href) => {
    setSettingsOpen(false)
    router.visit(href)
  }

  const handleLogout = () => {
    setSettingsOpen(false)
    logout()
  }

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
        <button
          className="navbar-icon-btn"
          onClick={onReset}
          title="Reset to Genesis 1 (KJV)"
          aria-label="Reset selection"
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
            <path d="M3 3v5h5"/>
          </svg>
        </button>

        <button
          className="navbar-icon-btn"
          onClick={() => setDark((d) => !d)}
          title={dark ? 'Switch to light mode' : 'Switch to dark mode'}
          aria-label="Toggle dark mode"
        >
          {dark ? (
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="4"/>
              <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"/>
            </svg>
          ) : (
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
            </svg>
          )}
        </button>

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

        {/* Version picker */}
        <div ref={versionRef} style={{ position: 'relative' }}>
          <button
            className="navbar-version-btn"
            onClick={() => setVersionOpen((o) => !o)}
            aria-haspopup="listbox"
            aria-expanded={versionOpen}
          >
            {current?.abbreviation ?? '…'}
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" style={{ marginLeft: 4 }}>
              <polyline points="6 9 12 15 18 9"/>
            </svg>
          </button>

          {versionOpen && (
            <div className="version-dropdown" role="listbox">
              {bibles?.map((b) => (
                <button
                  key={b.table}
                  className={`version-dropdown-item${b.table === primaryBible ? ' selected' : ''}`}
                  role="option"
                  aria-selected={b.table === primaryBible}
                  onClick={() => { onPrimaryBibleChange(b.table); setVersionOpen(false) }}
                >
                  <span className="version-dropdown-abbr">{b.abbreviation}</span>
                  <span className="version-dropdown-name">{b.version}</span>
                  {b.table === primaryBible && (
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" style={{ marginLeft: 'auto', flexShrink: 0, color: 'var(--primary)' }}>
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                  )}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Settings — the account links live in this list, not behind their own icon */}
        <div ref={settingsRef} style={{ position: 'relative' }}>
          <button
            className="navbar-icon-btn"
            onClick={() => setSettingsOpen((o) => !o)}
            title={user ? `Settings — signed in as ${user.name}` : 'Settings'}
            aria-label="Settings"
            aria-haspopup="menu"
            aria-expanded={settingsOpen}
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
              style={{ color: user ? 'var(--primary)' : 'currentColor' }}
            >
              <circle cx="12" cy="12" r="3"/>
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.6a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
            </svg>
          </button>

          {settingsOpen && (
            <div className="version-dropdown" role="menu" style={{ right: 0, left: 'auto', minWidth: 180 }}>
              {user ? (
                <>
                  <div className="settings-dropdown-identity">{user.email}</div>
                  <button className="version-dropdown-item" role="menuitem" onClick={() => go('/profile')}>
                    Profile
                  </button>
                  <button className="version-dropdown-item" role="menuitem" onClick={() => go('/bookmarks')}>
                    Bookmarks
                  </button>
                  <button className="version-dropdown-item" role="menuitem" onClick={handleLogout}>
                    Sign out
                  </button>
                </>
              ) : (
                <button className="version-dropdown-item" role="menuitem" onClick={() => go('/login')}>
                  Sign in
                </button>
              )}
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
