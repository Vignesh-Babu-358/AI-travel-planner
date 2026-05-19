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
              {trip.origin} → {trip.destination}
              {trip.waypoints ? (
                <span className="text-base font-normal text-slate-500">
                  {' '}via {trip.waypoints}
                </span>
              ) : null}
            </h1>
            <p className="text-sm text-slate-500">
              Ride #{trip.id} · {trip.startDate || '—'} → {trip.endDate || '—'}
              {trip.motorcycleModel ? ` · 🏍 ${trip.motorcycleModel}` : ''}
              {trip.routePreference ? ` · ${trip.routePreference}` : ''}
            </p>
            <p className="mt-1 text-xs text-slate-400">
              {trip.ridingExperience ? `${trip.ridingExperience} rider` : 'experience: any'}
              {trip.maxDailyDistanceKm ? ` · ≤ ${trip.maxDailyDistanceKm} km/day` : ''}
              {trip.fuelRangeKm ? ` · ${trip.fuelRangeKm} km range` : ''}
              {trip.avoidHighways ? ' · avoids highways' : ''}
              {trip.avoidTolls ? ' · avoids tolls' : ''}
              {trip.interests ? ` · ${trip.interests}` : ''}
              {trip.budget ? ` · ${trip.budget}` : ''}
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
