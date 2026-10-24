import { useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'

/**
 * The chapter grid, mirroring the Android reader's chapter sheet.
 *
 * A native <select> made Psalms a 150-row scroll; a grid puts a whole book
 * within a glance and a single click.
 *
 * Rendered into <body> so the reader's own overflow and stacking contexts
 * cannot clip or bury it.
 */
export default function ChapterPicker({ max, current, onSelect, onClose }) {
  const activeRef = useRef(null)

  useEffect(() => {
    const onKey = (event) => {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [onClose])

  // Open on the chapter being read: in a long book it would otherwise be far
  // below the fold.
  useEffect(() => {
    activeRef.current?.scrollIntoView({ block: 'center' })
    activeRef.current?.focus()
  }, [])

  const chapters = Array.from({ length: max }, (_, i) => i + 1)

  return createPortal(
    <div className="chapter-picker-backdrop" onClick={onClose}>
      <div
        className="chapter-picker"
        role="dialog"
        aria-modal="true"
        aria-label="Select chapter"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="chapter-picker-header">
          <h2 className="chapter-picker-title">Select Chapter</h2>
          <button className="drawer-close-btn" onClick={onClose} aria-label="Close chapter list">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <div className="chapter-picker-grid">
          {chapters.map((n) => (
            <button
              key={n}
              ref={n === current ? activeRef : undefined}
              className={`chapter-tile${n === current ? ' active' : ''}`}
              onClick={() => onSelect(n)}
              aria-current={n === current ? 'true' : undefined}
            >
              {n}
            </button>
          ))}
        </div>
      </div>
    </div>,
    document.body,
  )
}
