import { useState } from 'react'
import Sidebar from '../components/Sidebar'
import MainColumn from '../components/MainColumn'
import VersePanel from '../components/VersePanel'
import { useBible } from '../hooks/useBible'
import { maxChaptersForBook } from '../data/chapterCounts'

export default function ReadPage() {
  const { bibles, books, loading } = useBible()

  const [book, setBook] = useState(1)
  const [chapter, setChapter] = useState(1)
  const [comparisonBible, setComparisonBible] = useState('')
  const [activeVerse, setActiveVerse] = useState(null)

  const handleBookChange = (b) => {
    setBook(b)
    setChapter((prev) => Math.min(prev, maxChaptersForBook(b)))
    setActiveVerse(null)
  }
  const handleChapterChange = (c) => { setChapter(c); setActiveVerse(null) }

  const currentBook = books.find((b) => b.b === book)

  return (
    <div className="d-flex">
      {!loading && (
        <Sidebar
          books={books}
          bibles={bibles}
          book={book}
          chapter={chapter}
          comparisonBible={comparisonBible}
          setBook={handleBookChange}
          setChapter={handleChapterChange}
          setComparisonBible={setComparisonBible}
        />
      )}

      <div className="flex-grow-1 d-flex flex-column" style={{ minHeight: '100vh' }}>
        {/* Top bar (mobile) */}
        <nav className="navbar d-lg-none border-bottom px-3">
          <button
            className="btn btn-sm btn-outline-secondary"
            type="button"
            data-bs-toggle="offcanvas"
            data-bs-target="#sidebarOffcanvas"
            aria-controls="sidebarOffcanvas"
          >
            ☰
          </button>
          <span className="ms-3 fw-semibold">
            {currentBook?.n} {chapter}
          </span>
        </nav>

        <div className="flex-grow-1 d-flex">
          {/* KJV Chapter */}
          <div className="flex-grow-1 border-end" style={{ maxWidth: comparisonBible ? '60%' : '100%' }}>
            <div className="px-4 pt-4 pb-2 border-bottom d-none d-lg-block">
              <h5 className="mb-0 fw-bold">
                {currentBook?.n} {chapter} <span className="text-muted fw-normal fs-6">— KJV</span>
              </h5>
            </div>
            <MainColumn
              book={book}
              chapter={chapter}
              activeVerse={activeVerse}
              setActiveVerse={setActiveVerse}
            />
          </div>

          {/* Comparison panel */}
          {comparisonBible && (
            <div style={{ width: '40%', flexShrink: 0 }}>
              <div className="px-4 pt-4 pb-2 border-bottom d-none d-lg-block">
                <h5 className="mb-0 fw-bold text-muted">Comparison</h5>
              </div>
              <VersePanel
                comparisonBible={comparisonBible}
                book={book}
                chapter={chapter}
                activeVerse={activeVerse}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
