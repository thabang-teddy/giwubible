import { useEffect, useRef, useState } from 'react'
import { useChapter } from '../hooks/useChapter'
import ChapterNav from './ChapterNav'
import VerseActions from './VerseActions'
import { shareVerse } from '../lib/verseShare'

export default function MainColumn({
  primaryBible,
  bookName,
  book,
  chapter,
  versionAbbreviation,
  onChapterChange,
  activeVerse,
  setActiveVerse,
  onCompare,
  isBookmarked,
  onBookmarkToggle,
}) {
  const { verses, loading, error } = useChapter(primaryBible, book, chapter)
  const [shareState, setShareState] = useState(null)

  // A shared link arrives with its verse already selected, so the reader has
  // to be taken to it. Armed once per chapter load: a verse the reader taps
  // themselves is on screen already and must not be scrolled out from under
  // their finger.
  const pendingScrollRef = useRef(true)
  useEffect(() => {
    if (loading || verses.length === 0 || !pendingScrollRef.current) return
    pendingScrollRef.current = false
    if (!activeVerse) return
    document
      .getElementById(`verse-${activeVerse}`)
      ?.scrollIntoView({ block: 'center', behavior: 'smooth' })
  }, [loading, verses, activeVerse])

  useEffect(() => {
    pendingScrollRef.current = true
  }, [primaryBible, book, chapter])

  // The outcome of a share is only worth showing for a moment, and only for
  // the verse it happened on.
  useEffect(() => {
    setShareState(null)
  }, [activeVerse])

  useEffect(() => {
    if (shareState === null) return
    const timer = setTimeout(() => setShareState(null), 2500)
    return () => clearTimeout(timer)
  }, [shareState])

  const handleShare = async (verse) => {
    const result = await shareVerse({
      bookName,
      bookNumber: book,
      chapter,
      verse: verse.v,
      abbreviation: versionAbbreviation,
      text: verse.t,
    })
    if (result !== 'shared' && result !== 'cancelled') setShareState(result)
  }

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
          const isActive = activeVerse === verse.v
          const bookmarked = isBookmarked?.(primaryBible, book, chapter, verse.v) ?? false
          return (
            <div key={verse.v} id={`verse-${verse.v}`} className="verse-block">
              <div className={`verse-row${isActive ? ' active' : ''}`}>
                <p
                  className="verse-item"
                  onClick={() => setActiveVerse(isActive ? null : verse.v)}
                >
                  <span className="verse-num">{verse.v}</span>
                  {verse.t}
                </p>
              </div>

              {isActive && (
                <VerseActions
                  verse={verse.v}
                  shareState={shareState}
                  isBookmarked={bookmarked}
                  onShare={() => handleShare(verse)}
                  onCompare={onCompare ? () => onCompare(verse.v) : undefined}
                  onBookmarkToggle={
                    onBookmarkToggle ? () => onBookmarkToggle(verse.v, verse.t) : undefined
                  }
                />
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
