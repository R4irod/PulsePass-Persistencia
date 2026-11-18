import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../funcionalidades/auth/useAuth'
import { RUTAS } from '../lib/rutas'

export default function RutaProtegida() {
  const { sesion } = useAuth()
  const ubicacion = useLocation()

  if (sesion.estado === 'cargando') {
    return <p role="status" className="p-8">Comprobando tu sesión…</p>
  }
  if (sesion.estado === 'anonimo') {
    return (
      <Navigate
        to={RUTAS.login}
        replace
        state={{
          desde: ubicacion.pathname + ubicacion.search + ubicacion.hash,
          motivo: sesion.motivo,
        }}
      />
    )
  }
  return <Outlet />
}
