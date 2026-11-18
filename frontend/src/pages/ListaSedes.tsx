import Contenedor from '../componentes/Contenedor'
import { Cargando, EstadoError, EstadoVacio } from '../componentes/EstadoConsulta'
import { useSedesActivas } from '../funcionalidades/sedes/hooks'
import TarjetaSede from '../funcionalidades/sedes/TarjetaSede'

export default function ListaSedes() {
  const consulta = useSedesActivas()
  return <Contenedor><h1 className="text-3xl font-bold">Sedes</h1><p className="mb-8 mt-3 text-slate-600 dark:text-slate-300">Los espacios donde suceden los mejores encuentros.</p>{consulta.isPending ? <Cargando /> : consulta.isError ? <EstadoError error={consulta.error} reintentar={() => void consulta.refetch()} />
  : consulta.data.length ? <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">{consulta.data.map(sede => <TarjetaSede key={sede.code} sede={sede} />)}</div> : <EstadoVacio titulo="Aún no hay sedes activas" />}</Contenedor>
}
