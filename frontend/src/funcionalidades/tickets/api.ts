import { z } from 'zod'
import { http } from '../../lib/http'
import { TicketSchema, TipoTicketSchema } from '../../lib/esquemas'

export const CompraSchema = z.object({
  userEmail: z.email(),
  eventCode: z.string().trim().min(1),
  type: TipoTicketSchema,
})

export type DatosCompra = z.infer<typeof CompraSchema>
export const CodigoTicketSchema = z.string().trim().min(1).max(100)

export function comprarTicket(datos: DatosCompra) {
  return http('/tickets', TicketSchema, {
    metodo: 'POST',
    cuerpo: CompraSchema.parse(datos),
  })
}

export function obtenerMisTickets(email: string) {
  return http('/tickets/by-user', z.array(TicketSchema), {
    parametros: { email },
  })
}

export function obtenerTicket(codigo: string) {
  return http(`/tickets/${encodeURIComponent(codigo)}`, TicketSchema)
}

export function cancelarTicket(codigo: string) {
  return http(`/tickets/${encodeURIComponent(codigo)}/cancel`, TicketSchema, {
    metodo: 'PATCH',
  })
}

export function usarTicket(codigo: string) {
  return http(`/tickets/${encodeURIComponent(codigo)}/use`, TicketSchema, {
    metodo: 'PATCH',
  })
}

export function obtenerTicketsPagados(codigo: string) {
  return http(
    `/events/${encodeURIComponent(codigo)}/tickets/paid`,
    z.array(TicketSchema),
  )
}
