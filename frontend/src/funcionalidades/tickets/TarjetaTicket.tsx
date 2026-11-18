import { useState } from 'react'
import { Link } from 'react-router'
import type { Ticket } from '../../lib/esquemas'
import { etiquetaTipoTicket } from '../../lib/etiquetas'
import { fechaEvento } from '../../lib/formato'
import { RUTAS } from '../../lib/rutas'
import Tarjeta from '../../componentes/Tarjeta'
import Badge from '../../componentes/Badge'
import Boton from '../../componentes/Boton'
import Modal from '../../componentes/Modal'
import { useCancelarTicket } from './hooks'

const precioCOP = new Intl.NumberFormat('es-CO', {
  style: 'currency', currency: 'COP',
})

interface Props {
  ticket: Ticket
  detalle?: boolean
  permitirCancelar?: boolean
}

export default function TarjetaTicket({
  ticket, detalle = false, permitirCancelar = true,
}: Props) {
  const cancelar = useCancelarTicket()
  const [abierto, setAbierto] = useState(false)
  const [aviso, setAviso] = useState('')

  async function copiar() {
    try {
      await navigator.clipboard.writeText(ticket.ticketCode)
      setAviso('Código copiado.')
    } catch {
      setAviso('No se pudo copiar. Selecciona el código y cópialo manualmente.')
    }
  }

  return (
    <Tarjeta className="p-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-xl font-bold">{ticket.eventName}</h2>
        <Badge tono={ticket.status} />
      </div>
      <p className="my-4 break-all font-mono">{ticket.ticketCode}</p>
      <dl className="grid gap-4 sm:grid-cols-2">
        <div>
          <dt className="text-sm text-slate-500 dark:text-slate-400">Tipo</dt>
          <dd>{etiquetaTipoTicket[ticket.type]}</dd>
        </div>
        <div>
          <dt className="text-sm text-slate-500 dark:text-slate-400">Precio</dt>
          <dd>{precioCOP.format(ticket.price)}</dd>
        </div>
        <div>
          <dt className="text-sm text-slate-500 dark:text-slate-400">Fecha de compra</dt>
          <dd>{fechaEvento(ticket.purchaseDate)}</dd>
        </div>
        {detalle && <div>
          <dt className="text-sm text-slate-500 dark:text-slate-400">Titular</dt>
          <dd className="break-all">{ticket.userEmail}</dd>
        </div>}
      </dl>
      {detalle && ticket.status === 'PAID' && (
        <p className="mt-5">Presenta este código en el ingreso.</p>
      )}
      <div className="mt-6 flex flex-wrap items-center gap-4">
        {detalle
          ? <Boton variante="secundario" onClick={() => void copiar()}>Copiar código</Boton>
          : <Link to={RUTAS.ticket(ticket.ticketCode)} className="underline">
            Ver detalle
          </Link>}
        {permitirCancelar && ticket.status === 'PAID' && (
          <Boton variante="peligro" onClick={() => {
            cancelar.reset()
            setAbierto(true)
          }}>
            Cancelar ticket
          </Boton>
        )}
      </div>
      {aviso && <p role="status" className="mt-4">{aviso}</p>}
      <Modal
        abierto={abierto}
        titulo="¿Cancelar este ticket?"
        ocupado={cancelar.isPending}
        onCerrar={() => setAbierto(false)}
      >
        <p>{ticket.eventName}</p>
        <p className="my-3 break-all font-mono">{ticket.ticketCode}</p>
        <p>El ticket dejará de ser válido para ingresar.</p>
        {cancelar.isError && <p role="alert" className="mt-4 text-red-600 dark:text-red-300">
          {cancelar.error.message}
        </p>}
        <div className="mt-6 flex flex-wrap gap-3">
          <Boton variante="secundario" disabled={cancelar.isPending}
            onClick={() => setAbierto(false)}>
            Conservar ticket
          </Boton>
          <Boton variante="peligro" cargando={cancelar.isPending}
            onClick={() => {
              if (cancelar.isPending) return
              cancelar.mutate(ticket.ticketCode, {
                onSuccess: () => {
                  setAbierto(false)
                  setAviso('Ticket cancelado correctamente.')
                },
              })
            }}>
            Confirmar cancelación
          </Boton>
        </div>
      </Modal>
    </Tarjeta>
  )
}
