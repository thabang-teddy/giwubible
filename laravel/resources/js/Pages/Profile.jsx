import { Head, Link, useForm } from '@inertiajs/react'
import { useAuth } from '../hooks/useAuth'

export default function Profile() {
  const { user, logout } = useAuth()

  const infoForm = useForm({ name: user?.name ?? '', email: user?.email ?? '' })
  const pwForm = useForm({ password: '', password_confirmation: '' })

  if (!user) {
    return (
      <div className="bookmarks-page">
        <Head title="Profile" />
        <header className="app-navbar">
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Link href="/" className="navbar-logo">
              <img src="/app-icon.png" alt="Giwu Bible" className="navbar-logo-img" />
              <span className="navbar-logo-text">Profile</span>
            </Link>
          </div>
        </header>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', paddingTop: 80, gap: 16 }}>
          <p style={{ color: 'var(--gray-400)' }}>Sign in to view your profile.</p>
          <Link href="/login" className="login-submit" style={{ display: 'inline-block' }}>Sign in</Link>
        </div>
      </div>
    )
  }

  const handleInfoSave = (e) => {
    e.preventDefault()
    infoForm.transform((d) => ({ name: d.name, email: d.email }))
    infoForm.put('/profile', { preserveScroll: true })
  }

  const handlePasswordSave = (e) => {
    e.preventDefault()
    pwForm.transform((d) => ({ password: d.password, password_confirmation: d.password_confirmation }))
    pwForm.put('/profile', {
      preserveScroll: true,
      onSuccess: () => pwForm.reset(),
    })
  }

  const infoError = infoForm.errors.name || infoForm.errors.email
  const pwError = pwForm.errors.password || pwForm.errors.password_confirmation

  return (
    <div className="bookmarks-page">
      <Head title="Profile" />
      {/* ── Navbar ─────────────────────────────────────────── */}
      <header className="app-navbar">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Link href="/read" className="navbar-icon-btn" title="Back to reading" aria-label="Back">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="15 18 9 12 15 6"/>
            </svg>
          </Link>
          <Link href="/" className="navbar-logo">
            <img src="/app-icon.png" alt="Giwu Bible" className="navbar-logo-img" />
            <span className="navbar-logo-text">Profile</span>
          </Link>
        </div>
        <div className="navbar-actions">
          <button className="navbar-icon-btn" onClick={logout} title="Sign out" aria-label="Sign out">
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
                value={infoForm.data.name}
                onChange={(e) => infoForm.setData('name', e.target.value)}
                required
                autoComplete="name"
              />
            </div>
            <div className="login-field">
              <label htmlFor="p-email">Email</label>
              <input
                id="p-email"
                type="email"
                value={infoForm.data.email}
                onChange={(e) => infoForm.setData('email', e.target.value)}
                required
                autoComplete="email"
              />
            </div>

            {infoError && <p className="login-error">{infoError}</p>}
            {infoForm.recentlySuccessful && <p className="profile-success">Profile updated.</p>}

            <button type="submit" className="login-submit" disabled={infoForm.processing}>
              {infoForm.processing ? 'Saving…' : 'Save changes'}
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
                value={pwForm.data.password}
                onChange={(e) => pwForm.setData('password', e.target.value)}
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
                value={pwForm.data.password_confirmation}
                onChange={(e) => pwForm.setData('password_confirmation', e.target.value)}
                required
                autoComplete="new-password"
              />
            </div>

            {pwError && <p className="login-error">{pwError}</p>}
            {pwForm.recentlySuccessful && <p className="profile-success">Password changed.</p>}

            <button type="submit" className="login-submit" disabled={pwForm.processing}>
              {pwForm.processing ? 'Saving…' : 'Update password'}
            </button>
          </form>
        </section>

        {/* ── Danger zone ─────────────────────────────────── */}
        <section className="profile-card profile-danger-card">
          <h2 className="profile-card-title">Session</h2>
          <p style={{ fontSize: 13, color: 'var(--gray-500)', marginBottom: 12 }}>
            Signing out ends your session on this device.
          </p>
          <button className="profile-signout-btn" onClick={logout}>
            Sign out
          </button>
        </section>

      </div>
    </div>
  )
}
