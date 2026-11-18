import { useSearchParams } from 'react-router'
import Contenedor from '../componentes/Contenedor'
import Boton from '../componentes/Boton'
import { Cargando, EstadoError, EstadoVacio } from '../componentes/EstadoConsulta'
import { useEventosPublicados, useEventosPorArtista } from '../funcionalidades/eventos/hooks'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import GrillaEventos from '../funcionalidades/eventos/GrillaEventos'
import { CategoriaSchema } from '../lib/esquemas'
import { etiquetaCategoria } from '../lib/etiquetas'
import { leerFiltros, type FiltrosCatalogo } from '../lib/catalogo'
import { filtrarEventos } from '../lib/filtrarEventos'

const campo =
  'mt-2 min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3 ' +
  'dark:border-slate-700 dark:bg-slate-900'

export default function CatalogoEventos() {
  const [parametros, setParametros] = useSearchParams()
  const filtros = leerFiltros(parametros)
  const publicados = useEventosPublicados(!filtros.artista)
  const porArtista = useEventosPorArtista(filtros.artista)
  const artistas = useArtistasActivos()
  const consulta = filtros.artista ? porArtista : publicados
  const eventos = filtrarEventos(consulta.data ?? [], filtros)
  const actualizar = (clave: keyof FiltrosCatalogo, valor: string) => {
    setParametros(actuales => {
      const siguientes = new URLSearchParams(actuales)
      if (valor) siguientes.set(clave, valor)
      else siguientes.delete(clave)
      return siguientes
    }, { replace: true })
  }
  return (
    <Contenedor>
      <p
        className="text-sm font-bold uppercase tracking-widest text-marca-600 dark:text-marca-300"
      >
        Encuentra tu próximo plan
      </p>
      <h1 className="mt-2 text-3xl font-bold sm:text-4xl">Cartelera</h1>
      <p className="mt-3 text-slate-600 dark:text-slate-300">
        Música, cultura y experiencias que se viven en persona.
      </p>
      <div
        className="my-8 grid items-end gap-4 rounded-2xl border border-slate-200
          bg-white p-5 sm:grid-cols-2 lg:grid-cols-[2fr_1fr_1fr_auto]
          dark:border-slate-800 dark:bg-slate-900"
      >
        <label className="text-sm font-medium" htmlFor="buscar-eventos">
          Buscar por nombre
          <input
            id="buscar-eventos"
            type="search"
            maxLength={200}
            value={filtros.q}
            onChange={e => actualizar('q', e.target.value)}
            placeholder="¿Qué quieres vivir?"
            className={campo}
          />
        </label>
        <label className="text-sm font-medium" htmlFor="categoria">
          Categoría
          <select
            id="categoria"
            value={filtros.categoria}
            onChange={e => actualizar('categoria', e.target.value)}
            className={campo}
          >
            <option value="">Todas las categorías</option>
            {CategoriaSchema.options.map(categoria => (
              <option key={categoria} value={categoria}>{etiquetaCategoria[categoria]}</option>
            ))}
          </select>
        </label>
        <label className="text-sm font-medium" htmlFor="artista">
          Artista
          <select
            id="artista"
            value={filtros.artista}
            onChange={e => actualizar('artista', e.target.value)}
            className={campo}
          >
            <option value="">Todos los artistas</option>
            {filtros.artista &&
              !artistas.data?.some(a => a.stageName === filtros.artista) && (
                <option value={filtros.artista}>{filtros.artista}</option>
              )}
            {artistas.data?.map(artista => (
              <option key={artista.id} value={artista.stageName}>{artista.stageName}</option>
            ))}
          </select>
        </label>
        <Boton variante="secundario" onClick={() => setParametros({})}>Limpiar filtros</Boton>
      </div>
      {artistas.isError && <div className="mb-6">
        <EstadoError error={artistas.error} reintentar={() => void artistas.refetch()} />
      </div>}

      {consulta.isPending
        ? <Cargando />
        : consulta.isError
          ? <EstadoError error={consulta.error} reintentar={() => void consulta.refetch()} />
          : <>
            <p role="status" className="mb-5 text-sm text-slate-600 dark:text-slate-300">
              {eventos.length} {eventos.length === 1 ? 'evento encontrado' : 'eventos encontrados'}
            </p>
            {eventos.length
              ? <GrillaEventos eventos={eventos} />
              : <EstadoVacio
                titulo={filtros.q || filtros.categoria || filtros.artista
                  ? 'Sin resultados para tu búsqueda'
                  : 'Aún no hay eventos publicados'}
              >
                <Boton variante="secundario" onClick={() => setParametros({})}>Limpiar filtros</Boton>
              </EstadoVacio>}
          </>}

    </Contenedor>
  )
}
