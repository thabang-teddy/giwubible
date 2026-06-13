import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

function extractMessage(err) {
  return (
    err?.response?.data?.message ||
    (err?.response?.data?.errors &&
      Object.values(err.response.data.errors).flat().join(' ')) ||
    'Something went wrong.'
  )
}

export default function ProfilePage() {
  const { user, logout, updateProfile } = useAuth()
  const navigate = useNavigate()

  // ── Info form ──────────────────────────────────────────────
  const [name, setName] = useState(user?.name ?? '')
  const [email, setEmail] = useState(user?.email ?? '')
  const [infoStatus, setInfoStatus] = useState(null)   // { ok, msg }
  const [infoSaving, setInfoSaving] = useState(false)

  // ── Password form ──────────────────────────────────────────
  const [password, setPassword] = useState('')
  const [passwordConfirmation, setPasswordConfirmation] = useState('')
  const [pwStatus, setPwStatus] = useState(null)
  const [pwSaving, setPwSaving] = useState(false)

  if (!user) {
    return (
      <div className="bookmarks-page">
        <header className="app-navbar">
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <button type="button" className="navbar-icon-btn" onClick={() => navigate(-1)} aria-label="Go back">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="15 18 9 12 15 6"/>
              </svg>
            </button>
            <Link to="/" className="navbar-logo">
              <img src="/app-icon.png" alt="Giwu Bible" className="navbar-logo-img" />
              <span className="navbar-logo-text">Profile</span>
            </Link>
          </div>
        </header>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', paddingTop: 80, gap: 16 }}>
          <p style={{ color: 'var(--gray-400)' }}>Sign in to view your profile.</p>
          <Link to="/login" className="login-submit" style={{ display: 'inline-block' }}>Sign in</Link>
        </div>
      </div>
    )
  }

  const handleInfoSave = async (e) => {
    e.preventDefault()
    setInfoStatus(null)
    setInfoSaving(true)
    try {
      await updateProfile({ name, email })
      setInfoStatus({ ok: true, msg: 'Profile updated.' })
    } catch (err) {
      setInfoStatus({ ok: false, msg: extractMessage(err) })
    } finally {
      setInfoSaving(false)
    }
  }

  const handlePasswordSave = async (e) => {
    e.preventDefault()
    setPwStatus(null)
    if (password !== passwordConfirmation) {
      setPwStatus({ ok: false, msg: 'Passwords do not match.' })
      return
    }
    setPwSaving(true)
    try {
      await updateProfile({ password, password_confirmation: passwordConfirmation })
      setPwStatus({ ok: true, msg: 'Password changed.' })
      setPassword('')
      setPasswordConfirmation('')
    } catch (err) {
      setPwStatus({ ok: false, msg: extractMessage(err) })
    } finally {
      setPwSaving(false)
    }
  }

  const handleLogout = async () => {
    await logout()
    navigate('/')
  }

  return (
    <div className="bookmarks-page">
      {/* ── Navbar ─────────────────────────────────────────── */}
      <header className="app-navbar">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Link to="/read" className="navbar-icon-btn" title="Back to reading" aria-label="Back">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="15 18 9 12 15 6"/>
            </svg>
          </Link>
          <Link to="/" className="navbar-logo">
            <img src="/app-icon.png" alt="Giwu Bible" className="navbar-logo-img" />
            <span className="navbar-logo-text">Profile</span>
          </Link>
        </div>
        <div className="navbar-actions">
          <button className="navbar-icon-btn" onClick={handleLogout} title="Sign out" aria-label="Sign out">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
              <polyline points="16 17 21 12 16 7"/>
              <line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
          </button>
        </div>
      </header>

      {/* ── Content ────────────────────────────────────────── */}
      <div className="profile-page">

        {/* Avatar / greeting */}
        <div className="profile-avatar-row">
          <div className="profile-avatar" aria-hidden="true">
            {user.name.charAt(0).toUpperCase()}
          </div>
          <div>
            <div className="profile-greeting">{user.name}</div>
            <div className="profile-email-sub">{user.email}</div>
          </div>
        </div>

        {/* ── Info card ───────────────────────────────────── */}
        <section className="profile-card">
          <h2 className="profile-card-title">Account info</h2>
          <form onSubmit={handleInfoSave} className="login-form" style={{ gap: 14 }}>
            <div className="login-field">
              <label htmlFor="p-name">Name</label>
              <input
                id="p-name"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                autoComplete="name"
              />
            </div>
            <div className="login-field">
              <label htmlFor="p-email">Email</label>
              <input
                id="p-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
              />
            </div>

            {infoStatus && (
              <p className={infoStatus.ok ? 'profile-success' : 'login-error'}>
                {infoStatus.msg}
              </p>
            )}

            <button type="submit" className="login-submit" disabled={infoSaving}>
              {infoSaving ? 'Saving…' : 'Save changes'}
            </button>
          </form>
        </section>

        {/* ── Password card ───────────────────────────────── */}
        <section className="profile-card">
          <h2 className="profile-card-title">Change password</h2>
          <form onSubmit={handlePasswordSave} className="login-form" style={{ gap: 14 }}>
            <div className="login-field">
              <label htmlFor="p-pw">New password</label>
              <input
                id="p-pw"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="new-password"
                placeholder="At least 8 characters"
              />
            </div>
            <div className="login-field">
              <label htmlFor="p-pw2">Confirm password</label>
              <input
                id="p-pw2"
                type="password"
                value={passwordConfirmation}
                onChange={(e) => setPasswordConfirmation(e.target.value)}
                required
                autoComplete="new-password"
              />
            </div>

            {pwStatus && (
              <p className={pwStatus.ok ? 'profile-success' : 'login-error'}>
                {pwStatus.msg}
              </p>
            )}

            <button type="submit" className="login-submit" disabled={pwSaving}>
              {pwSaving ? 'Saving…' : 'Update password'}
            </button>
          </form>
        </section>

        {/* ── Danger zone ─────────────────────────────────── */}
        <section className="profile-card profile-danger-card">
          <h2 className="profile-card-title">Session</h2>
          <p style={{ fontSize: 13, color: 'var(--gray-500)', marginBottom: 12 }}>
            Signing out removes your token from this device.
          </p>
          <button className="profile-signout-btn" onClick={handleLogout}>
            Sign out
          </button>
        </section>

      </div>
    </div>
  )
}
