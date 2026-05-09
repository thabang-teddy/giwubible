export default function BibleSelector({ bibles, selectedBible, onBibleChange }) {
  return (
    <div className="mb-3">
      <label className="form-label fw-semibold">Compare with</label>
      <select
        className="form-select form-select-sm"
        value={selectedBible}
        onChange={(e) => onBibleChange(e.target.value)}
      >
        <option value="">— none —</option>
        {bibles.map((b) => (
          <option key={b.table} value={b.table}>
            {b.abbreviation} — {b.version}
          </option>
        ))}
      </select>
    </div>
  )
}
