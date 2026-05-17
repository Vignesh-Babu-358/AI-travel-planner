import { useState } from 'react'
import { Link } from 'react-router-dom'
import { saveTrip } from '../api/client.js'
import Field from '../components/Field.jsx'
import ErrorBanner from '../components/ErrorBanner.jsx'

const EMPTY = {
  origin: '',
  destination: '',
  startDate: '',
  endDate: '',
  interests: '',
  budget: '',
  itinerary: '',
}

export default function SavePage() {
  const [form, setForm] = useState(EMPTY)
  const [saved, setSaved] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const update = (e) => setForm({ ...form, [e.target.name]: e.target.value })

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
        <h1 className="text-2xl font-bold">Save a trip</h1>
        <p className="text-sm text-slate-500">
          Persists the trip and embeds it into PGVector so it becomes RAG context
          for future plans.
        </p>
      </div>

      {saved && (
        <div className="rounded-md border border-green-300 bg-green-50 px-4 py-3 text-sm text-green-800">
          Saved trip #{saved.id} to {saved.destination}.{' '}
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
        <Field label="Origin" name="origin" value={form.origin} onChange={update} required />
        <Field label="Destination" name="destination" value={form.destination} onChange={update} required />
        <Field label="Start date" name="startDate" type="date" value={form.startDate} onChange={update} />
        <Field label="End date" name="endDate" type="date" value={form.endDate} onChange={update} />
        <Field label="Interests" name="interests" value={form.interests} onChange={update} />
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
            {busy ? 'Saving…' : 'Save trip'}
          </button>
        </div>
      </form>

      <ErrorBanner message={error} />
    </div>
  )
}
