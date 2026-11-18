import { useQuery } from '@tanstack/react-query'
import { obtenerSede, obtenerSedesActivas } from './api'

export const useSedesActivas = () => useQuery({
  queryKey: ['sedes', 'activas'],
  queryFn: obtenerSedesActivas,
  staleTime: 300_000
})
export const useSede = (codigo: string) => useQuery({
  queryKey: ['sede', codigo],
  queryFn: () => obtenerSede(codigo),
  staleTime: 300_000,
  enabled: !!codigo
})
