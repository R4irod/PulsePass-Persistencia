import { useQuery } from '@tanstack/react-query'
import { obtenerArtista, obtenerArtistaPorNombre, obtenerArtistasActivos } from './api'

export const useArtistasActivos = () => useQuery({
  queryKey: ['artistas', 'activos'],
  queryFn: obtenerArtistasActivos,
  staleTime: 300_000
})
export const useArtista = (id: number) => useQuery({
  queryKey: ['artista', id],
  queryFn: () => obtenerArtista(id),
  staleTime: 300_000,
  enabled: Number.isSafeInteger(id) && id > 0
})
export const useArtistaPorNombre = (nombre: string) => useQuery({
  queryKey: ['artista', 'nombre', nombre],
  queryFn: () => obtenerArtistaPorNombre(nombre),
  staleTime: 300_000,
  enabled: !!nombre
})
