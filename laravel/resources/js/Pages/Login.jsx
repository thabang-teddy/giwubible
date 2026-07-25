import { useState } from 'react'
import { Head, Link, useForm } from '@inertiajs/react'

export default function Login() {
  const [mode, setMode] = useState('login')
  const form = useForm({ name: '', email: '', password: '', password_confirmation: '' })
  const { data, setData, processing, errors, reset, clearErrors } = form

  const switchMode = (next) => { setMode(next); clearErrors() }

  const handleSubmit = (e) => {
    e.preventDefault()
    if (mode === 'login') {
      form.transform((d) => ({ email: d.email, password: d.password }))
      form.post('/login', { onFinish: () => reset('password') })
    } else {
      form.transform((d) => ({
        name: d.name,
        email: d.email,
        password: d.password,
        password_confirmation: d.password,
      }))
      form.post('/register', { onFinish: () => reset('password') })
    }
  }

  const errorMsg = errors.email || errors.password || errors.name

  return (
    <div className="login-page">
      <Head title={mode === 'login' ? 'Sign in' : 'Create account'} />
      <div className="login-card">
        <Link href="/" className="login-back" aria-label="Go back">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
          Back
        </Link>

        <Link href="/" className="login-logo">
          <img src="/app-icon.png" alt="Giwu Bible" style={{ width: 32, height: 32 }} />
          <span>Giwu Bible</span>
        </Link>

        <div className="login-tabs">
          <button
            className={`login-tab${mode === 'login' ? ' active' : ''}`}
            onClick={() => switchMode('login')}
          >
            Sign in
          </button>
          <button
            className={`login-tab${mode === 'register' ? ' active' : ''}`}
            onClick={() => switchMode('register')}
          >
            Create account
          </button>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          {mode === 'register' && (
            <div className="login-field">
              <label htmlFor="name">Name</label>
              <input
                id="name"
                type="text"
                value={data.name}
                onChange={(e) => setData('name', e.target.value)}
                required
                autoComplete="name"
                placeholder="Your name"
              />
            </div>
          )}

          <div className="login-field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={data.email}
              onChange={(e) => setData('email', e.target.value)}
              required
              autoComplete="email"
              placeholder="you@example.com"
            />
          </div>

          <div className="login-field">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={data.password}
              onChange={(e) => setData('password', e.target.value)}
              required
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              placeholder={mode === 'register' ? 'At least 8 characters' : ''}
            />
          </div>

          {errorMsg && <p className="login-error">{errorMsg}</p>}

          <button type="submit" className="login-submit" disabled={processing}>
            {processing ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}
          </button>
        </form>
      </div>
    </div>
  )
}
