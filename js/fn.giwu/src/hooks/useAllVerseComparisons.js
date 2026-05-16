import { useState, useEffect } from 'react'
import { getVerse } from '../api/verse'

export function useAllVerseComparisons(selectedBibles, book, chapter, activeVerse) {
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!selectedBibles?.length || !book || !chapter || !activeVerse) {
      setResults([])
      return
    }
    setLoading(true)
    Promise.all(
      selectedBibles.map((b) =>
        getVerse(b.table, book, chapter, activeVerse).catch(() => null)
      )
    )
      .then((all) => setResults(all.filter(Boolean)))
      .finally(() => setLoading(false))
  }, [selectedBibles, book, chapter, activeVerse])

  return { results, loading }
}
