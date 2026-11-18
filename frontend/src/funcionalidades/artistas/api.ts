import { z } from 'zod'
import { ArtistaSchema } from '../../lib/esquemas'
import { http } from '../../lib/http'

export const obtenerArtistasActivos = () => http('/artists/active', z.array(ArtistaSchema))
export const obtenerArtista = (id: number) => http(`/artists/${id}`, ArtistaSchema)
export const obtenerArtistaPorNombre = (nombre: string) =>
  http('/artists/by-stage-name', ArtistaSchema, {
    parametros: { stageName: nombre },
  })
