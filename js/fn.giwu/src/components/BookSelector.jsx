export default function BookSelector({ books, selectedBook, onBookChange }) {
  return (
    <div className="mb-3">
      <label className="form-label fw-semibold">Book</label>
      <select
        className="form-select form-select-sm"
        value={selectedBook}
        onChange={(e) => onBookChange(Number(e.target.value))}
      >
        {books.map((book) => (
          <option key={book.b} value={book.b}>
            {book.n}
          </option>
        ))}
      </select>
    </div>
  )
}
