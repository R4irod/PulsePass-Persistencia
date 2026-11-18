import { useState } from 'react'
import { Link } from 'react-router'
import Contenedor from '../componentes/Contenedor'
import Boton from '../componentes/Boton'
import { Cargando, EstadoError, EstadoVacio } from '../componentes/EstadoConsulta'
import { useMisTickets } from '../funcionalidades/tickets/hooks'
import TarjetaTicket from '../funcionalidades/tickets/TarjetaTicket'
import { RUTAS } from '../lib/rutas'
import type { EstadoTicket } from '../lib/esquemas'

const filtros: { valor: EstadoTicket | ''; texto: string }[] = [
  { valor: '', texto: 'Todos' },
  { valor: 'PAID', texto: 'Pagados' },
  { valor: 'USED', texto: 'Usados' },
  { valor: 'CANCELLED', texto: 'Cancelados' },
  { valor: 'RESERVED', texto: 'Reservados' },
]

export default function MisTickets() {
  const consulta = useMisTickets()
  const [filtro, setFiltro] = useState<EstadoTicket | ''>('')
  const tickets = consulta.data?.filter(ticket => !filtro || ticket.status === filtro)

  return (
    <Contenedor>
      <h1 className="mb-6 text-3xl font-bold">Mis tickets</h1>
      <div className="mb-6 flex flex-wrap gap-2" aria-label="Filtrar por estado">
        {filtros.map(item => (
          <Boton key={item.valor} aria-pressed={filtro === item.valor}
            variante={filtro === item.valor ? 'primario' : 'secundario'}
            onClick={() => setFiltro(item.valor)}>
            {item.texto}
          </Boton>
        ))}
      </div>
      {consulta.isPending ? <Cargando /> : consulta.isError ? (
        <EstadoError error={consulta.error} reintentar={() => void consulta.refetch()} />
      ) : tickets?.length ? (
        <div className="grid gap-5">
          {tickets.map(ticket => <TarjetaTicket key={ticket.ticketCode} ticket={ticket} />)}
        </div>
      ) : (
        <EstadoVacio titulo={consulta.data.length
          ? 'No tienes tickets con este estado' : 'Aún no tienes tickets'}>
          <Link to={RUTAS.eventos} className="underline">Explorar la cartelera</Link>
        </EstadoVacio>
      )}
    </Contenedor>
  )
}
