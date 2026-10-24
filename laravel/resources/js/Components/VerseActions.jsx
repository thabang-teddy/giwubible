/**
 * The actions for the verse the reader tapped.
 *
 * A tap used to open the comparison panel outright; it now selects the verse
 * and reveals this row, so sharing, comparing and bookmarking all sit in one
 * place and none of them is the accidental result of reading.
 */

const ICON = {
  width: 13,
  height: 13,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 2.2,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
}

function ShareIcon() {
  return (
    <svg {...ICON}>
      <path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8" />
      <polyline points="16 6 12 2 8 6" />
      <line x1="12" y1="2" x2="12" y2="15" />
    </svg>
  )
}

function CompareIcon() {
  return (
    <svg {...ICON}>
      <rect x="3" y="3" width="18" height="18" rx="2" />
      <path d="M12 3v18" />
    </svg>
  )
}

function BookmarkIcon({ filled }) {
  return (
    <svg {...ICON} fill={filled ? 'currentColor' : 'none'}>
      <path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z" />
    </svg>
  )
}

export default function VerseActions({
  verse,
  shareState,
  isBookmarked,
  onShare,
  onCompare,
  onBookmarkToggle,
}) {
  const shareLabel = {
    copied: 'Link copied',
    failed: 'Could not share',
  }[shareState] ?? 'Share'

  return (
    <div className="verse-actions" role="group" aria-label={`Actions for verse ${verse}`}>
      <button className="verse-action" onClick={onShare} title="Share this verse">
        <ShareIcon />
        <span>{shareLabel}</span>
      </button>

      {onCompare && (
        <button
          className="verse-action verse-action--compare"
          onClick={onCompare}
          title="Compare translations"
        >
          <CompareIcon />
          <span>Compare</span>
        </button>
      )}

      {onBookmarkToggle && (
        <button
          className={`verse-action${isBookmarked ? ' verse-action--on' : ''}`}
          onClick={onBookmarkToggle}
          title={isBookmarked ? 'Remove bookmark' : 'Bookmark this verse'}
          aria-pressed={isBookmarked}
        >
          <BookmarkIcon filled={isBookmarked} />
          <span>{isBookmarked ? 'Bookmarked' : 'Bookmark'}</span>
        </button>
      )}
    </div>
  )
}
