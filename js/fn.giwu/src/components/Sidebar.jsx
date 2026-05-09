import { useState } from 'react'

export default function Sidebar({ books, selectedBook, onBookChange }) {
  const [search, setSearch] = useState('')

  const filtered = books.filter((b) =>
    b.n.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <aside className="app-sidebar">
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
