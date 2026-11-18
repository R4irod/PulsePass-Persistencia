import Hero from '../componentes/Hero'
import Contenedor from '../componentes/Contenedor'
import EnlaceBoton from '../componentes/EnlaceBoton'
import { Cargando, EstadoError, EstadoVacio } from '../componentes/EstadoConsulta'
import { useEventosPublicados } from '../funcionalidades/eventos/hooks'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import GrillaEventos from '../funcionalidades/eventos/GrillaEventos'
import TarjetaArtista from '../funcionalidades/artistas/TarjetaArtista'
import { RUTAS } from '../lib/rutas'

export default function Inicio() {
  const eventos = useEventosPublicados()
  const artistas = useArtistasActivos()
  return (
    <>
      <Hero />
      <Contenedor>
        <section className="py-4">
          <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-2xl font-bold">Próximos eventos</h2>
            <EnlaceBoton to={RUTAS.eventos} variante="fantasma">Ver cartelera →</EnlaceBoton>
          </div>
          {eventos.isPending
            ? <Cargando />
            : eventos.isError
              ? <EstadoError error={eventos.error} reintentar={() => void eventos.refetch()} />
              : eventos.data.length
                ? <GrillaEventos eventos={eventos.data.slice(0, 3)} />
                : <EstadoVacio titulo="Aún no hay eventos publicados" />}
        </section>

        <section className="py-10">
          <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-2xl font-bold">Artistas destacados</h2>
            <EnlaceBoton to={RUTAS.artistas} variante="fantasma">Ver artistas →</EnlaceBoton>
          </div>
          {artistas.isPending
            ? <Cargando />
            : artistas.isError
              ? <EstadoError error={artistas.error} reintentar={() => void artistas.refetch()} />
              : artistas.data.length
                ? <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
                  {artistas.data.slice(0, 6).map(artista => (
                    <TarjetaArtista key={artista.id} artista={artista} />
                  ))}
                </div>
                : <EstadoVacio titulo="Aún no hay artistas activos" />}
        </section>

      </Contenedor>
    </>
  )
}
