// Ground-truth route returned by the backend's routing API (OpenRouteService).
// Shown ABOVE the LLM narrative so the real distances/towns are visually
// separated from the model's commentary.
export default function RouteTable({ route }) {
  if (!route) return null
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-5">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">
          Route (from map data)
        </h2>
        <span className="text-xs text-slate-400">profile: {route.profile}</span>
      </div>
      <p className="mb-3 text-sm text-slate-600">
        Total <strong>{route.totalDistanceKm} km</strong> ·{' '}
        <strong>{route.totalDuration}</strong> riding
      </p>
      <div className="overflow-hidden rounded-md border border-slate-200">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-600">
            <tr>
              <th className="px-3 py-2">Day</th>
              <th className="px-3 py-2">From</th>
              <th className="px-3 py-2">To</th>
              <th className="px-3 py-2 text-right">km</th>
              <th className="px-3 py-2">Ride time</th>
            </tr>
          </thead>
          <tbody>
            {route.days?.map((d) => (
              <tr key={d.day} className="border-t border-slate-100">
                <td className="px-3 py-2 font-mono text-slate-500">{d.day}</td>
                <td className="px-3 py-2">{d.startName}</td>
                <td className="px-3 py-2">{d.endName}</td>
                <td className="px-3 py-2 text-right">{d.distanceKm}</td>
                <td className="px-3 py-2 text-slate-600">{d.duration}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
