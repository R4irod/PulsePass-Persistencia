import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Boton from '../../componentes/Boton'
import Tarjeta from '../../componentes/Tarjeta'
import Modal from '../../componentes/Modal'
import { TipoTicketSchema, type Evento } from '../../lib/esquemas'
import { etiquetaTipoTicket } from '../../lib/etiquetas'
import { RUTAS } from '../../lib/rutas'
import { useAuth } from '../auth/useAuth'
import { useComprarTicket } from './hooks'

const SeleccionSchema = z.object({ type: TipoTicketSchema })
type Seleccion = z.infer<typeof SeleccionSchema>

export default function PanelCompra({ evento }: { evento: Evento }) {
  const { sesion } = useAuth()
  const navegar = useNavigate()
  const compra = useComprarTicket()
  const [abierto, setAbierto] = useState(false)
  const { register, handleSubmit, getValues } = useForm<Seleccion>({
    resolver: zodResolver(SeleccionSchema),
    defaultValues: { type: 'GENERAL' },
  })

  if (sesion.estado === 'cargando') return null
  if (sesion.estado === 'autenticado' &&
    !sesion.usuario.roles.includes('USER')) return null

  const disponible = evento.status === 'PUBLISHED'

  function abrirCompra() {
    if (!disponible) return
    if (sesion.estado !== 'autenticado') {
      void navegar(RUTAS.login, { state: { desde: RUTAS.evento(evento.eventCode) } })
      return
    }
    compra.reset()
    setAbierto(true)
  }

  function confirmar() {
    if (sesion.estado !== 'autenticado' || compra.isPending) return
    compra.mutate({
      userEmail: sesion.usuario.email,
      eventCode: evento.eventCode,
      type: getValues('type'),
    }, {
      onSuccess: (ticket) => {
        setAbierto(false)
        void navegar(RUTAS.confirmacion(ticket.ticketCode))
      },
    })
  }

  const texto = sesion.estado === 'anonimo'
    ? 'Inicia sesión para comprar' : 'Comprar entrada'

  return (
    <Tarjeta className="mt-8 p-6 pb-24 md:pb-6">
      <h2 className="mb-4 text-2xl font-bold">Entradas</h2>
      <form onSubmit={handleSubmit(abrirCompra)}>
        <fieldset disabled={!disponible || compra.isPending}>
          <legend className="mb-3 font-semibold">Tipo de entrada</legend>
          <div className="flex flex-wrap gap-4">
            {TipoTicketSchema.options.map(tipo => (
              <label key={tipo} className="flex min-h-11 items-center gap-2">
                <input type="radio" value={tipo} {...register('type')} />
                {etiquetaTipoTicket[tipo]}
              </label>
            ))}
          </div>
        </fieldset>
        <p className="my-4 text-sm text-slate-600 dark:text-slate-300">
          El precio se mostrará al confirmar la compra. No se realizan pagos reales.
        </p>
        {!disponible && <p role="status" className="mb-4">
          Este evento no está disponible para comprar entradas.
        </p>}
        <div className="fixed inset-x-0 bottom-0 z-20 border-t bg-white p-4
          dark:bg-slate-900 md:static md:border-0 md:bg-transparent md:p-0">
          <Boton type="submit" disabled={!disponible || compra.isPending}>
            {texto}
          </Boton>
        </div>
      </form>
      <Modal
        abierto={abierto}
        titulo="Confirmar compra"
        ocupado={compra.isPending}
        onCerrar={() => setAbierto(false)}
      >
        <p className="font-semibold">{evento.name}</p>
        <p className="mt-2">Tipo: {etiquetaTipoTicket[getValues('type')]}</p>
        <p className="mt-2 break-all">
          Cuenta: {sesion.estado === 'autenticado' && sesion.usuario.email}
        </p>
        {compra.isError && <p role="alert" className="mt-4 text-red-600 dark:text-red-300">
          {compra.error.message}
        </p>}
        <div className="mt-6 flex flex-wrap gap-3">
          <Boton variante="secundario" disabled={compra.isPending}
            onClick={() => setAbierto(false)}>
            Volver
          </Boton>
          <Boton cargando={compra.isPending} onClick={confirmar}>Confirmar</Boton>
        </div>
      </Modal>
    </Tarjeta>
  )
}
