import { useState, useEffect } from 'react'
import { getVerse } from '../api/verse'

export function useVerseComparison(comparisonBible, book, chapter, activeVerse) {
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!comparisonBible || !book || !chapter || !activeVerse) {
      setResult(null)
      return
    }
    setLoading(true)
    setError(null)
    getVerse(comparisonBible, book, chapter, activeVerse)
      .then(setResult)
      .catch(setError)
      .finally(() => setLoading(false))
  }, [comparisonBible, book, chapter, activeVerse])

  return { result, loading, error }
}
