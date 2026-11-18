import { useNavigate } from 'react-router'
import Contenedor from '../componentes/Contenedor'
import Tarjeta from '../componentes/Tarjeta'
import Boton from '../componentes/Boton'
import EnlaceBoton from '../componentes/EnlaceBoton'
import { Cargando, EstadoError } from '../componentes/EstadoConsulta'
import { useAuth } from '../funcionalidades/auth/useAuth'
import { useMiPerfil } from '../funcionalidades/usuarios/hooks'
import { etiquetaRol } from '../lib/etiquetas'
import { RUTAS } from '../lib/rutas'

export default function Cuenta() {
  const { sesion, cerrarSesion } = useAuth()
  const consulta = useMiPerfil()
  const navegar = useNavigate()

  function salir() {
    cerrarSesion()
    void navegar(RUTAS.inicio, { replace: true })
  }

  if (sesion.estado !== 'autenticado') return null

  return (
    <Contenedor estrecho>
      <h1 className="mb-7 text-3xl font-bold">Mi cuenta</h1>
      {consulta.isPending ? (
        <Cargando />
      ) : consulta.isError ? (
        <EstadoError
          error={consulta.error}
          reintentar={() => void consulta.refetch()}
        />
      ) : (
        <Tarjeta className="p-6 sm:p-10">
          <h2 className="break-words text-2xl font-semibold">
            {consulta.data.firstName} {consulta.data.lastName}
          </h2>
          <p className="mt-2 break-all text-slate-600 dark:text-slate-300">
            @{consulta.data.username} · {consulta.data.email}
          </p>
          <dl className="mt-7 grid gap-6 sm:grid-cols-2">
            <div>
              <dt className="text-sm text-slate-500 dark:text-slate-400">Rol</dt>
              <dd>{sesion.usuario.roles.map(rol => etiquetaRol[rol]).join(', ')}</dd>
            </div>
            <div>
              <dt className="text-sm text-slate-500 dark:text-slate-400">Estado</dt>
              <dd>{consulta.data.active ? 'Activa' : 'Inactiva'}</dd>
            </div>
            <div>
              <dt className="text-sm text-slate-500 dark:text-slate-400">
                Fecha de nacimiento
              </dt>
              <dd>
                {consulta.data.birthDate?.split('-').reverse().join('/') ||
                  'No informada'}
              </dd>
            </div>
            <div>
              <dt className="text-sm text-slate-500 dark:text-slate-400">Ciudad</dt>
              <dd>{consulta.data.city || 'No informada'}</dd>
            </div>
            <div>
              <dt className="text-sm text-slate-500 dark:text-slate-400">Teléfono</dt>
              <dd>{consulta.data.phone || 'No informado'}</dd>
            </div>
          </dl>
        </Tarjeta>
      )}
      <div className="mt-6 flex flex-wrap justify-between gap-3">
        {sesion.usuario.roles.includes('USER') && (
          <EnlaceBoton to={RUTAS.misTickets}>Ver mis tickets</EnlaceBoton>
        )}
        <Boton variante="secundario" onClick={salir}>Cerrar sesión</Boton>
      </div>
    </Contenedor>
  )
}
