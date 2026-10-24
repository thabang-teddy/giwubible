import { useState, useEffect } from 'react'
import { Head } from '@inertiajs/react'
import Navbar from '../Components/Navbar'
import SettingsDrawer from '../Components/SettingsDrawer'
import Sidebar from '../Components/Sidebar'
import MainColumn from '../Components/MainColumn'
import VersePanel from '../Components/VersePanel'
import BottomBar from '../Components/BottomBar'
import { useAuth } from '../hooks/useAuth'
import { useBookmarks } from '../hooks/useBookmarks'
import { bookSlug } from '../lib/verseShare'

// The translation a /read/{book}/{chapter} link falls back to, matching
// ReadController. Anything else has to travel in the URL, or a refresh would
// quietly move the reader to the King James.
const DEFAULT_BIBLE = 't_kjv'

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

// `initial` is the server's reading of a shared /read/{book}/{chapter}?v={verse}
// link. It outranks both the query string and the reader's stored position.
export default function Read({ bibles = [], books = [], initial = null }) {
  const { user } = useAuth()
  const { bookmarks, isBookmarked, toggle: toggleBookmark } = useBookmarks()

  const [primaryBible, setPrimaryBible] = useState(
    () => initial?.bible ?? params.get('bible') ?? ls('giwu_bible', DEFAULT_BIBLE),
  )
  const [book, setBook] = useState(() => {
    const b = params.get('book')
    return initial?.book ?? (b ? Number(b) : ls('giwu_book', 1))
  })
  const [chapter, setChapter] = useState(() => {
    const c = params.get('chapter')
    return initial?.chapter ?? (c ? Number(c) : ls('giwu_chapter', 1))
  })
  const [activeVerse, setActiveVerse] = useState(() => {
    const v = params.get('verse')
    return initial?.verse ?? (v ? Number(v) : null)
  })
  const [sidebarOpen, setSidebarOpen] = useState(false)
  // A shared link highlights its verse and shows the verse actions; opening
  // the comparison is the reader's own choice, not something arrival does for
  // them — on a phone the panel covers the verse they came to read.
  const [panelOpen, setPanelOpen] = useState(false)
  const [settingsOpen, setSettingsOpen] = useState(false)

  useEffect(() => { localStorage.setItem('giwu_bible', JSON.stringify(primaryBible)) }, [primaryBible])
  useEffect(() => { localStorage.setItem('giwu_book', JSON.stringify(book)) }, [book])
  useEffect(() => { localStorage.setItem('giwu_chapter', JSON.stringify(chapter)) }, [chapter])

  const handleReset = () => {
    setPrimaryBible(DEFAULT_BIBLE)
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
    if (v === null) setPanelOpen(false)
  }

  const handleCompare = (v) => {
    setActiveVerse(v)
    setPanelOpen(true)
  }

  const handleBookmarkToggle = (verse, text) =>
    toggleBookmark(primaryBible, book, chapter, verse, text)

  const closeAll = () => { setSidebarOpen(false); setPanelOpen(false) }

  const currentBook = books.find((b) => b.b === book)
  const currentVersion = bibles.find((b) => b.table === primaryBible)

  // Keep the address bar on the chapter being read, so it can be copied,
  // bookmarked or refreshed back to the same place.
  //
  // This rewrites the URL in place: no navigation, no Inertia visit, and so
  // no refetch of a chapter the reader already has. replaceState rather than
  // pushState, because an entry per chapter would make Back change the URL
  // while the reader carried on showing the chapter it left.
  useEffect(() => {
    if (!currentBook) return

    const slug = bookSlug(currentBook.n) || String(book)

    // The verse rides along so a refresh keeps the highlight, and so arriving
    // on a shared link leaves that link intact in the address bar. The
    // translation only appears when it is not the one a bare link assumes.
    const query = new URLSearchParams()
    if (activeVerse) query.set('v', String(activeVerse))
    if (primaryBible !== DEFAULT_BIBLE) query.set('bible', primaryBible)

    const search = query.toString()
    const url = `/read/${slug}/${chapter}${search ? `?${search}` : ''}`

    if (window.location.pathname + window.location.search === url) return

    // Inertia keeps its own page and scroll state on the entry; replacing the
    // URL must not throw that away.
    window.history.replaceState(window.history.state, '', url)
  }, [currentBook, book, chapter, activeVerse, primaryBible])

  return (
    <div className="app">
      <Head title={currentBook ? `${currentBook.n} ${chapter}` : 'Read'} />
      <Navbar
        onMenuOpen={() => setSidebarOpen(true)}
        onSettingsOpen={() => setSettingsOpen(true)}
        bookmarkCount={bookmarks.length}
      />

      <SettingsDrawer
        isOpen={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        bibles={bibles}
        primaryBible={primaryBible}
        onPrimaryBibleChange={handlePrimaryBibleChange}
        onReset={handleReset}
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
          versionAbbreviation={currentVersion?.abbreviation}
          onChapterChange={handleChapterChange}
          activeVerse={activeVerse}
          setActiveVerse={handleVerseSelect}
          onCompare={handleCompare}
          isBookmarked={user ? isBookmarked : undefined}
          onBookmarkToggle={user ? handleBookmarkToggle : undefined}
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
