import { useState } from 'react'
import { maxChaptersForBook } from '../data/chapterCounts'
import ChapterPicker from './ChapterPicker'

export default function ChapterNav({ bookName, book, chapter, onChapterChange }) {
  const max = maxChaptersForBook(book)
  const [pickerOpen, setPickerOpen] = useState(false)

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

        <button
          className="chapter-nav-label"
          onClick={() => setPickerOpen(true)}
          aria-haspopup="dialog"
          aria-expanded={pickerOpen}
          aria-label="Choose a chapter"
        >
          {bookName} {chapter}
          <span className="chapter-nav-caret" aria-hidden="true">▾</span>
        </button>

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

      {pickerOpen && (
        <ChapterPicker
          max={max}
          current={chapter}
          onSelect={(n) => {
            setPickerOpen(false)
            onChapterChange(n)
          }}
          onClose={() => setPickerOpen(false)}
        />
      )}
    </div>
  )
}
