import { useState } from 'react'
import { findSimilar } from '../api/client.js'
import SimilarCard from '../components/SimilarCard.jsx'
import ErrorBanner from '../components/ErrorBanner.jsx'
import Spinner from '../components/Spinner.jsx'

export default function SimilarPage() {
  const [query, setQuery] = useState('')
  const [k, setK] = useState(5)
  const [results, setResults] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setResults(null)
    setLoading(true)
    try {
      setResults(await findSimilar(query, k))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold">Similar trips</h1>
        <p className="text-sm text-slate-500">
          Semantic search over embedded past trips (PGVector).
        </p>
      </div>

      <form onSubmit={onSubmit} className="flex flex-wrap items-end gap-3">
        <label className="flex-1">
          <span className="mb-1 block text-sm font-medium text-slate-700">Query</span>
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            required
            placeholder="temples and gardens in Japan"
            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-slate-500 focus:ring-1 focus:ring-slate-500"
          />
        </label>
        <label>
          <span className="mb-1 block text-sm font-medium text-slate-700">k</span>
          <input
            type="number"
            min={1}
            max={20}
            value={k}
            onChange={(e) => setK(Number(e.target.value))}
            className="w-20 rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-slate-500"
          />
        </label>
        <button
          type="submit"
          disabled={loading}
          className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-50"
        >
          {loading ? 'Searching…' : 'Search'}
        </button>
      </form>

      <ErrorBanner message={error} />
      {loading && <Spinner />}

      {results && results.length === 0 && (
        <p className="text-sm text-slate-500">No matches above the similarity threshold.</p>
      )}
      {results && results.length > 0 && (
        <div className="grid gap-3 sm:grid-cols-2">
          {results.map((r, i) => (
            <SimilarCard key={r.tripId ?? i} item={r} />
          ))}
        </div>
      )}
    </div>
  )
}
