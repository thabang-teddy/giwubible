import BookSelector from './BookSelector'
import BibleSelector from './BibleSelector'
import { maxChaptersForBook } from '../data/chapterCounts'

function ChapterPicker({ book, chapter, onChapterChange }) {
  const max = maxChaptersForBook(book)
  const options = Array.from({ length: max }, (_, i) => i + 1)
  return (
    <div className="mb-3">
      <label className="form-label fw-semibold">Chapter</label>
      <select
        className="form-select form-select-sm"
        value={chapter}
        onChange={(e) => onChapterChange(Number(e.target.value))}
      >
        {options.map((n) => (
          <option key={n} value={n}>{n}</option>
        ))}
      </select>
    </div>
  )
}

function SidebarContent({ books, bibles, book, chapter, comparisonBible, setBook, setChapter, setComparisonBible }) {
  return (
    <div className="p-3">
      <h6 className="text-uppercase text-muted mb-3" style={{ letterSpacing: '0.08em' }}>Navigation</h6>
      <BookSelector books={books} selectedBook={book} onBookChange={setBook} />
      <ChapterPicker book={book} chapter={chapter} onChapterChange={setChapter} />
      <hr />
      <h6 className="text-uppercase text-muted mb-3" style={{ letterSpacing: '0.08em' }}>Comparison</h6>
      <BibleSelector bibles={bibles} selectedBible={comparisonBible} onBibleChange={setComparisonBible} />
    </div>
  )
}

export default function Sidebar(props) {
  return (
    <>
      {/* Desktop sidebar */}
      <div className="d-none d-lg-flex flex-column border-end bg-light" style={{ width: 220, minHeight: '100vh', flexShrink: 0 }}>
        <SidebarContent {...props} />
      </div>

      {/* Mobile offcanvas */}
      <div
        className="offcanvas offcanvas-start"
        tabIndex={-1}
        id="sidebarOffcanvas"
        aria-labelledby="sidebarOffcanvasLabel"
      >
        <div className="offcanvas-header">
          <h5 className="offcanvas-title" id="sidebarOffcanvasLabel">Navigation</h5>
          <button type="button" className="btn-close" data-bs-dismiss="offcanvas" aria-label="Close" />
        </div>
        <div className="offcanvas-body p-0">
          <SidebarContent {...props} />
        </div>
      </div>
    </>
  )
}
