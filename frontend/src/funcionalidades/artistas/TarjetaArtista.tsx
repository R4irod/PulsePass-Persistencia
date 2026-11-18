import { Link } from 'react-router'
import type { Artista } from '../../lib/esquemas'
import { RUTAS } from '../../lib/rutas'

interface TarjetaArtistaProps {
  artista: Artista
}
export default function TarjetaArtista({ artista }: TarjetaArtistaProps) {
  return (
    <Link
      to={RUTAS.artista(artista.id)}
      className="rounded-2xl border border-slate-200 bg-white p-6 text-center
        transition-shadow hover:shadow-lg dark:border-slate-800
        dark:bg-slate-900"
    >
      <span
        aria-hidden="true"
        className="mx-auto mb-4 flex size-20 items-center justify-center rounded-full
          bg-marca-100 text-2xl font-semibold text-marca-700
          dark:bg-marca-900 dark:text-marca-100"
      >
        {artista.stageName.split(/\s+/).slice(0, 2).map(parte => parte.charAt(0)).join('')}
      </span>
      <h3 className="break-words font-semibold">{artista.stageName}</h3>
      <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">
        {artista.genre || 'Artista invitado'}
      </p>
      {artista.country && <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{artista.country}</p>}

    </Link>
  )
}
