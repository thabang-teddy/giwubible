import { useState, useEffect } from 'react'
import { Head, router } from '@inertiajs/react'
import Navbar from '../Components/Navbar'
import Sidebar from '../Components/Sidebar'
import MainColumn from '../Components/MainColumn'
import VersePanel from '../Components/VersePanel'
import BottomBar from '../Components/BottomBar'
import { useAuth } from '../hooks/useAuth'
import { useBookmarks } from '../hooks/useBookmarks'

function ls(key, fallback) {
  try {
    const v = localStorage.getItem(key)
    return v !== null ? JSON.parse(v) : fallback
  } catch {
    return fallback
  }
}

// Deep-link support: /read?book=1&chapter=1&verse=3&bible=t_kjv
const params = typeof window !== 'undefined'
  ? new URLSearchParams(window.location.search)
  : new URLSearchParams()

export default function Read({ bibles = [], books = [] }) {
  const { user } = useAuth()
  const { bookmarks, isBookmarked, toggle: toggleBookmark } = useBookmarks()

  const [primaryBible, setPrimaryBible] = useState(() => params.get('bible') ?? ls('giwu_bible', 't_kjv'))
  const [book, setBook] = useState(() => {
    const b = params.get('book')
    return b ? Number(b) : ls('giwu_book', 1)
  })
  const [chapter, setChapter] = useState(() => {
    const c = params.get('chapter')
    return c ? Number(c) : ls('giwu_chapter', 1)
  })
  const [activeVerse, setActiveVerse] = useState(() => {
    const v = params.get('verse')
    return v ? Number(v) : null
  })
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [panelOpen, setPanelOpen] = useState(() => !!params.get('verse'))

  useEffect(() => { localStorage.setItem('giwu_bible', JSON.stringify(primaryBible)) }, [primaryBible])
  useEffect(() => { localStorage.setItem('giwu_book', JSON.stringify(book)) }, [book])
  useEffect(() => { localStorage.setItem('giwu_chapter', JSON.stringify(chapter)) }, [chapter])

  const handleReset = () => {
    setPrimaryBible('t_kjv')
    setBook(1)
    setChapter(1)
    setActiveVerse(null)
    setPanelOpen(false)
  }

  const handleBookChange = (b) => {
    setBook(b)
    setChapter(1)
    setActiveVerse(null)
    setSidebarOpen(false)
  }

  const handleChapterChange = (c) => { setChapter(c); setActiveVerse(null) }

  const handlePrimaryBibleChange = (table) => {
    setPrimaryBible(table)
    setBook(1)
    setChapter(1)
    setActiveVerse(null)
    setPanelOpen(false)
  }

  const handleVerseSelect = (v) => {
    setActiveVerse(v)
    if (v !== null) setPanelOpen(true)
  }

  const handleBookmarkToggle = (verse, text) => {
    if (!user) { router.visit('/login'); return }
    toggleBookmark(primaryBible, book, chapter, verse, text)
  }

  const closeAll = () => { setSidebarOpen(false); setPanelOpen(false) }

  const currentBook = books.find((b) => b.b === book)
  const currentVersion = bibles.find((b) => b.table === primaryBible)

  return (
    <div className="app">
      <Head title={currentBook ? `${currentBook.n} ${chapter}` : 'Read'} />
      <Navbar
        bibles={bibles}
        primaryBible={primaryBible}
        onPrimaryBibleChange={handlePrimaryBibleChange}
        onReset={handleReset}
        onMenuOpen={() => setSidebarOpen(true)}
        bookmarkCount={bookmarks.length}
      />

      <div
        className={`mobile-overlay${sidebarOpen || panelOpen ? ' visible' : ''}`}
        onClick={closeAll}
      />

      <div className="app-body">
        <Sidebar
          books={books}
          selectedBook={book}
          onBookChange={handleBookChange}
          isOpen={sidebarOpen}
          onClose={() => setSidebarOpen(false)}
        />

        <MainColumn
          primaryBible={primaryBible}
          bookName={currentBook?.n ?? ''}
          book={book}
          chapter={chapter}
          onChapterChange={handleChapterChange}
          activeVerse={activeVerse}
          setActiveVerse={handleVerseSelect}
          isBookmarked={isBookmarked}
          onBookmarkToggle={handleBookmarkToggle}
        />

        <VersePanel
          bibles={bibles}
          primaryBible={primaryBible}
          book={book}
          chapter={chapter}
          activeVerse={activeVerse}
          isOpen={panelOpen}
          onClose={() => setPanelOpen(false)}
        />
      </div>

      <BottomBar
        bookName={currentBook?.n}
        chapter={chapter}
        verse={activeVerse}
        versionName={currentVersion?.version}
        onOpenPanel={() => setPanelOpen(true)}
      />
    </div>
  )
}
