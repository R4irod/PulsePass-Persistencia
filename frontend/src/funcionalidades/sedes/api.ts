import { z } from 'zod'
import { SedeSchema } from '../../lib/esquemas'
import { http } from '../../lib/http'

export const obtenerSedesActivas = () => http('/venues/active', z.array(SedeSchema))
export const obtenerSede = (codigo: string) => http(`/venues/${encodeURIComponent(codigo)}`, SedeSchema)
