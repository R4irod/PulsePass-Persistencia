import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Contenedor from '../componentes/Contenedor'
import Campo from '../componentes/Campo'
import Boton from '../componentes/Boton'
import { EstadoError } from '../componentes/EstadoConsulta'
import TarjetaTicket from '../funcionalidades/tickets/TarjetaTicket'
import { CodigoTicketSchema } from '../funcionalidades/tickets/api'
import { useTicket, useUsarTicket } from '../funcionalidades/tickets/hooks'

const BusquedaSchema = z.object({ codigo: CodigoTicketSchema })
type Busqueda = z.infer<typeof BusquedaSchema>

export default function ValidacionIngreso() {
  const [codigo, setCodigo] = useState('')
  const consulta = useTicket(codigo)
  const usar = useUsarTicket()
  const { register, handleSubmit, reset, setFocus, formState: { errors } } = useForm<Busqueda>({
    resolver: zodResolver(BusquedaSchema),
    defaultValues: { codigo: '' },
  })

  function buscar(datos: Busqueda) {
    usar.reset()
    if (datos.codigo === codigo) void consulta.refetch()
    else setCodigo(datos.codigo)
  }

  return (
    <Contenedor estrecho>
      <h1 className="mb-3 text-3xl font-bold">Validación de ingreso</h1>
      <p className="mb-6">Busca el código de la entrada para registrar el ingreso.</p>
      <form onSubmit={handleSubmit(buscar)} className="mb-6 flex flex-wrap items-end gap-3">
        <Campo
          etiqueta="Código del ticket"
          className="min-w-0 flex-1"
          autoFocus
          autoComplete="off"
          maxLength={100}
          disabled={usar.isPending}
          error={errors.codigo ? 'Escribe un código válido.' : undefined}
          {...register('codigo')}
        />
        <Boton type="submit" cargando={!!codigo && consulta.isFetching}
          disabled={usar.isPending}>
          Buscar
        </Boton>
      </form>
      {codigo && consulta.isFetching ? <p role="status">Buscando ticket...</p>
        : codigo && consulta.isError ? (
          <EstadoError error={consulta.error}
            reintentar={() => void consulta.refetch()}
            noDisponible="Ticket no encontrado o sin permisos" />
        ) : codigo && consulta.data ? (
          <>
            <TarjetaTicket ticket={consulta.data} detalle permitirCancelar={false} />
            {consulta.data.status === 'PAID' ? (
              <Boton className="mt-5" cargando={usar.isPending}
                onClick={() => {
                  if (usar.isPending) return
                  usar.mutate(consulta.data.ticketCode, {
                    onSuccess: () => {
                      reset({ codigo: '' })
                      setFocus('codigo')
                    },
                  })
                }}>
                Marcar como usado
              </Boton>
            ) : <p className="mt-5" role="status">
              {consulta.data.status === 'USED'
                ? 'Este ticket ya fue utilizado.'
                : 'Solo se pueden validar tickets pagados.'}
            </p>}
          </>
        ) : null}
      {usar.isSuccess && <p role="status"
        className="mt-5 font-semibold text-emerald-700 dark:text-emerald-300">
        Ingreso registrado: {usar.data.ticketCode}.
      </p>}
      {usar.isError && <p role="alert" className="mt-5 text-red-600 dark:text-red-300">
        {usar.error.message}
      </p>}
    </Contenedor>
  )
}
