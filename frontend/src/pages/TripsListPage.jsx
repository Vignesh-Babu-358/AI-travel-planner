import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listTrips } from '../api/client.js'
import ErrorBanner from '../components/ErrorBanner.jsx'
import Spinner from '../components/Spinner.jsx'

export default function TripsListPage() {
  const [trips, setTrips] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    listTrips()
      .then(setTrips)
      .catch((e) => setError(e.message))
  }, [])

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Saved trips</h1>
      <ErrorBanner message={error} />
      {!trips && !error && <Spinner />}

      {trips && trips.length === 0 && (
        <p className="text-sm text-slate-500">
          No trips yet. Plan one or use Save.
        </p>
      )}

      {trips && trips.length > 0 && (
        <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-left text-slate-600">
              <tr>
                <th className="px-4 py-2">#</th>
                <th className="px-4 py-2">Destination</th>
                <th className="px-4 py-2">Origin</th>
                <th className="px-4 py-2">Dates</th>
                <th className="px-4 py-2">Interests</th>
                <th className="px-4 py-2"></th>
              </tr>
            </thead>
            <tbody>
              {trips.map((t) => (
                <tr key={t.id} className="border-t border-slate-100 hover:bg-slate-50">
                  <td className="px-4 py-2 font-mono text-slate-500">{t.id}</td>
                  <td className="px-4 py-2 font-medium">{t.destination}</td>
                  <td className="px-4 py-2 text-slate-600">{t.origin}</td>
                  <td className="px-4 py-2 text-slate-600">
                    {t.startDate || '—'} → {t.endDate || '—'}
                  </td>
                  <td className="px-4 py-2 text-slate-600">{t.interests || '—'}</td>
                  <td className="px-4 py-2">
                    <Link className="text-blue-600 underline" to={`/trips/${t.id}`}>
                      Open
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
