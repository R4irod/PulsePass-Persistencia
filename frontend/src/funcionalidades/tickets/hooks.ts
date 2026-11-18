import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../auth/useAuth'
import {
  comprarTicket,
  cancelarTicket,
  obtenerMisTickets,
  obtenerTicket,
  usarTicket,
  obtenerTicketsPagados,
} from './api'

export function useMisTickets() {
  const { sesion } = useAuth()
  const email = sesion.estado === 'autenticado' ? sesion.usuario.email : ''
  return useQuery({
    queryKey: ['tickets', email],
    queryFn: () => obtenerMisTickets(email),
    enabled: !!email,
  })
}

export function useTicketsPagados(codigo: string) {
  return useQuery({
    queryKey: ['tickets-pagados', codigo],
    queryFn: () => obtenerTicketsPagados(codigo),
    enabled: !!codigo,
  })
}

export function useUsarTicket() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: usarTicket,
    retry: false,
    onSuccess: (ticket) => {
      cliente.setQueryData(['ticket', ticket.ticketCode], ticket)
      void cliente.invalidateQueries({ queryKey: ['tickets', ticket.userEmail] })
      void cliente.invalidateQueries({ queryKey: ['tickets-pagados', ticket.eventCode] })
    },
  })
}

export function useTicket(codigo: string) {
  return useQuery({
    queryKey: ['ticket', codigo],
    queryFn: () => obtenerTicket(codigo),
    enabled: !!codigo,
  })
}

export function useComprarTicket() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: comprarTicket,
    retry: false,
    onSuccess: (ticket) => {
      cliente.setQueryData(['ticket', ticket.ticketCode], ticket)
      void cliente.invalidateQueries({ queryKey: ['tickets', ticket.userEmail] })
      void cliente.invalidateQueries({ queryKey: ['evento', ticket.eventCode] })
      void cliente.invalidateQueries({ queryKey: ['eventos'] })
    },
  })
}

export function useCancelarTicket() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: cancelarTicket,
    retry: false,
    onSuccess: (ticket) => {
      cliente.setQueryData(['ticket', ticket.ticketCode], ticket)
      void cliente.invalidateQueries({ queryKey: ['tickets', ticket.userEmail] })
    },
  })
}
