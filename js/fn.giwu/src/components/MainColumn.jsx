import { useChapter } from '../hooks/useChapter'
import ChapterNav from './ChapterNav'

export default function MainColumn({
  primaryBible,
  bookName,
  book,
  chapter,
  onChapterChange,
  activeVerse,
  setActiveVerse,
  isBookmarked,
  onBookmarkToggle,
}) {
  const { verses, loading, error } = useChapter(primaryBible, book, chapter)

  return (
    <div className="app-main">
      <ChapterNav
        bookName={bookName}
        book={book}
        chapter={chapter}
        onChapterChange={onChapterChange}
      />

      <div className="chapter-content">
        <h1 className="chapter-title">{bookName} {chapter}</h1>

        {loading && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} className="skeleton" style={{ height: 20, width: `${70 + (i % 3) * 10}%` }} />
            ))}
          </div>
        )}

        {error && (
          <p style={{ color: 'var(--gray-400)', textAlign: 'center', marginTop: 40 }}>
            Failed to load chapter.
          </p>
        )}

        {!loading && !error && verses.map((verse) => {
          const bookmarked = isBookmarked?.(primaryBible, book, chapter, verse.v) ?? false
          return (
            <div
              key={verse.v}
              className={`verse-row${activeVerse === verse.v ? ' active' : ''}`}
            >
              <p
                className="verse-item"
                onClick={() => setActiveVerse(verse.v === activeVerse ? null : verse.v)}
              >
                <span className="verse-num">{verse.v}</span>
                {verse.t}
              </p>
              {onBookmarkToggle && (
                <button
                  className={`verse-bookmark-btn${bookmarked ? ' bookmarked' : ''}`}
                  onClick={() => onBookmarkToggle(verse.v, verse.t)}
                  title={bookmarked ? 'Remove bookmark' : 'Bookmark this verse'}
                  aria-label={bookmarked ? 'Remove bookmark' : 'Bookmark this verse'}
                  aria-pressed={bookmarked}
                >
                  <svg width="12" height="12" viewBox="0 0 24 24" fill={bookmarked ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"/>
                  </svg>
                </button>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
