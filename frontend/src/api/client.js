// Thin fetch wrapper around the Travel Planner backend.
// All paths are relative to /api and proxied by Vite to http://localhost:8080.

const BASE = '/api'

async function request(path, { method = 'GET', body, params } = {}) {
  let url = BASE + path
  if (params) {
    const qs = new URLSearchParams(
      Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== ''),
    ).toString()
    if (qs) url += `?${qs}`
  }

  const res = await fetch(url, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  })

  const text = await res.text()
  const data = text ? JSON.parse(text) : null

  if (!res.ok) {
    // Backend returns ProblemDetail ({detail,title}) when enabled, or Spring's
    // default error body ({message,error}); cover both so the real cause
    // surfaces in the UI instead of just the status code.
    const message =
      (data && (data.detail || data.message || data.error || data.title)) ||
      `Request failed (${res.status})`
    throw new Error(message)
  }
  return data
}

// Strip empty optional fields so the backend uses its own defaults.
function compact(obj) {
  return Object.fromEntries(
    Object.entries(obj).filter(([, v]) => v !== undefined && v !== null && v !== ''),
  )
}

export const planTrip = (req) =>
  request('/trips/plan', { method: 'POST', body: compact(req) })

export const saveTrip = (req) =>
  request('/trips', { method: 'POST', body: compact(req) })

export const listTrips = () => request('/trips')

export const getTrip = (id) => request(`/trips/${id}`)

export const findSimilar = (query, k) =>
  request('/trips/similar', { params: { query, k } })
