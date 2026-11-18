import { z } from 'zod'
import { CategoriaSchema } from './esquemas'

export const CodigoRutaSchema = z.string().trim().min(1).max(200)
export const IdArtistaSchema = z.string().regex(/^\d+$/).transform(Number).pipe(z.number().int().positive().max(Number.MAX_SAFE_INTEGER))
export const FiltrosCatalogoSchema = z.object({
  q: z.string().max(200).catch(''),


  categoria: z.union([z.literal(''), CategoriaSchema]).catch(''),


  artista: z.string().max(200).catch(''),
})
export type FiltrosCatalogo = z.infer<typeof FiltrosCatalogoSchema>
export function leerFiltros(parametros: URLSearchParams): FiltrosCatalogo {
  return FiltrosCatalogoSchema.parse({
    q: parametros.get('q') ?? '',

    categoria: parametros.get('categoria') ?? '',

    artista: parametros.get('artista') ?? ''
  })
}
