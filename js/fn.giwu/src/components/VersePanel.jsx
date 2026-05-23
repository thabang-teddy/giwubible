import { useState, useEffect, useMemo } from 'react'
import { useAllVerseComparisons } from '../hooks/useAllVerseComparisons'

const LS_KEY = 'giwu_parallel_bibles'

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

export default function VersePanel({ bibles, primaryBible, book, chapter, activeVerse, isOpen, onClose }) {
  const [activeTab, setActiveTab] = useState('verses')
  const [selectedTables, setSelectedTables] = useState(() => {
    try {
      const saved = localStorage.getItem(LS_KEY)
      return saved ? JSON.parse(saved) : []
    } catch {
      return []
    }
  })

  useEffect(() => {
    localStorage.setItem(LS_KEY, JSON.stringify(selectedTables))
  }, [selectedTables])

  const comparableBibles = useMemo(
    () => bibles.filter((b) => b.table !== primaryBible),
    [bibles, primaryBible]
  )

  const activeBibles = useMemo(
    () => selectedTables.length === 0
      ? comparableBibles
      : comparableBibles.filter((b) => selectedTables.includes(b.table)),
    [selectedTables, comparableBibles]
  )

  const { results, loading } = useAllVerseComparisons(activeBibles, book, chapter, activeVerse)

  const isChecked = (table) => selectedTables.length === 0 || selectedTables.includes(table)

  const toggleBible = (table) => {
    if (selectedTables.length === 0) {
      setSelectedTables(comparableBibles.map((b) => b.table).filter((t) => t !== table))
    } else if (selectedTables.includes(table)) {
      const next = selectedTables.filter((t) => t !== table)
      setSelectedTables(next.length === comparableBibles.length ? [] : next)
    } else {
      const next = [...selectedTables, table]
      setSelectedTables(next.length === comparableBibles.length ? [] : next)
    }
  }

  return (
    <aside className={`app-panel${isOpen ? ' open' : ''}`}>
      <div className="panel-drag-handle" />

      <div className="panel-tabs">
        <div
          className={`panel-tab${activeTab === 'verses' ? ' active' : ''}`}
          onClick={() => setActiveTab('verses')}
        >
          Parallel Verses
        </div>
        <div
          className={`panel-tab${activeTab === 'parallel-bibles' ? ' active' : ''}`}
          onClick={() => setActiveTab('parallel-bibles')}
        >
          Parallel Bibles
        </div>
        <div className="panel-tabs-close">
          <button className="drawer-close-btn" onClick={onClose} aria-label="Close panel">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
      </div>

      <div className="panel-body">
        {activeTab === 'parallel-bibles' ? (
          <>
            <p className="panel-section-title">Select bibles to compare</p>
            <div className="parallel-bibles-list">
              {comparableBibles.map((b) => (
                <label key={b.table} className="parallel-bible-item">
                  <input
                    type="checkbox"
                    checked={isChecked(b.table)}
                    onChange={() => toggleBible(b.table)}
                  />
                  <span className="parallel-bible-name">{b.version}</span>
                  <span className="version-card-badge">{b.abbreviation}</span>
                </label>
              ))}
            </div>
          </>
        ) : !activeVerse ? (
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
