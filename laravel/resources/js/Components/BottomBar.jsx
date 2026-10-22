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
    </footer>
  )
}
