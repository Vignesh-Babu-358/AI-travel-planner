import { Link } from 'react-router-dom'

// Renders one RAG / similarity result (SimilarTripResponse).
export default function SimilarCard({ item }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold text-slate-900">
          {item.destination || 'Unknown destination'}
        </h3>
        <span className="rounded bg-slate-100 px-2 py-0.5 text-xs font-mono text-slate-600">
          score {Number(item.score).toFixed(3)}
        </span>
      </div>
      {item.interests && (
        <p className="mt-1 text-sm text-slate-500">{item.interests}</p>
      )}
      <p className="mt-2 text-sm whitespace-pre-wrap text-slate-700">
        {item.snippet}
      </p>
      {item.tripId != null && (
        <Link
          to={`/trips/${item.tripId}`}
          className="mt-2 inline-block text-sm text-blue-600 underline"
        >
          View trip #{item.tripId}
        </Link>
      )}
    </div>
  )
}
