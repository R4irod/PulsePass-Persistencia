import { useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { useAuth } from './useAuth'
import { RUTAS } from '../../lib/rutas'

export default function RedireccionSesion() {
  const { sesion } = useAuth()
  const ubicacion = useLocation()
  const navegar = useNavigate()

  useEffect(() => {
    if (
      sesion.estado === 'anonimo' &&
      sesion.motivo === 'sesion-expirada' &&
      ubicacion.pathname !== RUTAS.login
    ) {
      void navegar(RUTAS.login, {
        replace: true,
        state: {
          motivo: 'sesion-expirada',
          desde: ubicacion.pathname + ubicacion.search + ubicacion.hash,
        },
      })
    }
  }, [sesion, ubicacion, navegar])

  return null
}
