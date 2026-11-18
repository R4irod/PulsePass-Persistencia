import { useQuery } from '@tanstack/react-query'
import { obtenerUsuarioPorEmail } from './api'
import { useAuth } from '../auth/useAuth'

export function useMiPerfil() {
  const { sesion } = useAuth()
  const email = sesion.estado === 'autenticado' ? sesion.usuario.email : ''

  return useQuery({
    queryKey: ['usuario', email],
    queryFn: () => obtenerUsuarioPorEmail(email),
    enabled: !!email,
    staleTime: 60_000,
  })
}
