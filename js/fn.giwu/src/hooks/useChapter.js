import { useState, useEffect } from 'react'
import { getChapter } from '../api/chapter'

export function useChapter(bible, book, chapter) {
  const [verses, setVerses] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!bible || !book || !chapter) return
    setLoading(true)
    setError(null)
    getChapter(bible, book, chapter)
      .then(setVerses)
      .catch(setError)
      .finally(() => setLoading(false))
  }, [bible, book, chapter])

  return { verses, loading, error }
}
