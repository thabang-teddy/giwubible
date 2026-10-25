import { useEffect, useRef } from 'react'
import { router } from '@inertiajs/react'
import { useAuth } from '../hooks/useAuth'
import { useDarkMode } from '../hooks/useDarkMode'

const FOCUSABLE = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'

/**
 * The reader's settings, in a panel that slides in from the right.
 *
 * Everything the navbar used to scatter across loose icons lives here in one
 * order: who you are, how it looks, what you are reading, and the way back to
 * Genesis 1. The panel owns the theme so no other component has to.
 */
export default function SettingsDrawer({
  isOpen,
  onClose,
  bibles = [],
  primaryBible,
  onPrimaryBibleChange,
  onReset,
}) {
  const { user, logout } = useAuth()
  const { dark, toggleDark } = useDarkMode()
  const panelRef = useRef(null)
  const closeRef = useRef(null)
  const openerRef = useRef(null)

  // Escape closes, as the gear menu it replaces did. Tab stays inside: the
  // panel claims to be modal, so focus must not wander onto the chapter
  // behind the scrim.
  useEffect(() => {
    if (!isOpen) return

    const onKeyDown = (e) => {
      if (e.key === 'Escape') { onClose(); return }
      if (e.key !== 'Tab') return

      const stops = panelRef.current?.querySelectorAll(FOCUSABLE)
      if (!stops?.length) return

      const first = stops[0]
      const last = stops[stops.length - 1]
      const active = document.activeElement

      if (!panelRef.current.contains(active)) {
        e.preventDefault()
        first.focus()
      } else if (e.shiftKey && active === first) {
        e.preventDefault()
        last.focus()
      } else if (!e.shiftKey && active === last) {
        e.preventDefault()
        first.focus()
      }
    }

    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [isOpen, onClose])

  // Focus moves into the panel on open and back to the gear on close.
  useEffect(() => {
    if (isOpen) {
      openerRef.current = document.activeElement
      closeRef.current?.focus()
    } else if (openerRef.current) {
      openerRef.current.focus?.()
      openerRef.current = null
    }
  }, [isOpen])

  const go = (href) => { onClose(); router.visit(href) }

  const handleReset = () => { onClose(); onReset() }

  const handleBibleChange = (table) => { onClose(); onPrimaryBibleChange(table) }

  return (
    <>
      <div
        className={`settings-scrim${isOpen ? ' visible' : ''}`}
        onClick={onClose}
        aria-hidden="true"
      />

      <aside
        ref={panelRef}
        className={`settings-drawer${isOpen ? ' open' : ''}`}
        role="dialog"
        aria-modal="true"
        aria-label="Settings"
        aria-hidden={!isOpen}
        inert={!isOpen}
      >
        <div className="settings-drawer-head">
          <span className="settings-drawer-title">Settings</span>
          <button
            ref={closeRef}
            className="settings-drawer-close"
            onClick={onClose}
            aria-label="Close settings"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>

        <div className="settings-drawer-body">
          {/* ── Account ──────────────────────────────────────── */}
          <section className="settings-group">
            <h3 className="settings-group-label">Account</h3>
            {user ? (
              <>
                <p className="settings-identity">
                  <span className="settings-identity-name">{user.name}</span>
                  <span className="settings-identity-email">{user.email}</span>
                </p>
                {!user.email_verified && (
                  <p className="settings-group-note settings-group-note--warn">
                    Confirm your email to save bookmarks.{' '}
                    <button
                      type="button"
                      className="settings-note-link"
                      onClick={() => go('/email/verify')}
                    >
                      Send the link
                    </button>
                  </p>
                )}
                <button className="settings-row" onClick={() => go('/profile')}>
                  <span className="settings-row-label">Profile</span>
                  <Chevron />
                </button>
                <button className="settings-row" onClick={() => go('/bookmarks')}>
                  <span className="settings-row-label">Bookmarks</span>
                  <Chevron />
                </button>
                <button
                  className="settings-row settings-row--quiet"
                  onClick={() => { onClose(); logout() }}
                >
                  <span className="settings-row-label">Sign out</span>
                </button>
              </>
            ) : (
              <>
                <p className="settings-group-note">
                  Reading needs no account. Sign in to keep your bookmarks across devices.
                </p>
                <button className="settings-btn settings-btn--primary" onClick={() => go('/login')}>
                  Sign in
                </button>
                <button
                  className="settings-btn"
                  onClick={() => go('/login?mode=register')}
                >
                  Sign up
                </button>
              </>
            )}
          </section>

          {/* ── Theme ────────────────────────────────────────── */}
          <section className="settings-group">
            <h3 className="settings-group-label">Theme</h3>
            <button
              className="settings-row"
              onClick={toggleDark}
              role="switch"
              aria-checked={dark}
            >
              <span className="settings-row-label">Dark mode</span>
              <span className={`settings-switch${dark ? ' on' : ''}`} aria-hidden="true">
                <span className="settings-switch-knob" />
              </span>
            </button>
          </section>

          {/* ── Main bible ───────────────────────────────────── */}
          <section className="settings-group">
            <h3 className="settings-group-label">Main bible</h3>
            {/* Plain buttons, not a listbox: a listbox promises one tab stop
                and arrow-key navigation, and these are ordinary tab stops. */}
            <div>
              {bibles.map((b) => {
                const selected = b.table === primaryBible
                return (
                  <button
                    key={b.table}
                    className={`settings-row settings-row--bible${selected ? ' selected' : ''}`}
                    aria-current={selected ? 'true' : undefined}
                    onClick={() => handleBibleChange(b.table)}
                  >
                    <span className="settings-bible-abbr">{b.abbreviation}</span>
                    <span className="settings-bible-name">{b.version}</span>
                    {selected && (
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" className="settings-bible-check">
                        <polyline points="20 6 9 17 4 12"/>
                      </svg>
                    )}
                  </button>
                )
              })}
            </div>
          </section>

          {/* ── Reset ────────────────────────────────────────── */}
          <section className="settings-group">
            <h3 className="settings-group-label">Reset</h3>
            <button className="settings-row" onClick={handleReset}>
              <span className="settings-row-label">Back to Genesis 1 (KJV)</span>
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" className="settings-row-icon">
                <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
                <path d="M3 3v5h5"/>
              </svg>
            </button>
            <p className="settings-group-note">
              Clears the verse you have open and starts the reader over.
            </p>
          </section>
        </div>
      </aside>
    </>
  )
}

function Chevron() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" className="settings-row-icon">
      <polyline points="9 18 15 12 9 6"/>
    </svg>
  )
}
