import { Link } from 'react-router'
import { RUTAS } from '../lib/rutas'

export default function Footer() {
  return (
    <footer className="border-t border-slate-200 dark:border-slate-800">
      <div className="mx-auto flex max-w-6xl flex-col gap-2 px-4 py-6 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between dark:text-slate-400">
        <p>© 2026 PulsePass · Caso de estudio académico</p>
        <nav aria-label="Pie de página" className="flex gap-4">
          <Link to={RUTAS.sedes} className="hover:text-slate-900 dark:hover:text-white">
            Sedes
          </Link>
          <Link to={RUTAS.artistas} className="hover:text-slate-900 dark:hover:text-white">
            Artistas
          </Link>
        </nav>
      </div>
    </footer>
  )
}