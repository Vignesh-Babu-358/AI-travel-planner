import { Routes, Route } from 'react-router-dom'
import NavBar from './components/NavBar.jsx'
import PlanPage from './pages/PlanPage.jsx'
import SavePage from './pages/SavePage.jsx'
import TripsListPage from './pages/TripsListPage.jsx'
import TripDetailPage from './pages/TripDetailPage.jsx'
import SimilarPage from './pages/SimilarPage.jsx'

function App() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <NavBar />
      <main className="mx-auto max-w-5xl px-4 py-6">
        <Routes>
          <Route path="/" element={<PlanPage />} />
          <Route path="/trips" element={<TripsListPage />} />
          <Route path="/trips/:id" element={<TripDetailPage />} />
          <Route path="/save" element={<SavePage />} />
          <Route path="/similar" element={<SimilarPage />} />
          <Route path="*" element={<p>Not found.</p>} />
        </Routes>
      </main>
    </div>
  )
}

export default App
