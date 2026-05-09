import { useChapter } from '../hooks/useChapter'
import ChapterNav from './ChapterNav'

export default function MainColumn({ bookName, book, chapter, onChapterChange, activeVerse, setActiveVerse }) {
  const { verses, loading, error } = useChapter(book, chapter)

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

        {!loading && !error && verses.map((verse) => (
          <p
            key={verse.v}
            className={`verse-item${activeVerse === verse.v ? ' active' : ''}`}
            onClick={() => setActiveVerse(verse.v === activeVerse ? null : verse.v)}
          >
            <span className="verse-num">{verse.v}</span>
            {verse.t}
          </p>
        ))}
      </div>
    </div>
  )
}
