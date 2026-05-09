import { useState } from 'react'
import Navbar from '../components/Navbar'
import Sidebar from '../components/Sidebar'
import MainColumn from '../components/MainColumn'
import VersePanel from '../components/VersePanel'
import BottomBar from '../components/BottomBar'
import { useBible } from '../hooks/useBible'
import { maxChaptersForBook } from '../data/chapterCounts'

export default function ReadPage() {
  const { bibles, books, loading } = useBible()

  const [book, setBook] = useState(1)
  const [chapter, setChapter] = useState(1)
  const [activeVerse, setActiveVerse] = useState(null)

  const handleBookChange = (b) => {
    setBook(b)
    setChapter((prev) => Math.min(prev, maxChaptersForBook(b)))
    setActiveVerse(null)
  }
  const handleChapterChange = (c) => { setChapter(c); setActiveVerse(null) }

  const currentBook = books.find((b) => b.b === book)

  return (
    <div className="app">
      <Navbar />

      <div className="app-body">
        {!loading && (
          <Sidebar
            books={books}
            selectedBook={book}
            onBookChange={handleBookChange}
          />
        )}

        <MainColumn
          bookName={currentBook?.n ?? ''}
          book={book}
          chapter={chapter}
          onChapterChange={handleChapterChange}
          activeVerse={activeVerse}
          setActiveVerse={setActiveVerse}
        />

        <VersePanel
          bibles={bibles}
          book={book}
          chapter={chapter}
          activeVerse={activeVerse}
        />
      </div>

      <BottomBar
        bookName={currentBook?.n}
        chapter={chapter}
        verse={activeVerse}
      />
    </div>
  )
}
