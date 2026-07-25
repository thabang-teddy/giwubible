import { useCallback } from 'react'
import { usePage, router } from '@inertiajs/react'

/**
 * Bookmarks come from Inertia's shared props. Toggling issues an Inertia
 * visit (partial reload of just the bookmarks prop), so the reader's verse
 * state and the navbar badge stay in sync without a full page reload.
 */
export function useBookmarks() {
  const { bookmarks = [] } = usePage().props

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
    (bible, book, chapter, verse, text) => {
      const existing = bookmarks.find(
        (b) => b.bible === bible && b.book === book && b.chapter === chapter && b.verse === verse
      )
      const opts = { preserveScroll: true, preserveState: true, only: ['bookmarks', 'flash'] }
      if (existing) {
        router.delete(`/bookmarks/${existing.id}`, opts)
      } else {
        router.post('/bookmarks', { bible, book, chapter, verse, text }, opts)
      }
    },
    [bookmarks]
  )

  return { bookmarks, loading: false, isBookmarked, getBookmark, toggle }
}
