import { Link } from 'react-router-dom'

export default function Navbar() {
  return (
    <header className="app-navbar">
      <Link to="/" className="navbar-logo">
        <span className="navbar-logo-icon">📖</span>
        Giwu Bible
      </Link>
      <div className="navbar-actions">
        <span className="navbar-version-badge">KJV</span>
      </div>
    </header>
  )
}
