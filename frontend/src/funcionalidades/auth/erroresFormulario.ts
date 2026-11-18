import type {
  FieldValues,
  Path,
  UseFormSetError,
} from 'react-hook-form'
import { ApiError } from '../../lib/http'

export function mostrarErrorFormulario<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  campos: readonly Path<T>[],
  operacion: 'login' | 'registro',
) {
  let mensaje = 'No pudimos completar la solicitud. Inténtalo de nuevo.'
  if (error instanceof ApiError) {
    mensaje = error.message
    if (operacion === 'login' && error.status === 401) {
      mensaje = 'Usuario o contraseña incorrectos.'
    }
    if (operacion === 'registro' && error.status === 409) {
      mensaje = 'El usuario o correo ya está registrado.'
    }
    if (error.status === 400) {
      for (const campo of campos) {
        const clave = campo === 'password'
          ? error.details.password ?? error.details.passwordWithinBcryptLimit
          : error.details[campo]
        if (clave) {
          setError(campo, {
            type: 'server',
            message: 'Revisa este dato; el servidor no lo aceptó.',
          })
        }
      }
    }
  }
  setError('root', { type: 'server', message: mensaje })
}
