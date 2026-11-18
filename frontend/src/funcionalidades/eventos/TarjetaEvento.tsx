import { Link } from 'react-router'
import { CalendarDays, MapPin, ArrowUpRight } from 'lucide-react'
import type { EventoResumen } from '../../lib/esquemas'
import { RUTAS } from '../../lib/rutas'
import { fechaEvento } from '../../lib/formato'
import Badge from '../../componentes/Badge'
import PanelCategoria from './PanelCategoria'

interface TarjetaEventoProps {
  evento: EventoResumen
}
export default function TarjetaEvento({ evento }: TarjetaEventoProps) {
  return (
    <Link
      to={RUTAS.evento(evento.eventCode)}
      className="group overflow-hidden rounded-2xl border border-slate-200 bg-white
        shadow-sm transition-shadow hover:shadow-lg dark:border-slate-800
        dark:bg-slate-900"
    >
      <PanelCategoria categoria={evento.category} />
      <div className="p-5">
        <Badge tono={evento.status} />
        <h3
          className="mt-3 break-words text-lg font-semibold group-hover:text-marca-600
            dark:group-hover:text-marca-300"
        >
          {evento.name}
        </h3>
        <p className="mt-4 flex items-start gap-2 text-sm text-slate-600 dark:text-slate-300">
          <CalendarDays aria-hidden="true" className="size-4 shrink-0" />
          <time dateTime={evento.eventDate}>{fechaEvento(evento.eventDate)}</time>
        </p>
        <p className="mt-2 flex items-start gap-2 text-sm text-slate-600 dark:text-slate-300">
          <MapPin aria-hidden="true" className="size-4 shrink-0" />
          {evento.venueName || 'Sede por confirmar'}
        </p>
        <span
          className="mt-6 flex items-center gap-2 text-sm font-semibold text-marca-700
            dark:text-marca-300"
        >
          Ver evento
          <ArrowUpRight aria-hidden="true" className="size-4" />
        </span>
      </div>
    </Link>
  )
}
