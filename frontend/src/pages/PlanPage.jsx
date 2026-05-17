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
  startDate: '',
  endDate: '',
  interests: '',
  budget: '',
  notes: '',
}

export default function PlanPage() {
  const [form, setForm] = useState(EMPTY)
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const update = (e) => setForm({ ...form, [e.target.name]: e.target.value })

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
        <h1 className="text-2xl font-bold">Plan a trip</h1>
        <p className="text-sm text-slate-500">
          Generates an itinerary with OpenAI, grounded in similar past trips (RAG).
        </p>
      </div>

      <form
        onSubmit={onSubmit}
        className="grid grid-cols-1 gap-4 rounded-lg border border-slate-200 bg-white p-5 sm:grid-cols-2"
      >
        <Field label="Origin" name="origin" value={form.origin} onChange={update} required />
        <Field label="Destination" name="destination" value={form.destination} onChange={update} required />
        <Field label="Start date" name="startDate" type="date" value={form.startDate} onChange={update} />
        <Field label="End date" name="endDate" type="date" value={form.endDate} onChange={update} />
        <Field label="Interests" name="interests" value={form.interests} onChange={update} placeholder="temples, food, hiking" />
        <Field label="Budget" name="budget" value={form.budget} onChange={update} placeholder="moderate" />
        <div className="sm:col-span-2">
          <Field label="Notes" name="notes" value={form.notes} onChange={update} placeholder="traveling with kids, vegetarian…" />
        </div>
        <div className="sm:col-span-2">
          <button
            type="submit"
            disabled={loading}
            className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-50"
          >
            {loading ? 'Generating…' : 'Generate itinerary'}
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
                Itinerary — {result.destination}
              </h2>
              <button
                onClick={onSave}
                disabled={saving}
                className="rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium hover:bg-slate-50 disabled:opacity-50"
              >
                {saving ? 'Saving…' : 'Save this trip'}
              </button>
            </div>
            <p className="mb-4 text-xs text-slate-400">model: {result.model}</p>
            <Markdown>{result.itinerary}</Markdown>
          </section>

          <section>
            <h2 className="mb-3 text-lg font-semibold">
              RAG context — similar past trips ({result.usedContext?.length || 0})
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
                No similar past trips were retrieved for this request.
              </p>
            )}
          </section>
        </div>
      )}
    </div>
  )
}
