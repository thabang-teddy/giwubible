import { useState, useEffect, useCallback } from 'react'
import { listBookmarks, saveBookmark, deleteBookmark } from '../api/bookmarks'
import { useAuth } from './useAuth'

export function useBookmarks() {
  const { user } = useAuth()
  const [bookmarks, setBookmarks] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!user) { setBookmarks([]); return }
    setLoading(true)
    listBookmarks()
      .then(setBookmarks)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [user])

  const isBookmarked = useCallback(
    (bible, book, chapter, verse) =>
      bookmarks.some(
        (b) => b.bible === bible && b.book === book && b.chapter === chapter && b.verse === verse
      ),
    [bookmarks]
  )

  const getBookmark = useCallback(
    (bible, book, chapter, verse) =>
      bookmarks.find(
        (b) => b.bible === bible && b.book === book && b.chapter === chapter && b.verse === verse
      ),
    [bookmarks]
  )

  const toggle = useCallback(
    async (bible, book, chapter, verse, text) => {
      const existing = bookmarks.find(
        (b) => b.bible === bible && b.book === book && b.chapter === chapter && b.verse === verse
      )
      if (existing) {
        setBookmarks((prev) => prev.filter((b) => b.id !== existing.id))
        await deleteBookmark(existing.id)
      } else {
        const newBm = await saveBookmark(bible, book, chapter, verse, text)
        setBookmarks((prev) => [...prev, newBm])
      }
    },
    [bookmarks]
  )

  return { bookmarks, loading, isBookmarked, getBookmark, toggle }
}
