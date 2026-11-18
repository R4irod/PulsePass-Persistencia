import { http } from '../../lib/http'
import { UsuarioSchema } from '../../lib/esquemas'

export function obtenerUsuarioPorEmail(email: string) {
  return http('/users/by-email', UsuarioSchema, {
    parametros: { email },
  })
}
