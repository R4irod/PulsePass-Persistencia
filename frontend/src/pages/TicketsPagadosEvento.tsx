import { useNavigate, useParams } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Contenedor from '../componentes/Contenedor'
import Campo from '../componentes/Campo'
import Boton from '../componentes/Boton'
import Badge from '../componentes/Badge'
import { Cargando, EstadoError, EstadoVacio } from '../componentes/EstadoConsulta'
import { useTicketsPagados, useUsarTicket } from '../funcionalidades/tickets/hooks'
import { CodigoRutaSchema } from '../lib/catalogo'
import { etiquetaTipoTicket } from '../lib/etiquetas'
import { RUTAS } from '../lib/rutas'

const BusquedaSchema = z.object({ codigo: CodigoRutaSchema })
type Busqueda = z.infer<typeof BusquedaSchema>
const precioCOP = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP' })

export default function TicketsPagadosEvento() {
  const { eventCode } = useParams()
  const validado = CodigoRutaSchema.safeParse(eventCode)
  const codigo = validado.success ? validado.data : ''
  const navegar = useNavigate()
  const consulta = useTicketsPagados(codigo)
  const usar = useUsarTicket()
  const { register, handleSubmit, formState: { errors } } = useForm<Busqueda>({
    resolver: zodResolver(BusquedaSchema),
    defaultValues: { codigo },
  })

  function buscar(datos: Busqueda) {
    usar.reset()
    if (datos.codigo === codigo) void consulta.refetch()
    else void navegar(RUTAS.ticketsPagados(datos.codigo))
  }

  return (
    <Contenedor>
      <h1 className="mb-6 text-3xl font-bold">Tickets pagados por evento</h1>
      <form onSubmit={handleSubmit(buscar)} className="mb-6 flex flex-wrap items-end gap-3">
        <Campo etiqueta="Código del evento" className="min-w-0 flex-1"
          autoComplete="off" maxLength={100} disabled={usar.isPending}
          error={errors.codigo ? 'Escribe un código de evento válido.' : undefined}
          {...register('codigo')} />
        <Boton type="submit" disabled={usar.isPending}>Buscar</Boton>
      </form>
      {usar.isSuccess && <p role="status" className="mb-5 text-emerald-700 dark:text-emerald-300">
        Ingreso registrado: {usar.data.ticketCode}.
      </p>}
      {usar.isError && <p role="alert" className="mb-5 text-red-600 dark:text-red-300">
        {usar.error.message}
      </p>}
      {!codigo ? <p>Introduce el código del evento que deseas consultar.</p>
        : consulta.isPending ? <Cargando />
        : consulta.isError ? <EstadoError error={consulta.error}
          reintentar={() => void consulta.refetch()} noDisponible="Evento no disponible o sin permisos" />
        : !consulta.data.length ? <EstadoVacio titulo="No hay tickets pagados" />
        : <>
          <p className="mb-4 break-all">Evento: {codigo} · Total: {consulta.data.length}</p>
          <table className="block w-full md:table">
            <caption className="sr-only">Entradas pendientes de ingreso</caption>
            <thead className="hidden text-left md:table-header-group">
              <tr>
                {['Código', 'Tipo', 'Titular', 'Precio', 'Estado', 'Acción'].map(titulo => (
                  <th key={titulo} scope="col" className="p-3">{titulo}</th>
                ))}
              </tr>
            </thead>
            <tbody className="block md:table-row-group">
              {consulta.data.map(ticket => (
                <tr key={ticket.ticketCode}
                  className="mb-4 block rounded-xl border border-slate-200
                    bg-white p-3 dark:border-slate-700 dark:bg-slate-900 md:table-row">
                  <td className="block break-all p-3 font-mono md:table-cell">{ticket.ticketCode}</td>
                  <td className="block p-3 md:table-cell">{etiquetaTipoTicket[ticket.type]}</td>
                  <td className="block break-all p-3 md:table-cell">{ticket.userEmail}</td>
                  <td className="block p-3 md:table-cell">{precioCOP.format(ticket.price)}</td>
                  <td className="block p-3 md:table-cell"><Badge tono={ticket.status} /></td>
                  <td className="block p-3 md:table-cell">
                    <Boton disabled={usar.isPending || ticket.status !== 'PAID'}
                      cargando={usar.isPending && usar.variables === ticket.ticketCode}
                      onClick={() => {
                        if (!usar.isPending) usar.mutate(ticket.ticketCode)
                      }}>
                      Usar
                    </Boton>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </>}
    </Contenedor>
  )
}
