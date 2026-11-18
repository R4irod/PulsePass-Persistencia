import { Link, useParams } from 'react-router'
import Contenedor from '../componentes/Contenedor'
import Tarjeta from '../componentes/Tarjeta'
import { Cargando, EstadoError } from '../componentes/EstadoConsulta'
import { useSede } from '../funcionalidades/sedes/hooks'
import { CodigoRutaSchema } from '../lib/catalogo'
import { numero } from '../lib/formato'
import { RUTAS } from '../lib/rutas'
import NoEncontrada from './NoEncontrada'

export default function DetalleSede() {
  const { code } = useParams()
  const validado = CodigoRutaSchema.safeParse(code)
  const consulta = useSede(validado.success ? validado.data : '')
  if (!validado.success) return <NoEncontrada />
  return (
    <Contenedor estrecho>
      <Link
        to={RUTAS.sedes}
        className="mb-6 inline-flex min-h-11 items-center text-marca-700 dark:text-marca-300"
      >
        ← Volver a sedes
      </Link>
      {consulta.isPending
        ? <Cargando />
        : consulta.isError
          ? <EstadoError
            error={consulta.error}
            reintentar={() => void consulta.refetch()}
            noDisponible="Sede no disponible"
          />
          : <Tarjeta className="p-6 sm:p-10">
            <p className="mb-3 break-all font-mono text-sm text-slate-500 dark:text-slate-400">
              {consulta.data.code}
            </p>
            <h1 className="break-words text-3xl font-bold">{consulta.data.name}</h1>
            <p className="mt-3 font-medium text-emerald-700 dark:text-emerald-300">
              {consulta.data.active ? 'Activa' : 'Inactiva'}
            </p>
            <dl className="mt-8 space-y-6">
              <div>
                <dt className="text-sm text-slate-500 dark:text-slate-400">Ciudad</dt>
                <dd className="text-lg">{consulta.data.city}</dd>
              </div>
              <div>
                <dt className="text-sm text-slate-500 dark:text-slate-400">Dirección</dt>
                <dd className="text-lg">{consulta.data.address || 'Dirección no informada'}</dd>
              </div>
              <div>
                <dt className="text-sm text-slate-500 dark:text-slate-400">Capacidad de la sede</dt>
                <dd className="text-lg">{numero(consulta.data.capacity)} personas</dd>
              </div>
            </dl>
          </Tarjeta>}
    </Contenedor>
  )
}
