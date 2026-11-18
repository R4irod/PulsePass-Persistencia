import { z } from 'zod'
import { RUTAS } from '../../lib/rutas'

const EstadoNavegacionSchema = z.object({
  desde: z.string().optional(),
  registrado: z.boolean().optional(),
  motivo: z.literal('sesion-expirada').optional(),
})

export function leerEstadoNavegacion(valor: unknown) {
  const resultado = EstadoNavegacionSchema.safeParse(valor)
  return resultado.success ? resultado.data : {}
}

export function destinoSeguro(valor: unknown): string {
  if (typeof valor !== 'string' || !valor.startsWith('/')) return RUTAS.inicio
  if (
    valor.startsWith('//') ||
    valor.includes('\\') ||
    [...valor].some(caracter => caracter.charCodeAt(0) <= 32)
  ) return RUTAS.inicio

  try {
    const url = new URL(valor, 'https://pulsepass.local')
    if (url.origin !== 'https://pulsepass.local') return RUTAS.inicio
    const ruta = decodeURIComponent(url.pathname).replace(/\/+$/, '') || '/'
    if (ruta === RUTAS.login || ruta === RUTAS.registro) return RUTAS.inicio
    return url.pathname + url.search + url.hash
  } catch {
    return RUTAS.inicio
  }
}
