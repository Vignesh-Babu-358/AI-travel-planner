import { NavLink } from 'react-router-dom'

const links = [
  { to: '/', label: 'Plan', end: true },
  { to: '/trips', label: 'Trips' },
  { to: '/save', label: 'Save' },
  { to: '/similar', label: 'Similar' },
]

export default function NavBar() {
  return (
    <header className="border-b border-slate-200 bg-white">
      <nav className="mx-auto flex max-w-5xl items-center gap-1 px-4 py-3">
        <span className="mr-4 text-lg font-bold text-slate-900">🏍 Moto Road-Trip Planner</span>
        {links.map((l) => (
          <NavLink
            key={l.to}
            to={l.to}
            end={l.end}
            className={({ isActive }) =>
              `rounded-md px-3 py-1.5 text-sm font-medium ${
                isActive
                  ? 'bg-slate-900 text-white'
                  : 'text-slate-600 hover:bg-slate-100'
              }`
            }
          >
            {l.label}
          </NavLink>
        ))}
      </nav>
    </header>
  )
}
