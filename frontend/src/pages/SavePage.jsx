import { useState } from 'react'
import { Link } from 'react-router-dom'
import { saveTrip } from '../api/client.js'
import Field from '../components/Field.jsx'
import ErrorBanner from '../components/ErrorBanner.jsx'

const EMPTY = {
  origin: '',
  destination: '',
  waypoints: '',
  startDate: '',
  endDate: '',
  motorcycleModel: '',
  ridingExperience: '',
  maxDailyDistanceKm: '',
  fuelRangeKm: '',
  routePreference: '',
  avoidHighways: false,
  avoidTolls: false,
  interests: '',
  budget: '',
  itinerary: '',
}

const EXPERIENCE = [
  { value: '', label: 'Any / unspecified' },
  { value: 'beginner', label: 'Beginner' },
  { value: 'intermediate', label: 'Intermediate' },
  { value: 'experienced', label: 'Experienced' },
]

function readEvent(e) {
  const { name, type, value, checked } = e.target
  if (type === 'checkbox') return [name, checked]
  if (type === 'number') return [name, value === '' ? '' : Number(value)]
  return [name, value]
}

export default function SavePage() {
  const [form, setForm] = useState(EMPTY)
  const [saved, setSaved] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const update = (e) => {
    const [name, val] = readEvent(e)
    setForm((f) => ({ ...f, [name]: val }))
  }

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setSaved(null)
    setBusy(true)
    try {
      const trip = await saveTrip(form)
      setSaved(trip)
      setForm(EMPTY)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Save a ride</h1>
        <p className="text-sm text-slate-500">
          Persists the ride and embeds it into PGVector so it becomes RAG context
          for future ride plans.
        </p>
      </div>

      {saved && (
        <div className="rounded-md border border-green-300 bg-green-50 px-4 py-3 text-sm text-green-800">
          Saved ride #{saved.id} to {saved.destination}.{' '}
          <Link className="underline" to={`/trips/${saved.id}`}>
            View it
          </Link>
          .
        </div>
      )}

      <form
        onSubmit={onSubmit}
        className="grid grid-cols-1 gap-4 rounded-lg border border-slate-200 bg-white p-5 sm:grid-cols-2"
      >
        <Field label="Ride start" name="origin" value={form.origin} onChange={update} required />
        <Field label="Destination / end" name="destination" value={form.destination} onChange={update} required />
        <div className="sm:col-span-2">
          <Field label="Waypoints (via)" name="waypoints" value={form.waypoints} onChange={update} />
        </div>
        <Field label="Start date" name="startDate" type="date" value={form.startDate} onChange={update} />
        <Field label="End date" name="endDate" type="date" value={form.endDate} onChange={update} />
        <Field label="Motorcycle" name="motorcycleModel" value={form.motorcycleModel} onChange={update} placeholder="BMW R 1250 GS" />
        <Field label="Riding experience" name="ridingExperience" value={form.ridingExperience} onChange={update} options={EXPERIENCE} />
        <Field label="Max daily distance (km)" name="maxDailyDistanceKm" type="number" value={form.maxDailyDistanceKm} onChange={update} />
        <Field label="Fuel / charge range (km)" name="fuelRangeKm" type="number" value={form.fuelRangeKm} onChange={update} />
        <div className="sm:col-span-2">
          <Field label="Route preference" name="routePreference" value={form.routePreference} onChange={update} placeholder="twisty mountain passes" />
        </div>
        <Field label="Avoid highways" name="avoidHighways" checkbox value={form.avoidHighways} onChange={update} />
        <Field label="Avoid tolls" name="avoidTolls" checkbox value={form.avoidTolls} onChange={update} />
        <Field label="Scenery & points of interest" name="interests" value={form.interests} onChange={update} />
        <Field label="Budget" name="budget" value={form.budget} onChange={update} />
        <div className="sm:col-span-2">
          <Field
            label="Itinerary (Markdown)"
            name="itinerary"
            value={form.itinerary}
            onChange={update}
            textarea
            required
            rows={12}
            placeholder="Day 1: ..."
          />
        </div>
        <div className="sm:col-span-2">
          <button
            type="submit"
            disabled={busy}
            className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-50"
          >
            {busy ? 'Saving…' : 'Save ride'}
          </button>
        </div>
      </form>

      <ErrorBanner message={error} />
    </div>
  )
}
