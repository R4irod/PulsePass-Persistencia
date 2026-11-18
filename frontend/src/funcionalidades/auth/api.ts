import {
  AuthSchema,
  UsuarioAutenticadoSchema,
  UsuarioSchema,
} from '../../lib/esquemas'

import { http } from '../../lib/http'
import { LoginSchema, type DatosLogin } from './tipos'
import { RegistroSchema, type DatosRegistro } from './registro'

export function iniciarSesion(datos: DatosLogin) {
  const datosValidados = LoginSchema.parse(datos)

  return http('/auth/login', AuthSchema, {
    metodo: 'POST',
    cuerpo: datosValidados,
    autenticada: false,
  })
}

export function obtenerUsuarioActual() {
  return http('/auth/me', UsuarioAutenticadoSchema)
}

export function registrarUsuario(datos: DatosRegistro) {
  return http('/users', UsuarioSchema, {
    metodo: 'POST',
    cuerpo: RegistroSchema.parse(datos),
    autenticada: false,
  })
}
