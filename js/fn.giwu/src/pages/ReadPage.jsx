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

  const [primaryBible, setPrimaryBible] = useState('t_kjv')
  const [book, setBook] = useState(1)
  const [chapter, setChapter] = useState(1)
  const [activeVerse, setActiveVerse] = useState(null)

  const handleBookChange = (b) => {
    setBook(b)
    setChapter((prev) => Math.min(prev, maxChaptersForBook(b)))
    setActiveVerse(null)
  }
  const handleChapterChange = (c) => { setChapter(c); setActiveVerse(null) }
  const handlePrimaryBibleChange = (table) => { setPrimaryBible(table); setActiveVerse(null) }

  const currentBook = books.find((b) => b.b === book)
  const currentVersion = bibles.find((b) => b.table === primaryBible)

  return (
    <div className="app">
      <Navbar
        bibles={bibles}
        primaryBible={primaryBible}
        onPrimaryBibleChange={handlePrimaryBibleChange}
      />

      <div className="app-body">
        {!loading && (
          <Sidebar
            books={books}
            selectedBook={book}
            onBookChange={handleBookChange}
          />
        )}

        <MainColumn
          primaryBible={primaryBible}
          bookName={currentBook?.n ?? ''}
          book={book}
          chapter={chapter}
          onChapterChange={handleChapterChange}
          activeVerse={activeVerse}
          setActiveVerse={setActiveVerse}
        />

        <VersePanel
          bibles={bibles}
          primaryBible={primaryBible}
          book={book}
          chapter={chapter}
          activeVerse={activeVerse}
        />
      </div>

      <BottomBar
        bookName={currentBook?.n}
        chapter={chapter}
        verse={activeVerse}
        versionName={currentVersion?.version}
      />
    </div>
  )
}
