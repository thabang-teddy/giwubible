import { useState, useEffect } from 'react'
import { getBibles } from '../api/bibles'
import { getBooks } from '../api/books'

export function useBible() {
  const [bibles, setBibles] = useState([])
  const [books, setBooks] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    Promise.all([getBibles(), getBooks()])
      .then(([b, bk]) => {
        setBibles(b)
        setBooks(bk)
      })
      .catch(setError)
      .finally(() => setLoading(false))
  }, [])

  return { bibles, books, loading, error }
}
