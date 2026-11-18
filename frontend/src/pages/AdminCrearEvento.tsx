import { useNavigate } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import Contenedor from '../componentes/Contenedor'
import Tarjeta from '../componentes/Tarjeta'
import Campo from '../componentes/Campo'
import Boton from '../componentes/Boton'
import EnlaceBoton from '../componentes/EnlaceBoton'
import { Cargando, EstadoError } from '../componentes/EstadoConsulta'
import { useSedesActivas } from '../funcionalidades/sedes/hooks'
import { CrearEventoSchema, crearEvento, recordarEvento, type DatosCrearEvento } from '../funcionalidades/eventos/admin'
import { CategoriaSchema } from '../lib/esquemas'
import { etiquetaCategoria } from '../lib/etiquetas'
import { ApiError } from '../lib/http'
import { traducirMensaje } from '../lib/mensajes'
import { RUTAS } from '../lib/rutas'

const estilo = 'block min-h-11 w-full rounded-xl border border-slate-300 bg-white p-3 dark:border-slate-700 dark:bg-slate-900'

export default function AdminCrearEvento() {
  const sedes = useSedesActivas()
  const navegar = useNavigate()
  const cliente = useQueryClient()
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<DatosCrearEvento>({
    resolver: zodResolver(CrearEventoSchema),
    defaultValues: { eventCode: '', name: '', description: '', category: 'MUSIC', eventDate: '', minimumAge: 0, venueCode: '' },
  })

  async function enviar(datos: DatosCrearEvento) {
    try {
      const evento = await crearEvento(datos)
      recordarEvento(evento.eventCode)
      cliente.setQueryData(['evento', evento.eventCode], evento)
      await navegar(RUTAS.adminEvento(evento.eventCode), { replace: true, state: { creado: true } })
    } catch (error) {
      if (error instanceof ApiError && error.status === 403) {
        await navegar(RUTAS.sinPermisos)
        return
      }
      setError('root', { message: error instanceof Error ? error.message : 'No se pudo crear el evento.' })
      if (error instanceof ApiError) {
        for (const campo of Object.keys(CrearEventoSchema.shape) as (keyof DatosCrearEvento)[]) {
          if (error.details[campo]) setError(campo, { message: traducirMensaje(error.details[campo], error.status) })
        }
      }
    }
  }

  return (
    <Contenedor estrecho>
      <EnlaceBoton variante="fantasma" to={RUTAS.adminEventos}>Volver a administración</EnlaceBoton>
      <h1 className="my-5 text-3xl font-bold">Crear evento</h1>
      <p className="mb-6">Se guardará como borrador. Después podrás añadir artistas y publicarlo.</p>
      {sedes.isPending ? <Cargando /> : sedes.isError ? (
        <EstadoError error={sedes.error} reintentar={() => void sedes.refetch()} />
      ) : !sedes.data.length ? <p>No hay sedes activas disponibles para crear un evento.</p> : (
        <Tarjeta className="p-6">
          <form noValidate onSubmit={handleSubmit(enviar)}>
            <fieldset disabled={isSubmitting} className="min-w-0 space-y-5">
              <legend className="sr-only">Datos del evento</legend>
              <Campo etiqueta="Código" maxLength={30} ayuda="Guárdalo para encontrar el evento después."
                error={errors.eventCode?.message} {...register('eventCode')} />
              <Campo etiqueta="Nombre" maxLength={150} error={errors.name?.message} {...register('name')} />
              <div>
                <label htmlFor="descripcion" className="mb-2 block">Descripción (opcional)</label>
                <textarea id="descripcion" rows={4} maxLength={2000} className={estilo}
                  aria-invalid={!!errors.description} aria-describedby="error-descripcion" {...register('description')} />
                <p id="error-descripcion" className="text-red-600">{errors.description?.message}</p>
              </div>
              <div>
                <label htmlFor="categoria" className="mb-2 block">Categoría</label>
                <select id="categoria" className={estilo} {...register('category')}>
                  {CategoriaSchema.options.map(categoria => <option key={categoria} value={categoria}>{etiquetaCategoria[categoria]}</option>)}
                </select>
                {errors.category && <p role="alert">{errors.category.message}</p>}
              </div>
              <Campo etiqueta="Fecha y hora" type="datetime-local" ayuda="Hora local del evento."
                error={errors.eventDate?.message} {...register('eventDate')} />
              <Campo etiqueta="Edad mínima" type="number" min={0} step={1}
                error={errors.minimumAge?.message} {...register('minimumAge', { valueAsNumber: true })} />
              <div>
                <label htmlFor="sede" className="mb-2 block">Sede</label>
                <select id="sede" className={estilo} aria-invalid={!!errors.venueCode}
                  aria-describedby="error-sede" {...register('venueCode')}>
                  <option value="">Selecciona una sede</option>
                  {sedes.data.map(sede => <option key={sede.code} value={sede.code}>{sede.name} — {sede.city}</option>)}
                </select>
                <p id="error-sede" className="text-red-600">{errors.venueCode?.message}</p>
              </div>
              {errors.root && <p role="alert" className="text-red-600">{errors.root.message}</p>}
              <Boton type="submit" cargando={isSubmitting}>Guardar borrador</Boton>
            </fieldset>
          </form>
        </Tarjeta>
      )}
    </Contenedor>
  )
}
