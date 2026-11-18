import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../funcionalidades/auth/useAuth'
import {
  destinoSeguro,
  leerEstadoNavegacion,
} from '../funcionalidades/auth/navegacion'

export default function SoloAnonimo() {
  const { sesion } = useAuth()
  const ubicacion = useLocation()
  const estado = leerEstadoNavegacion(ubicacion.state)

  if (sesion.estado === 'cargando') {
    return <p role="status" className="p-8">Comprobando tu sesión…</p>
  }
  if (sesion.estado === 'autenticado') {
    return <Navigate to={destinoSeguro(estado.desde)} replace />
  }
  return <Outlet />
}
