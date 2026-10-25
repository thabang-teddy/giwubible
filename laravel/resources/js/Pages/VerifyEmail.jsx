import { Head, Link, useForm, usePage } from '@inertiajs/react'
import { useAuth } from '../hooks/useAuth'

/**
 * Where the `verified` middleware sends a reader who reaches for a bookmark
 * before confirming their address.
 *
 * Deliberately not a dead end: reading is the whole app and needs no account,
 * so "Keep reading" is as prominent as the resend, and the address itself can
 * still be corrected from the profile.
 */
export default function VerifyEmail({ email }) {
  const { flash } = usePage().props
  const { logout } = useAuth()
  const { post, processing } = useForm({})

  const resend = (e) => {
    e.preventDefault()
    post('/email/verification-notification', { preserveScroll: true })
  }

  return (
    <div className="login-page">
      <Head title="Confirm your email" />
      <div className="login-card">
        <Link href="/read" className="login-back" aria-label="Back to reading">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
          Back to reading
        </Link>

        <Link href="/" className="login-logo">
          <img src="/app-icon.png" alt="Giwu Bible" style={{ width: 32, height: 32 }} />
          <span>Giwu Bible</span>
        </Link>

        <h1 className="verify-heading">Confirm your email</h1>

        <p className="verify-body">
          We sent a link to <strong>{email}</strong>. Open it and your bookmarks
          will follow you to any device — the Android and desktop apps included.
        </p>
        <p className="verify-body verify-body--muted">
          Reading needs no account at all, so you can carry on without this.
        </p>

        {flash?.success && <p className="verify-flash">{flash.success}</p>}
        {flash?.error && <p className="login-error">{flash.error}</p>}

        <form onSubmit={resend}>
          <button type="submit" className="login-submit" disabled={processing} style={{ width: '100%' }}>
            {processing ? 'Sending…' : 'Send the link again'}
          </button>
        </form>

        <div className="verify-links">
          <Link href="/read">Keep reading</Link>
          <Link href="/profile">Use a different address</Link>
          <button type="button" onClick={logout}>Sign out</button>
        </div>
      </div>
    </div>
  )
}
