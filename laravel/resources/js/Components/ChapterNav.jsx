import { maxChaptersForBook } from '../data/chapterCounts'

export default function ChapterNav({ bookName, book, chapter, onChapterChange }) {
  const max = maxChaptersForBook(book)
  const options = Array.from({ length: max }, (_, i) => i + 1)

  return (
    <div className="chapter-nav-bar">
      <div className="chapter-nav-pill">
        <button
          className="chapter-nav-btn"
          onClick={() => onChapterChange(chapter - 1)}
          disabled={chapter <= 1}
          aria-label="Previous chapter"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>

        <span className="chapter-nav-label">
          {bookName}
          <select
            className="chapter-nav-select"
            value={chapter}
            onChange={(e) => onChapterChange(Number(e.target.value))}
            aria-label="Chapter"
          >
            {options.map((n) => (
              <option key={n} value={n}>{n}</option>
            ))}
          </select>
          <span className="chapter-nav-caret">▾</span>
        </span>

        <button
          className="chapter-nav-btn"
          onClick={() => onChapterChange(chapter + 1)}
          disabled={chapter >= max}
          aria-label="Next chapter"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </button>
      </div>
    </div>
  )
}
