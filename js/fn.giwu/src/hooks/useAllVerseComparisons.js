import { useState, useEffect } from 'react'
import { getVerse } from '../api/verse'

const KJV = 't_kjv'

export function useAllVerseComparisons(bibles, book, chapter, activeVerse) {
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    const comparisons = bibles?.filter((b) => b.table !== KJV)
    if (!comparisons?.length || !book || !chapter || !activeVerse) {
      setResults([])
      return
    }
    setLoading(true)
    Promise.all(
      comparisons.map((b) =>
        getVerse(b.table, book, chapter, activeVerse).catch(() => null)
      )
    )
      .then((all) => setResults(all.filter(Boolean)))
      .finally(() => setLoading(false))
  }, [bibles, book, chapter, activeVerse])

  return { results, loading }
}
