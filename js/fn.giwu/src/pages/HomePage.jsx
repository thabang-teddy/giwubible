import { Link } from 'react-router-dom'

export default function HomePage() {
  return (
    <div style={{
      display: 'flex', flexDirection: 'column', alignItems: 'center',
      justifyContent: 'center', minHeight: '100vh', textAlign: 'center', padding: '0 24px',
      fontFamily: 'var(--font-sans)',
    }}>
      <img
        src="/app-icon.png"
        alt="Giwu Bible"
        style={{ width: 80, height: 80, marginBottom: 20, objectFit: 'contain', borderRadius: 16 }}
      />
      <h1 style={{ fontSize: 36, fontWeight: 700, margin: '0 0 10px', color: 'var(--gray-900)' }}>
        Giwu Bible
      </h1>
      <p style={{ fontSize: 16, color: 'var(--gray-500)', marginBottom: 32, maxWidth: 360 }}>
        Read the King James Version side-by-side with parallel translations.
      </p>
      <Link to="/read" style={{
        background: 'var(--primary)', color: '#fff', textDecoration: 'none',
        padding: '12px 36px', borderRadius: 8, fontWeight: 600, fontSize: 15,
        transition: 'background 0.15s',
      }}>
        Start Reading
      </Link>
    </div>
  )
}
