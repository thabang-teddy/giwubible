/**
 * Building and handing out the link to a single verse.
 *
 * Deliberately mirrors `kotlin/.../ui/reader/VerseShare.kt` — both clients hand
 * out `/read/{book-slug}/{chapter}?v={verse}`, which `ReadController::chapter`
 * resolves back to a book, chapter and highlighted verse.
 */

/** "1 John" -> "1-john", "Song of Solomon" -> "song-of-solomon". */
export function bookSlug(bookName) {
  return (bookName ?? '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
}

/**
 * The shareable link for a verse. Unlike the Android build this stays on the
 * host the reader is already on, so a link shared from a staging or local
 * reader opens there rather than on the live site.
 */
export function verseShareUrl({ origin, bookName, bookNumber, chapter, verse }) {
  const slug = bookSlug(bookName) || String(bookNumber)
  const query = verse > 0 ? `?v=${verse}` : ''

  return `${origin}/read/${slug}/${chapter}${query}`
}

/** "John 3:16 (KJV)" — the heading a share sheet shows. */
export function verseReference({ bookName, chapter, verse, abbreviation }) {
  const ref = [bookName, verse ? `${chapter}:${verse}` : `${chapter}`]
    .filter(Boolean)
    .join(' ')

  return abbreviation ? `${ref} (${abbreviation})` : ref
}

/** The verse text above its reference and link, as the Android build shares it. */
export function verseShareBody({ text, reference, url }) {
  return text ? `${text}\n\n— ${reference}\n${url}` : `${reference}\n${url}`
}

/**
 * The async clipboard needs a secure context and a focused document, neither
 * of which a reader on a plain http:// LAN address is guaranteed. The old
 * selection-based copy still works there, so it stands behind the modern call
 * rather than replacing it.
 */
function copyFallback(text) {
  const area = document.createElement('textarea')
  area.value = text
  area.setAttribute('readonly', '')
  area.style.position = 'fixed'
  area.style.top = '-1000px'
  area.style.opacity = '0'
  document.body.appendChild(area)

  try {
    area.select()
    area.setSelectionRange(0, text.length)
    return document.execCommand('copy')
  } catch {
    return false
  } finally {
    document.body.removeChild(area)
  }
}

/**
 * Hands the verse to the platform share sheet, falling back to the clipboard
 * where there is none (every desktop browser but Safari).
 *
 * Returns how it went so the caller can tell the reader: 'shared', 'copied',
 * 'cancelled' when they dismissed the sheet, or 'failed'.
 */
export async function shareVerse({ bookName, bookNumber, chapter, verse, abbreviation, text }) {
  const url = verseShareUrl({
    origin: window.location.origin,
    bookName,
    bookNumber,
    chapter,
    verse,
  })
  const reference = verseReference({ bookName, chapter, verse, abbreviation })
  const body = verseShareBody({ text, reference, url })

  if (navigator.share) {
    try {
      await navigator.share({ title: reference, text: body, url })
      return 'shared'
    } catch (error) {
      // A dismissed sheet is not a failure, and must not fall through to the
      // clipboard — that would copy a verse the reader decided not to share.
      if (error instanceof Error && error.name === 'AbortError') return 'cancelled'
    }
  }

  if (navigator.clipboard) {
    try {
      await navigator.clipboard.writeText(body)
      return 'copied'
    } catch {
      // Falls through to the selection-based copy below.
    }
  }

  return copyFallback(body) ? 'copied' : 'failed'
}
