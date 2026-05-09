import { useChapter } from '../hooks/useChapter'

export default function MainColumn({ book, chapter, activeVerse, setActiveVerse }) {
  const { verses, loading, error } = useChapter(book, chapter)

  if (loading) return <div className="p-4 text-muted">Loading…</div>
  if (error) return <div className="p-4 text-danger">Failed to load chapter.</div>
  if (!verses.length) return <div className="p-4 text-muted">No verses found.</div>

  return (
    <div className="p-4">
      {verses.map((verse) => (
        <p
          key={verse.v}
          className={`mb-2 rounded px-2 py-1 ${activeVerse === verse.v ? 'bg-warning-subtle' : 'verse-row'}`}
          style={{ cursor: 'pointer' }}
          onClick={() => setActiveVerse(verse.v === activeVerse ? null : verse.v)}
        >
          <sup className="text-muted me-1">{verse.v}</sup>
          {verse.t}
        </p>
      ))}
    </div>
  )
}
