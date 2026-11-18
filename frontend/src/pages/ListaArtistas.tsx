import Contenedor from '../componentes/Contenedor'
import { Cargando, EstadoError, EstadoVacio } from '../componentes/EstadoConsulta'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import TarjetaArtista from '../funcionalidades/artistas/TarjetaArtista'

export default function ListaArtistas() {
  const consulta = useArtistasActivos()
  return (
    <Contenedor>
      <h1 className="text-3xl font-bold">Artistas</h1>
      <p className="mb-8 mt-3 text-slate-600 dark:text-slate-300">
        Conoce a quienes hacen vibrar cada escenario.
      </p>
      {consulta.isPending
        ? <Cargando />
        : consulta.isError
          ? <EstadoError error={consulta.error} reintentar={() => void consulta.refetch()} />
          : consulta.data.length
            ? <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
              {consulta.data.map(artista => (
                <TarjetaArtista key={artista.id} artista={artista} />
              ))}
            </div>
            : <EstadoVacio titulo="Aún no hay artistas activos" />}

    </Contenedor>
  )
}
