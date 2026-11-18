import { Link } from 'react-router'
import { MapPin, Users } from 'lucide-react'
import type { Sede } from '../../lib/esquemas'
import { RUTAS } from '../../lib/rutas'
import { numero } from '../../lib/formato'

interface TarjetaSedeProps {
  sede: Sede
}
export default function TarjetaSede({ sede }: TarjetaSedeProps) {
  return (
    <Link
      to={RUTAS.sede(sede.code)}
      className="rounded-2xl border border-slate-200 bg-white p-6 transition-shadow
        hover:shadow-lg dark:border-slate-800 dark:bg-slate-900"
    >
      <MapPin aria-hidden="true" className="mb-5 size-8 text-marca-500" />
      <h3 className="break-words text-lg font-semibold">{sede.name}</h3>
      <p className="mt-2 text-slate-600 dark:text-slate-300">{sede.city}</p>
      <p className="mt-3 flex items-center gap-2 text-sm">
        <Users aria-hidden="true" className="size-4" />Capacidad: {numero(sede.capacity)} personas
      </p>
      <p className="mt-4 text-sm font-medium text-emerald-700 dark:text-emerald-300">
        {sede.active ? 'Activa' : 'Inactiva'}
      </p>
    </Link>
  )
}
