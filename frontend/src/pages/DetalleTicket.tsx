import { Link, useParams } from 'react-router'
import Contenedor from '../componentes/Contenedor'
import { Cargando, EstadoError } from '../componentes/EstadoConsulta'
import { CodigoTicketSchema } from '../funcionalidades/tickets/api'
import { useTicket } from '../funcionalidades/tickets/hooks'
import TarjetaTicket from '../funcionalidades/tickets/TarjetaTicket'
import { RUTAS } from '../lib/rutas'
import NoEncontrada from './NoEncontrada'

export default function DetalleTicket({ confirmacion = false }: { confirmacion?: boolean }) {
  const { ticketCode } = useParams()
  const codigo = CodigoTicketSchema.safeParse(ticketCode)
  const consulta = useTicket(codigo.success ? codigo.data : '')
  if (!codigo.success) return <NoEncontrada />

  return (
    <Contenedor estrecho>
      <Link to={RUTAS.misTickets} className="mb-6 inline-block underline">
        Volver a mis tickets
      </Link>
      {consulta.isPending ? <Cargando /> : consulta.isError ? (
        <EstadoError error={consulta.error}
          reintentar={() => void consulta.refetch()}
          noDisponible="Ticket no disponible" />
      ) : (
        <>
          <h1 className="mb-6 text-3xl font-bold">
            {confirmacion ? 'Compra registrada' : 'Detalle del ticket'}
          </h1>
          {confirmacion && consulta.data.status === 'PAID' && (
            <p role="status" className="mb-5 text-emerald-700 dark:text-emerald-300">
              ¡Compra realizada con éxito!
            </p>
          )}
          <TarjetaTicket ticket={consulta.data} detalle permitirCancelar={!confirmacion} />
          <Link to={RUTAS.eventos} className="mt-6 inline-block underline">
            Seguir explorando
          </Link>
        </>
      )}
    </Contenedor>
  )
}
