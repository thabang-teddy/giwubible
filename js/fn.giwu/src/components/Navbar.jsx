import { useState, useEffect, useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export default function Navbar({ bibles, primaryBible, onPrimaryBibleChange, onReset, onMenuOpen, bookmarkCount = 0 }) {
  const [versionOpen, setVersionOpen] = useState(false)
  const [userOpen, setUserOpen] = useState(false)
  const [dark, setDark] = useState(() => {
    try { return localStorage.getItem('giwu_dark') === 'true' } catch { return false }
  })
  const versionRef = useRef(null)
  const userRef = useRef(null)
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const current = bibles?.find((b) => b.table === primaryBible)

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
    try { localStorage.setItem('giwu_dark', dark) } catch {}
  }, [dark])

  useEffect(() => {
    if (!versionOpen && !userOpen) return
    const handler = (e) => {
      if (!versionRef.current?.contains(e.target)) setVersionOpen(false)
      if (!userRef.current?.contains(e.target)) setUserOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [versionOpen, userOpen])

  const handleLogout = async () => {
    setUserOpen(false)
    await logout()
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

        <Link to="/" className="navbar-logo">
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

        {/* Bookmarks */}
        <Link
          to="/bookmarks"
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

        {/* User menu */}
        <div ref={userRef} style={{ position: 'relative' }}>
          <button
            className="navbar-icon-btn"
            onClick={() => user ? setUserOpen((o) => !o) : navigate('/login')}
            title={user ? `Signed in as ${user.name}` : 'Sign in'}
            aria-label={user ? 'User menu' : 'Sign in'}
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"
              style={{ color: user ? 'var(--primary)' : 'currentColor' }}
            >
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
          </button>

          {userOpen && user && (
            <div className="version-dropdown" style={{ right: 0, left: 'auto', minWidth: 160 }}>
              <div style={{ padding: '8px 12px', fontSize: 12, color: 'var(--gray-400)', borderBottom: '1px solid var(--border)' }}>
                {user.email}
              </div>
              <button
                className="version-dropdown-item"
                onClick={() => { setUserOpen(false); navigate('/profile') }}
              >
                Profile
              </button>
              <button
                className="version-dropdown-item"
                onClick={() => { setUserOpen(false); navigate('/bookmarks') }}
              >
                Bookmarks
              </button>
              <button className="version-dropdown-item" onClick={handleLogout}>
                Sign out
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
