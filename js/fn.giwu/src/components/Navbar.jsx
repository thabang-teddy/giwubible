import { useState, useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'

export default function Navbar({ bibles, primaryBible, onPrimaryBibleChange }) {
  const [open, setOpen] = useState(false)
  const ref = useRef(null)

  const current = bibles?.find((b) => b.table === primaryBible)

  useEffect(() => {
    if (!open) return
    const handler = (e) => { if (!ref.current?.contains(e.target)) setOpen(false) }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [open])

  return (
    <header className="app-navbar">
      <Link to="/" className="navbar-logo">
        <span className="navbar-logo-icon">📖</span>
        Giwu Bible
      </Link>

      <div className="navbar-actions">
        <div ref={ref} style={{ position: 'relative' }}>
          <button
            className="navbar-version-btn"
            onClick={() => setOpen((o) => !o)}
            aria-haspopup="listbox"
            aria-expanded={open}
          >
            {current?.abbreviation ?? '…'}
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" style={{ marginLeft: 4 }}>
              <polyline points="6 9 12 15 18 9"/>
            </svg>
          </button>

          {open && (
            <div className="version-dropdown" role="listbox">
              {bibles?.map((b) => (
                <button
                  key={b.table}
                  className={`version-dropdown-item${b.table === primaryBible ? ' selected' : ''}`}
                  role="option"
                  aria-selected={b.table === primaryBible}
                  onClick={() => { onPrimaryBibleChange(b.table); setOpen(false) }}
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
      </div>
    </header>
  )
}
