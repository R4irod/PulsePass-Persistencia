import type { ZodType } from 'zod'
import { ErrorRespuestaSchema } from './esquemas'
import { traducirMensaje } from './mensajes'

const BASE_URL: string = import.meta.env?.VITE_API_BASE_URL ?? '/api'

export class ApiError extends Error {
  status: number
  details: Record<string, string>

  constructor(status: number, message: string, details: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

// El token vive en memoria; AuthContext lo actualizará al iniciar o cerrar sesión.
let token: string | null = null
let versionToken = 0
let alExpirar: (() => void) | null = null

export function fijarToken(nuevo: string | null) {
  versionToken += 1
  token = nuevo
}

export function registrarAlExpirar(funcion: (() => void) | null) {
  alExpirar = funcion
}

interface Opciones {
  metodo?: 'GET' | 'POST' | 'PATCH'
  cuerpo?: unknown
  parametros?: Record<string, string>
  autenticada?: boolean
}

export async function http<T>(
  ruta: string,
  esquema: ZodType<T>,
  { metodo = 'GET', cuerpo, parametros, autenticada = true }: Opciones = {},
): Promise<T> {
  const consulta = parametros ? `?${new URLSearchParams(parametros).toString()}` : ''
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (cuerpo !== undefined) headers['Content-Type'] = 'application/json'
  const tokenEnviado = autenticada ? token : null
  const versionEnviada = versionToken
  if (tokenEnviado) headers.Authorization = `Bearer ${tokenEnviado}`

  let respuesta: Response
  try {
    respuesta = await fetch(`${BASE_URL}${ruta}${consulta}`, {
      method: metodo,
      headers,
      body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
    })
  } catch {
    throw new ApiError(0, 'No pudimos conectar con el servidor.')
  }

  let datos: unknown
  try {
    datos = await respuesta.json()
  } catch {
    datos = null
  }

  if (!respuesta.ok) {
    // Un 401 con sesión activa significa token vencido o inválido.
    if (
      respuesta.status === 401 &&
      tokenEnviado &&
      versionEnviada === versionToken
    ) alExpirar?.()

    const error = ErrorRespuestaSchema.safeParse(datos)
    if (error.success) {
      throw new ApiError(
        respuesta.status,
        traducirMensaje(error.data.message, respuesta.status),
        error.data.details,
      )
    }
    throw new ApiError(respuesta.status, traducirMensaje('', respuesta.status))
  }

  const valido = esquema.safeParse(datos)
  if (!valido.success) {
    throw new ApiError(500, 'La respuesta del servidor no tiene el formato esperado.')
  }
  return valido.data
}
