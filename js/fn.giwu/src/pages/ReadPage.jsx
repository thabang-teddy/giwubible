import { useState, useEffect } from 'react'
import Navbar from '../components/Navbar'
import Sidebar from '../components/Sidebar'
import MainColumn from '../components/MainColumn'
import VersePanel from '../components/VersePanel'
import BottomBar from '../components/BottomBar'
import { useBible } from '../hooks/useBible'
import { maxChaptersForBook } from '../data/chapterCounts'

function ls(key, fallback) {
  try {
    const v = localStorage.getItem(key)
    return v !== null ? JSON.parse(v) : fallback
  } catch {
    return fallback
  }
}

export default function ReadPage() {
  const { bibles, books, loading } = useBible()

  const [primaryBible, setPrimaryBible] = useState(() => ls('giwu_bible', 't_kjv'))
  const [book, setBook] = useState(() => ls('giwu_book', 1))
  const [chapter, setChapter] = useState(() => ls('giwu_chapter', 1))
  const [activeVerse, setActiveVerse] = useState(null)

  useEffect(() => { localStorage.setItem('giwu_bible', JSON.stringify(primaryBible)) }, [primaryBible])
  useEffect(() => { localStorage.setItem('giwu_book', JSON.stringify(book)) }, [book])
  useEffect(() => { localStorage.setItem('giwu_chapter', JSON.stringify(chapter)) }, [chapter])

  const handleReset = () => {
    setPrimaryBible('t_kjv')
    setBook(1)
    setChapter(1)
    setActiveVerse(null)
  }

  const handleBookChange = (b) => {
    setBook(b)
    setChapter(1)
    setActiveVerse(null)
  }
  const handleChapterChange = (c) => { setChapter(c); setActiveVerse(null) }
  const handlePrimaryBibleChange = (table) => {
    setPrimaryBible(table)
    setBook(1)
    setChapter(1)
    setActiveVerse(null)
  }

  const currentBook = books.find((b) => b.b === book)
  const currentVersion = bibles.find((b) => b.table === primaryBible)

  return (
    <div className="app">
      <Navbar
        bibles={bibles}
        primaryBible={primaryBible}
        onPrimaryBibleChange={handlePrimaryBibleChange}
        onReset={handleReset}
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
