import { useQuery } from '@tanstack/react-query'
import { obtenerEvento, obtenerEventosPorArtista, obtenerEventosPublicados } from './api'

export const useEventosPublicados = (enabled = true) => useQuery({
  queryKey: ['eventos', 'publicados'],
  queryFn: obtenerEventosPublicados,
  staleTime: 60_000,
  enabled
})
export const useEvento = (codigo: string) => useQuery({
  queryKey: ['evento', codigo],
  queryFn: () => obtenerEvento(codigo),
  staleTime: 30_000,
  enabled: !!codigo
})
export const useEventosPorArtista = (nombre: string) => useQuery({
  queryKey: ['eventos', 'artista', nombre],
  queryFn: () => obtenerEventosPorArtista(nombre),
  staleTime: 60_000,
  enabled: !!nombre
})
