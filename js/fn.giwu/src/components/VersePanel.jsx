import { useVerseComparison } from '../hooks/useVerseComparison'

export default function VersePanel({ comparisonBible, book, chapter, activeVerse }) {
  const { result, loading, error } = useVerseComparison(comparisonBible, book, chapter, activeVerse)

  if (!comparisonBible) {
    return (
      <div className="p-4 text-muted fst-italic">
        Select a comparison version in the sidebar.
      </div>
    )
  }

  if (!activeVerse) {
    return (
      <div className="p-4 text-muted fst-italic">
        Click a verse to compare.
      </div>
    )
  }

  if (loading) return <div className="p-4 text-muted">Loading…</div>
  if (error) return <div className="p-4 text-danger">Failed to load verse.</div>

  return (
    <div className="p-4">
      <div className="mb-2 text-muted small fw-semibold text-uppercase" style={{ letterSpacing: '0.06em' }}>
        {result?.abbreviation} — verse {activeVerse}
      </div>
      <p className="mb-0 fs-5">{result?.text}</p>
      <div className="mt-2 text-muted small">{result?.version}</div>
    </div>
  )
}
