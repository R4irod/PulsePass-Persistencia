import type { EventoResumen } from '../../lib/esquemas'
import TarjetaEvento from './TarjetaEvento'

interface GrillaEventosProps {
  eventos: EventoResumen[]
}
export default function GrillaEventos({ eventos }: GrillaEventosProps) {
  return (
    <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
      {eventos.map(evento => (
        <TarjetaEvento key={evento.eventCode} evento={evento} />
      ))}
    </div>
  )
}
