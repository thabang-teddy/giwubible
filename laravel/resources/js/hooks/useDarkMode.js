import { useState, useEffect } from 'react'

const STORAGE_KEY = 'giwu_dark'

/**
 * The reader's light/dark choice, applied to <html data-theme> and remembered
 * in localStorage.
 *
 * Only one component may own this at a time — each call keeps its own state,
 * so the settings drawer holds it for the reader rather than the navbar.
 */
export function useDarkMode() {
  const [dark, setDark] = useState(() => {
    try { return localStorage.getItem(STORAGE_KEY) === 'true' } catch { return false }
  })

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
    try { localStorage.setItem(STORAGE_KEY, dark) } catch {}
  }, [dark])

  return { dark, toggleDark: () => setDark((d) => !d) }
}
