import { Link, useParams } from 'react-router'
import Contenedor from '../componentes/Contenedor'
import Tarjeta from '../componentes/Tarjeta'
import Badge from '../componentes/Badge'
import { Cargando, EstadoError } from '../componentes/EstadoConsulta'
import { useEvento } from '../funcionalidades/eventos/hooks'
import { useArtistaPorNombre } from '../funcionalidades/artistas/hooks'
import { useSede } from '../funcionalidades/sedes/hooks'
import PanelCategoria from '../funcionalidades/eventos/PanelCategoria'
import TarjetaArtista from '../funcionalidades/artistas/TarjetaArtista'
import TarjetaSede from '../funcionalidades/sedes/TarjetaSede'
import { CodigoRutaSchema } from '../lib/catalogo'
import { fechaEvento } from '../lib/formato'
import { RUTAS } from '../lib/rutas'
import NoEncontrada from './NoEncontrada'
import PanelCompra from '../funcionalidades/tickets/PanelCompra'

function ArtistaEvento({ nombre }: { nombre: string }) {
  const consulta = useArtistaPorNombre(nombre)
  if (consulta.isPending) return (
    <Tarjeta className="p-6" role="status">Cargando a {nombre}…</Tarjeta>
  )
  if (consulta.isError) return (
    <div>
      <p className="mb-2 font-semibold">{nombre}</p>
      <EstadoError
        error={consulta.error}
        reintentar={() => void consulta.refetch()}
        noDisponible="Perfil no disponible"
      />
    </div>
  )
  return <TarjetaArtista artista={consulta.data} />
}

function SedeEvento({ codigo }: { codigo: string }) {
  const consulta = useSede(codigo)
  if (consulta.isPending) return <Cargando />
  if (consulta.isError) return <EstadoError
    error={consulta.error}
    reintentar={() => void consulta.refetch()}
    noDisponible="Sede no disponible"
  />
  return (
    <div className="grid">
      <TarjetaSede sede={consulta.data} />
    </div>
  )
}

export default function DetalleEvento() {
  const { eventCode } = useParams()
  const validado = CodigoRutaSchema.safeParse(eventCode)
  const consulta = useEvento(validado.success ? validado.data : '')
  if (!validado.success) return <NoEncontrada />
  return (
    <Contenedor>
      <Link
        to={RUTAS.eventos}
        className="mb-6 inline-flex min-h-11 items-center text-marca-700 dark:text-marca-300"
      >
        ← Volver a la cartelera
      </Link>
      {consulta.isPending
        ? <Cargando />
        : consulta.isError
          ? <EstadoError
            error={consulta.error}
            reintentar={() => void consulta.refetch()}
            noDisponible="Evento no disponible"
          />
          : <article>
            <Tarjeta className="overflow-hidden">
              <PanelCategoria categoria={consulta.data.category} grande />
              <div className="p-6 sm:p-10">
                <Badge tono={consulta.data.status} />
                <h1 className="mt-4 break-words text-3xl font-bold sm:text-4xl">{consulta.data.name}</h1>
                <p className="mt-2 break-all font-mono text-sm text-slate-500 dark:text-slate-400">
                  {consulta.data.eventCode}
                </p>
                <dl className="my-7 grid gap-5 sm:grid-cols-2">
                  <div>
                    <dt className="text-sm text-slate-500 dark:text-slate-400">Fecha y hora</dt>
                    <dd>
                      <time dateTime={consulta.data.eventDate}>{fechaEvento(consulta.data.eventDate)}</time>
                    </dd>
                  </div>
                  <div>
                    <dt className="text-sm text-slate-500 dark:text-slate-400">Edad mínima</dt>
                    <dd>
                      {consulta.data.minimumAge === 0 ? 'Todas las edades' : `${consulta.data.minimumAge} años`}
                    </dd>
                  </div>
                </dl>
                <p className="whitespace-pre-line leading-relaxed text-slate-600 dark:text-slate-300">
                  {consulta.data.description || 'Este evento todavía no tiene descripción.'}
                </p>
              </div>
            </Tarjeta>
            <PanelCompra evento={consulta.data} />
            <div className="mt-8 grid gap-8 lg:grid-cols-[2fr_1fr]">
              <section>
                <h2 className="mb-5 text-2xl font-semibold">Artistas</h2>
                {consulta.data.artists.length
                  ? <div className="grid gap-4 sm:grid-cols-2">
                    {consulta.data.artists.map(nombre => (
                      <ArtistaEvento key={nombre} nombre={nombre} />
                    ))}
                  </div>
                  : <p className="text-slate-600 dark:text-slate-300">
                    Los artistas se anunciarán próximamente.
                  </p>}
              </section>
              <section>
                <h2 className="mb-5 text-2xl font-semibold">Sede</h2>
                <SedeEvento codigo={consulta.data.venueCode} />
              </section>
            </div>
          </article>}

    </Contenedor>
  )
}
