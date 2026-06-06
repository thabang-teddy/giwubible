import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { useBookmarks } from '../hooks/useBookmarks'

function BookNum({ b }) {
  const books = [
    'Gen','Exo','Lev','Num','Deu','Jos','Jdg','Rut','1Sa','2Sa','1Ki','2Ki',
    '1Ch','2Ch','Ezr','Neh','Est','Job','Psa','Pro','Ecc','SoS','Isa','Jer',
    'Lam','Eze','Dan','Hos','Joe','Amo','Oba','Jon','Mic','Nah','Hab','Zep',
    'Hag','Zec','Mal','Mat','Mar','Luk','Joh','Act','Rom','1Co','2Co','Gal',
    'Eph','Phi','Col','1Th','2Th','1Ti','2Ti','Tit','Phm','Heb','Jam','1Pe',
    '2Pe','1Jo','2Jo','3Jo','Jud','Rev',
  ]
  return books[b - 1] ?? `Book ${b}`
}

export default function BookmarksPage() {
  const { user, logout } = useAuth()
  const { bookmarks, toggle } = useBookmarks()
  const navigate = useNavigate()

  if (!user) {
    return (
      <div className="bookmarks-page">
        <header className="app-navbar">
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <button
              type="button"
              className="navbar-icon-btn"
              onClick={() => navigate(-1)}
              aria-label="Go back"
            >
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="15 18 9 12 15 6"/>
              </svg>
            </button>
            <Link to="/" className="navbar-logo">
              <img src="/app-icon.png" alt="Giwu Bible" className="navbar-logo-img" />
              <span className="navbar-logo-text">Bookmarks</span>
            </Link>
          </div>
        </header>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', paddingTop: 80, gap: 16 }}>
          <p style={{ color: 'var(--gray-400)' }}>Sign in to see your bookmarks.</p>
          <Link to="/login" className="login-submit" style={{ display: 'inline-block' }}>
            Sign in
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="bookmarks-page">
      <header className="app-navbar">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Link to="/read" className="navbar-icon-btn" title="Back to reading" aria-label="Back">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="15 18 9 12 15 6"/>
            </svg>
          </Link>
          <Link to="/" className="navbar-logo">
            <img src="/app-icon.png" alt="Giwu Bible" className="navbar-logo-img" />
            <span className="navbar-logo-text">Bookmarks</span>
          </Link>
        </div>
        <div className="navbar-actions">
          <span style={{ fontSize: 12, color: 'var(--gray-400)', marginRight: 8 }}>{user.name}</span>
          <button className="navbar-icon-btn" onClick={logout} title="Sign out">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
              <polyline points="16 17 21 12 16 7"/>
              <line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
          </button>
        </div>
      </header>

      <div className="bookmarks-list">
        {bookmarks.length === 0 ? (
          <div className="bookmarks-empty">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" style={{ color: 'var(--gray-400)', marginBottom: 12 }}>
              <path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"/>
            </svg>
            <p>No bookmarks yet.</p>
            <p style={{ fontSize: 13, marginTop: 4 }}>Tap any verse while reading to bookmark it.</p>
          </div>
        ) : (
          bookmarks.map((bm) => (
            <div key={bm.id} className="bookmark-item">
              <button
                className="bookmark-item-ref"
                onClick={() => navigate(`/read?book=${bm.book}&chapter=${bm.chapter}&verse=${bm.verse}&bible=${bm.bible}`)}
              >
                <span className="bookmark-item-location">
                  <BookNum b={bm.book} /> {bm.chapter}:{bm.verse}
                  <span className="bookmark-item-version">{bm.bible.replace('t_', '').toUpperCase()}</span>
                </span>
                <span className="bookmark-item-text">{bm.text}</span>
              </button>
              <button
                className="bookmark-item-delete"
                onClick={() => toggle(bm.bible, bm.book, bm.chapter, bm.verse, bm.text)}
                aria-label="Remove bookmark"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>
          ))
        )}
      </div>
    </div>
  )
}
