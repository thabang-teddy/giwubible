import { useState, useEffect } from 'react'
import { getChapter } from '../api/chapter'

const PRIMARY_BIBLE = 't_kjv'

export function useChapter(book, chapter) {
  const [verses, setVerses] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!book || !chapter) return
    setLoading(true)
    setError(null)
    getChapter(PRIMARY_BIBLE, book, chapter)
      .then(setVerses)
      .catch(setError)
      .finally(() => setLoading(false))
  }, [book, chapter])

  return { verses, loading, error }
}
