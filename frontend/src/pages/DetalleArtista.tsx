import { Link, useParams } from 'react-router'
import Contenedor from '../componentes/Contenedor'
import Tarjeta from '../componentes/Tarjeta'
import EnlaceBoton from '../componentes/EnlaceBoton'
import { Cargando, EstadoError, EstadoVacio } from '../componentes/EstadoConsulta'
import { useArtista } from '../funcionalidades/artistas/hooks'
import { useEventosPorArtista } from '../funcionalidades/eventos/hooks'
import GrillaEventos from '../funcionalidades/eventos/GrillaEventos'
import { IdArtistaSchema } from '../lib/catalogo'
import { RUTAS } from '../lib/rutas'
import NoEncontrada from './NoEncontrada'

function EventosArtista({ nombre }: { nombre: string }) {
  const consulta = useEventosPorArtista(nombre)
  const eventos = consulta.data?.filter(evento => evento.status === 'PUBLISHED') ?? []
  return (
    <section className="mt-10">
      <h2 className="mb-6 text-2xl font-semibold">Eventos de {nombre}</h2>
      {consulta.isPending
        ? <Cargando />
        : consulta.isError
          ? <EstadoError error={consulta.error} reintentar={() => void consulta.refetch()} />
          : eventos.length
            ? <GrillaEventos eventos={eventos} />
            : <EstadoVacio titulo="Sin eventos publicados">
              <EnlaceBoton to={RUTAS.eventos}>Explorar cartelera</EnlaceBoton>
            </EstadoVacio>}
    </section>
  )
}

export default function DetalleArtista() {
  const { id } = useParams()
  const validado = IdArtistaSchema.safeParse(id)
  const consulta = useArtista(validado.success ? validado.data : 0)
  if (!validado.success) return <NoEncontrada />
  return (
    <Contenedor>
      <Link
        to={RUTAS.artistas}
        className="mb-6 inline-flex min-h-11 items-center text-marca-700 dark:text-marca-300"
      >
        ← Volver a artistas
      </Link>
      {consulta.isPending
        ? <Cargando />
        : consulta.isError
          ? <EstadoError
            error={consulta.error}
            reintentar={() => void consulta.refetch()}
            noDisponible="Artista no disponible"
          />
          : <>
            <Tarjeta className="p-6 sm:p-10">
              <p className="mb-2 text-sm font-semibold text-marca-600 dark:text-marca-300">
                {consulta.data.active ? 'Artista activo' : 'Artista inactivo'}
              </p>
              <h1 className="break-words text-3xl font-bold">{consulta.data.stageName}</h1>
              <dl className="mt-6 grid gap-5 sm:grid-cols-2">
                <div>
                  <dt className="text-sm text-slate-500 dark:text-slate-400">Género</dt>
                  <dd>{consulta.data.genre || 'No informado'}</dd>
                </div>
                <div>
                  <dt className="text-sm text-slate-500 dark:text-slate-400">País</dt>
                  <dd>{consulta.data.country || 'No informado'}</dd>
                </div>
              </dl>
            </Tarjeta>
            <EventosArtista nombre={consulta.data.stageName} />
          </>}

    </Contenedor>
  )
}
