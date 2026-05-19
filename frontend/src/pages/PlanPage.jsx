import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { planTrip, saveTrip } from '../api/client.js'
import Field from '../components/Field.jsx'
import Markdown from '../components/Markdown.jsx'
import SimilarCard from '../components/SimilarCard.jsx'
import ErrorBanner from '../components/ErrorBanner.jsx'
import Spinner from '../components/Spinner.jsx'

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
  notes: '',
}

const EXPERIENCE = [
  { value: '', label: 'Any / unspecified' },
  { value: 'beginner', label: 'Beginner' },
  { value: 'intermediate', label: 'Intermediate' },
  { value: 'experienced', label: 'Experienced' },
]

// Generic change handler: checkboxes → boolean, number inputs → number.
function readEvent(e) {
  const { name, type, value, checked } = e.target
  if (type === 'checkbox') return [name, checked]
  if (type === 'number') return [name, value === '' ? '' : Number(value)]
  return [name, value]
}

export default function PlanPage() {
  const [form, setForm] = useState(EMPTY)
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const update = (e) => {
    const [name, val] = readEvent(e)
    setForm((f) => ({ ...f, [name]: val }))
  }

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setResult(null)
    setLoading(true)
    try {
      setResult(await planTrip(form))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  async function onSave() {
    setError('')
    setSaving(true)
    try {
      const trip = await saveTrip({ ...form, itinerary: result.itinerary })
      navigate(`/trips/${trip.id}`)
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Plan a ride</h1>
        <p className="text-sm text-slate-500">
          Generates a day-by-day motorcycle route with OpenAI, grounded in
          similar past rides (RAG).
        </p>
      </div>

      <form
        onSubmit={onSubmit}
        className="grid grid-cols-1 gap-4 rounded-lg border border-slate-200 bg-white p-5 sm:grid-cols-2"
      >
        <Field label="Ride start" name="origin" value={form.origin} onChange={update} required placeholder="Manali" />
        <Field label="Destination / end" name="destination" value={form.destination} onChange={update} required placeholder="Leh" />
        <div className="sm:col-span-2">
          <Field label="Waypoints (via)" name="waypoints" value={form.waypoints} onChange={update} placeholder="Jispa, Sarchu, Pang" />
        </div>
        <Field label="Start date" name="startDate" type="date" value={form.startDate} onChange={update} />
        <Field label="End date" name="endDate" type="date" value={form.endDate} onChange={update} />
        <Field label="Motorcycle" name="motorcycleModel" value={form.motorcycleModel} onChange={update} placeholder="Royal Enfield Himalayan 450" />
        <Field label="Riding experience" name="ridingExperience" value={form.ridingExperience} onChange={update} options={EXPERIENCE} />
        <Field label="Max daily distance (km)" name="maxDailyDistanceKm" type="number" value={form.maxDailyDistanceKm} onChange={update} placeholder="250" />
        <Field label="Fuel / charge range (km)" name="fuelRangeKm" type="number" value={form.fuelRangeKm} onChange={update} placeholder="250" />
        <div className="sm:col-span-2">
          <Field label="Route preference" name="routePreference" value={form.routePreference} onChange={update} placeholder="twisty mountain passes, scenic coastal…" />
        </div>
        <Field label="Avoid highways" name="avoidHighways" checkbox value={form.avoidHighways} onChange={update} />
        <Field label="Avoid tolls" name="avoidTolls" checkbox value={form.avoidTolls} onChange={update} />
        <Field label="Scenery & points of interest" name="interests" value={form.interests} onChange={update} placeholder="passes, viewpoints, lakes" />
        <Field label="Budget" name="budget" value={form.budget} onChange={update} placeholder="moderate" />
        <div className="sm:col-span-2">
          <Field label="Notes" name="notes" value={form.notes} onChange={update} placeholder="two-up, panniers, cold mornings…" />
        </div>
        <div className="sm:col-span-2">
          <button
            type="submit"
            disabled={loading}
            className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-50"
          >
            {loading ? 'Generating…' : 'Generate ride plan'}
          </button>
        </div>
      </form>

      <ErrorBanner message={error} />
      {loading && <Spinner label="Calling the model…" />}

      {result && (
        <div className="space-y-6">
          <section className="rounded-lg border border-slate-200 bg-white p-6">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-xl font-semibold">
                Ride plan — {result.destination}
              </h2>
              <button
                onClick={onSave}
                disabled={saving}
                className="rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium hover:bg-slate-50 disabled:opacity-50"
              >
                {saving ? 'Saving…' : 'Save this ride'}
              </button>
            </div>
            <p className="mb-4 text-xs text-slate-400">model: {result.model}</p>
            <Markdown>{result.itinerary}</Markdown>
          </section>

          <section>
            <h2 className="mb-3 text-lg font-semibold">
              RAG context — similar past rides ({result.usedContext?.length || 0})
            </h2>
            {result.usedContext?.length ? (
              <div className="grid gap-3 sm:grid-cols-2">
                {[...result.usedContext]
                  .sort((a, b) => b.score - a.score)
                  .map((c, i) => (
                    <SimilarCard key={c.tripId ?? i} item={c} />
                  ))}
              </div>
            ) : (
              <p className="text-sm text-slate-500">
                No similar past rides were retrieved for this request.
              </p>
            )}
          </section>
        </div>
      )}
    </div>
  )
}
