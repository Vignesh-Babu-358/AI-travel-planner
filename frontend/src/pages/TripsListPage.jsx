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
      <h1 className="text-2xl font-bold">Saved rides</h1>
      <ErrorBanner message={error} />
      {!trips && !error && <Spinner />}

      {trips && trips.length === 0 && (
        <p className="text-sm text-slate-500">
          No rides yet. Plan one or use Save.
        </p>
      )}

      {trips && trips.length > 0 && (
        <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-left text-slate-600">
              <tr>
                <th className="px-4 py-2">#</th>
                <th className="px-4 py-2">Route</th>
                <th className="px-4 py-2">Motorcycle</th>
                <th className="px-4 py-2">Route preference</th>
                <th className="px-4 py-2">Dates</th>
                <th className="px-4 py-2"></th>
              </tr>
            </thead>
            <tbody>
              {trips.map((t) => (
                <tr key={t.id} className="border-t border-slate-100 hover:bg-slate-50">
                  <td className="px-4 py-2 font-mono text-slate-500">{t.id}</td>
                  <td className="px-4 py-2 font-medium">
                    {t.origin} → {t.destination}
                  </td>
                  <td className="px-4 py-2 text-slate-600">{t.motorcycleModel || '—'}</td>
                  <td className="px-4 py-2 text-slate-600">{t.routePreference || '—'}</td>
                  <td className="px-4 py-2 text-slate-600">
                    {t.startDate || '—'} → {t.endDate || '—'}
                  </td>
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
