import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getTrip } from '../api/client.js'
import Markdown from '../components/Markdown.jsx'
import ErrorBanner from '../components/ErrorBanner.jsx'
import Spinner from '../components/Spinner.jsx'

export default function TripDetailPage() {
  const { id } = useParams()
  const [trip, setTrip] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    setTrip(null)
    setError('')
    getTrip(id)
      .then(setTrip)
      .catch((e) => setError(e.message))
  }, [id])

  return (
    <div className="space-y-4">
      <Link to="/trips" className="text-sm text-blue-600 underline">
        ← Back to trips
      </Link>
      <ErrorBanner message={error} />
      {!trip && !error && <Spinner />}

      {trip && (
        <div className="space-y-4">
          <div>
            <h1 className="text-2xl font-bold">
              {trip.destination}{' '}
              <span className="text-base font-normal text-slate-500">
                (from {trip.origin})
              </span>
            </h1>
            <p className="text-sm text-slate-500">
              Trip #{trip.id} · {trip.startDate || '—'} → {trip.endDate || '—'} ·{' '}
              {trip.interests || 'no interests'} · {trip.budget || 'no budget'}
            </p>
          </div>
          <section className="rounded-lg border border-slate-200 bg-white p-6">
            <Markdown>{trip.itinerary}</Markdown>
          </section>
        </div>
      )}
    </div>
  )
}
