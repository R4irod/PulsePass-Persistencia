import { Navigate, Outlet } from 'react-router'
import type { Rol } from '../lib/esquemas'
import { RUTAS } from '../lib/rutas'
import { useAuth } from '../funcionalidades/auth/useAuth'

interface RutaPorRolProps {
  roles: Rol[]
}

// Se coloca dentro de RutaProtegida. El backend aplica los permisos reales.
export default function RutaPorRol({ roles }: RutaPorRolProps) {
  const { sesion } = useAuth()
  if (sesion.estado !== 'autenticado') return null

  const permitido = sesion.usuario.roles.some(rol => roles.includes(rol))
  return permitido ? <Outlet /> : <Navigate to={RUTAS.sinPermisos} replace />
}
