export default function BottomBar({ bookName, chapter, verse, versionName, onOpenPanel }) {
  const ref = verse
    ? `${bookName?.toUpperCase()} ${chapter}:${verse} • ${(versionName ?? '').toUpperCase()}`
    : ''

  return (
    <footer className="app-footer" style={{ position: 'relative' }}>
      <span
        className={`footer-verse-ref${verse ? ' footer-verse-ref--link' : ''}`}
        onClick={verse ? onOpenPanel : undefined}
        title={verse ? 'View parallel translations' : undefined}
      >
        {ref}
      </span>
      <span className="footer-tagline">Built for seekers of truth</span>
      <div className="footer-icons">
        <button
          className="footer-icon-btn footer-panel-btn"
          onClick={onOpenPanel}
          title="Parallel translations"
          aria-label="Open parallel translations"
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <rect x="3" y="3" width="7" height="18" rx="1"/>
            <rect x="14" y="3" width="7" height="18" rx="1"/>
          </svg>
        </button>
        <button className="footer-icon-btn" title="Bookmark" aria-label="Bookmark">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>
          </svg>
        </button>
        <button className="footer-icon-btn" title="Share" aria-label="Share">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/>
            <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/>
            <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
          </svg>
        </button>
      </div>
    </footer>
  )
}
