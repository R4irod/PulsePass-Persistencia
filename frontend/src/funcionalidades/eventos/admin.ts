import { z } from 'zod'
import { CategoriaSchema, EventoSchema } from '../../lib/esquemas'
import { http } from '../../lib/http'

export const CrearEventoSchema = z.object({
  eventCode: z.string().trim().min(1, 'Escribe un código.').max(30),
  name: z.string().trim().min(1, 'Escribe un nombre.').max(150),
  description: z.string().trim().max(2000),
  category: CategoriaSchema,
  eventDate: z.string().refine(
    fecha => Number.isFinite(Date.parse(fecha)) && Date.parse(fecha) > Date.now(),
    'Selecciona una fecha futura.',
  ),
  minimumAge: z.number().int('Usa un número entero.').min(0, 'La edad no puede ser negativa.'),
  venueCode: z.string().trim().min(1, 'Selecciona una sede.').max(30),
})

export type DatosCrearEvento = z.infer<typeof CrearEventoSchema>

export const crearEvento = (datos: DatosCrearEvento) =>
  http('/events', EventoSchema, { metodo: 'POST', cuerpo: CrearEventoSchema.parse(datos) })

export const publicarEvento = (codigo: string) =>
  http(`/events/${encodeURIComponent(codigo)}/publish`, EventoSchema, { metodo: 'PATCH' })

export const asociarArtista = (codigo: string, id: number) =>
  http(`/events/${encodeURIComponent(codigo)}/artists/${id}`, EventoSchema, { metodo: 'POST' })

// Solo sirve como acceso rápido: los eventos siguen guardados en el backend.
const clave = 'pulsepass-eventos-creados'
export function leerEventosRecientes(): string[] {
  try {
    return z.array(z.string()).parse(JSON.parse(sessionStorage.getItem(clave) ?? '[]'))
  } catch {
    return []
  }
}

export function recordarEvento(codigo: string) {
  try {
    sessionStorage.setItem(clave, JSON.stringify(
      [codigo, ...leerEventosRecientes().filter(actual => actual !== codigo)].slice(0, 20),
    ))
  } catch {
    // El bloqueo del almacenamiento no impide crear eventos.
  }
}
