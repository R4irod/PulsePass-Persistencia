import { useState } from 'react'
import { Navigate, useLocation, useParams } from 'react-router'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import Contenedor from '../componentes/Contenedor'
import Tarjeta from '../componentes/Tarjeta'
import Boton from '../componentes/Boton'
import EnlaceBoton from '../componentes/EnlaceBoton'
import Badge from '../componentes/Badge'
import Modal from '../componentes/Modal'
import { Cargando, EstadoError } from '../componentes/EstadoConsulta'
import { useEvento } from '../funcionalidades/eventos/hooks'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import { asociarArtista, publicarEvento } from '../funcionalidades/eventos/admin'
import { ApiError } from '../lib/http'
import { etiquetaCategoria } from '../lib/etiquetas'
import type { Evento } from '../lib/esquemas'
import { RUTAS } from '../lib/rutas'

export default function AdminGestionarEvento() {
  const { codigo = '' } = useParams()
  // Reinicia la selección y los mensajes cuando cambia el código de la ruta.
  return <GestionEvento key={codigo} codigo={codigo} />
}

function GestionEvento({ codigo }: { codigo: string }) {
  const consulta = useEvento(codigo)
  const artistas = useArtistasActivos()
  const cliente = useQueryClient()
  const ubicacion = useLocation()
  const [artistaId, setArtistaId] = useState('')
  const [confirmar, setConfirmar] = useState(false)

  async function actualizar(evento: Evento) {
    cliente.setQueryData(['evento', codigo], evento)
    await cliente.invalidateQueries({ queryKey: ['eventos'] })
  }

  const publicar = useMutation({
    mutationFn: () => publicarEvento(codigo),
    onSuccess: async evento => {
      setConfirmar(false)
      await actualizar(evento)
    },
  })
  const asociar = useMutation({
    mutationFn: (id: number) => asociarArtista(codigo, id),
    onSuccess: async evento => {
      setArtistaId('')
      await actualizar(evento)
    },
  })
  const sinPermisos = [consulta.error, publicar.error, asociar.error].some(
    error => error instanceof ApiError && error.status === 403,
  )
  if (sinPermisos) return <Navigate to={RUTAS.sinPermisos} replace />

  const evento = consulta.data
  const disponibles = artistas.data?.filter(artista => !evento?.artists.includes(artista.stageName)) ?? []
  const ocupado = publicar.isPending || asociar.isPending

  return (
    <Contenedor estrecho>
      <EnlaceBoton variante="fantasma" to={RUTAS.adminEventos}>Volver a administración</EnlaceBoton>
      <h1 className="my-5 text-3xl font-bold">Gestionar evento</h1>
      {consulta.isPending ? <Cargando /> : consulta.isError ? (
        <EstadoError error={consulta.error} reintentar={() => void consulta.refetch()} />
      ) : evento && (
        <div className="space-y-6">
          {ubicacion.state?.creado && <p role="status">Evento creado correctamente. Ya puedes añadir artistas y publicarlo.</p>}
          <Tarjeta className="space-y-4 p-6">
            <Badge tono={evento.status} />
            <h2 className="break-words text-2xl font-bold">{evento.name}</h2>
            <p className="break-all">Código: {evento.eventCode}</p>
            <p className="whitespace-pre-wrap break-words">{evento.description || 'Sin descripción.'}</p>
            <dl className="grid gap-3 sm:grid-cols-2">
              <div><dt className="font-semibold">Categoría</dt><dd>{etiquetaCategoria[evento.category]}</dd></div>
              <div><dt className="font-semibold">Fecha y hora</dt><dd>{new Date(evento.eventDate).toLocaleString('es-CO')}</dd></div>
              <div><dt className="font-semibold">Sede</dt><dd>{evento.venueName || evento.venueCode}</dd></div>
              <div><dt className="font-semibold">Edad mínima</dt><dd>{evento.minimumAge} años</dd></div>
            </dl>
            {evento.status === 'DRAFT' && <Boton disabled={ocupado} onClick={() => {
              publicar.reset()
              setConfirmar(true)
            }}>Publicar evento</Boton>}
            {publicar.isSuccess && <p role="status">Evento publicado. Ya aparece en el catálogo.</p>}
          </Tarjeta>

          <Tarjeta className="space-y-4 p-6">
            <h2 className="text-xl font-bold">Artistas del evento</h2>
            {evento.artists.length ? (
              <ul className="list-inside list-disc">{evento.artists.map(nombre => <li key={nombre}>{nombre}</li>)}</ul>
            ) : <p>Este evento aún no tiene artistas asociados.</p>}
            {evento.status === 'CANCELLED' || evento.status === 'FINISHED' ? (
              <p>No se pueden añadir artistas a un evento cancelado o finalizado.</p>
            ) : artistas.isPending ? <Cargando /> : artistas.isError ? (
              <EstadoError error={artistas.error} reintentar={() => void artistas.refetch()} />
            ) : disponibles.length === 0 ? <p>No hay más artistas activos disponibles para añadir.</p> : (
              <form className="space-y-3" onSubmit={e => {
                e.preventDefault()
                if (artistaId && !ocupado) asociar.mutate(Number(artistaId))
              }}>
                <label htmlFor="artista" className="block">Añadir un artista existente</label>
                <select id="artista" required value={artistaId} disabled={ocupado}
                  onChange={e => { setArtistaId(e.target.value); asociar.reset() }}
                  className="min-h-11 w-full rounded-xl border p-3 dark:bg-slate-900">
                  <option value="">Selecciona un artista</option>
                  {disponibles.map(artista => <option key={artista.id} value={artista.id}>{artista.stageName}</option>)}
                </select>
                <Boton type="submit" disabled={!artistaId || publicar.isPending} cargando={asociar.isPending}>Añadir artista</Boton>
              </form>
            )}
            {asociar.isError && <p role="alert" className="text-red-600">{asociar.error.message}</p>}
            {asociar.isSuccess && <p role="status">Artista añadido correctamente.</p>}
          </Tarjeta>
        </div>
      )}
      <Modal abierto={confirmar} titulo="Publicar evento" ocupado={publicar.isPending}
        onCerrar={() => setConfirmar(false)}>
        <p>¿Publicar {evento?.name}? Aparecerá en el catálogo para que los usuarios puedan comprar tickets.</p>
        {publicar.isError && <p role="alert" className="mt-3 text-red-600">{publicar.error.message}</p>}
        <div className="mt-5 flex gap-3">
          <Boton variante="secundario" disabled={publicar.isPending} onClick={() => setConfirmar(false)}>Cancelar</Boton>
          <Boton cargando={publicar.isPending} onClick={() => {
            if (!ocupado) publicar.mutate()
          }}>Confirmar publicación</Boton>
        </div>
      </Modal>
    </Contenedor>
  )
}
