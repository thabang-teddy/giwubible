import { useState, useEffect } from 'react'
import { getVerseComparisons } from '../api/verses'

/**
 * Fetches the active verse in every translation with a single AJAX call.
 * The caller filters the returned rows down to the versions it wants to show.
 */
export function useAllVerseComparisons(book, chapter, verse) {
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!book || !chapter || !verse) {
      setResults([])
      return
    }
    let cancelled = false
    setLoading(true)
    getVerseComparisons(book, chapter, verse)
      .then((data) => { if (!cancelled) setResults(data) })
      .catch(() => { if (!cancelled) setResults([]) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [book, chapter, verse])

  return { results, loading }
}
