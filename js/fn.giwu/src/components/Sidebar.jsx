import { useState } from 'react'

export default function Sidebar({ books, selectedBook, onBookChange, isOpen, onClose }) {
  const [search, setSearch] = useState('')

  const filtered = books.filter((b) =>
    b.n.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <aside className={`app-sidebar${isOpen ? ' open' : ''}`}>
      <button className="drawer-close-btn" onClick={onClose} aria-label="Close book list">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
          <line x1="18" y1="6" x2="6" y2="18"/>
          <line x1="6" y1="6" x2="18" y2="18"/>
        </svg>
      </button>

      <div className="sidebar-header">Bible Books</div>

      <div className="sidebar-search">
        <input
          className="sidebar-search-input"
          type="text"
          placeholder="Search books..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <div className="sidebar-book-list">
        {filtered.map((book) => (
          <div
            key={book.b}
            className={`sidebar-book-item${selectedBook === book.b ? ' active' : ''}`}
            onClick={() => onBookChange(book.b)}
          >
            <span>{book.n}</span>
            <span className="sidebar-book-chevron">›</span>
          </div>
        ))}
      </div>
    </aside>
  )
}
