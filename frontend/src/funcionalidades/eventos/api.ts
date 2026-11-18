import { z } from 'zod'
import { EventoSchema, EventoResumenSchema } from '../../lib/esquemas'
import { http } from '../../lib/http'

export const obtenerEventosPublicados = () => http('/events/published', z.array(EventoResumenSchema))
export const obtenerEvento = (codigo: string) => http(`/events/${encodeURIComponent(codigo)}`, EventoSchema)
export const obtenerEventosPorArtista = (nombre: string) =>
  http('/events/by-artist', z.array(EventoResumenSchema), {
    parametros: { stageName: nombre },
  })
