import { useAllVerseComparisons } from '../hooks/useAllVerseComparisons'

function VersionCard({ result }) {
  return (
    <div className="version-card">
      <div className="version-card-header">
        <span className="version-card-name">{result.version}</span>
        <span className="version-card-badge">{result.abbreviation}</span>
      </div>
      {result.text
        ? <p className="version-card-text">{result.text}</p>
        : <span className="version-card-dots">• • •</span>
      }
    </div>
  )
}

export default function VersePanel({ bibles, book, chapter, activeVerse }) {
  const { results, loading } = useAllVerseComparisons(bibles, book, chapter, activeVerse)

  return (
    <aside className="app-panel">
      <div className="panel-tabs">
        <div className="panel-tab active">Parallel Verses</div>
        <div className="panel-tab">AI Insights</div>
      </div>

      <div className="panel-body">
        {!activeVerse ? (
          <p className="panel-empty">Click any verse to see parallel translations.</p>
        ) : (
          <>
            <p className="panel-section-title">Verse {activeVerse} Comparisons</p>

            {loading && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {Array.from({ length: 4 }).map((_, i) => (
                  <div key={i} className="version-card">
                    <div className="skeleton" style={{ height: 12, width: '60%', marginBottom: 10 }} />
                    <div className="skeleton" style={{ height: 14, width: '90%' }} />
                  </div>
                ))}
              </div>
            )}

            {!loading && results.map((r) => (
              <VersionCard key={r.bible} result={r} />
            ))}
          </>
        )}
      </div>
    </aside>
  )
}
