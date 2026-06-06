import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Sidebar from '../components/Sidebar'
import MainColumn from '../components/MainColumn'
import VersePanel from '../components/VersePanel'
import BottomBar from '../components/BottomBar'
import { useBible } from '../hooks/useBible'
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

export default function ReadPage() {
  const { bibles, books, loading } = useBible()
  const { user } = useAuth()
  const { bookmarks, isBookmarked, toggle: toggleBookmark } = useBookmarks()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const [primaryBible, setPrimaryBible] = useState(() => searchParams.get('bible') ?? ls('giwu_bible', 't_kjv'))
  const [book, setBook] = useState(() => {
    const b = searchParams.get('book')
    return b ? Number(b) : ls('giwu_book', 1)
  })
  const [chapter, setChapter] = useState(() => {
    const c = searchParams.get('chapter')
    return c ? Number(c) : ls('giwu_chapter', 1)
  })
  const [activeVerse, setActiveVerse] = useState(() => {
    const v = searchParams.get('verse')
    return v ? Number(v) : null
  })
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [panelOpen, setPanelOpen] = useState(() => !!searchParams.get('verse'))

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

  const handleBookmarkToggle = async (verse, text) => {
    if (!user) { navigate('/login'); return }
    await toggleBookmark(primaryBible, book, chapter, verse, text)
  }

  const closeAll = () => { setSidebarOpen(false); setPanelOpen(false) }

  const currentBook = books.find((b) => b.b === book)
  const currentVersion = bibles.find((b) => b.table === primaryBible)

  return (
    <div className="app">
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
        {!loading && (
          <Sidebar
            books={books}
            selectedBook={book}
            onBookChange={handleBookChange}
            isOpen={sidebarOpen}
            onClose={() => setSidebarOpen(false)}
          />
        )}

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
