import { z } from 'zod'

export const CategoriaSchema = z.enum([
  'MUSIC',
  'SPORTS',
  'TECHNOLOGY',
  'EDUCATION',
  'CULTURE',
  'ENTERTAINMENT',
])
export const EstadoEventoSchema = z.enum(['DRAFT', 'PUBLISHED', 'SOLD_OUT', 'CANCELLED', 'FINISHED'])
export const TipoTicketSchema = z.enum(['GENERAL', 'VIP', 'BACKSTAGE', 'STUDENT'])
export const EstadoTicketSchema = z.enum(['RESERVED', 'PAID', 'CANCELLED', 'USED'])
export const RolSchema = z.enum(['USER', 'STAFF', 'ADMIN'])

export const SedeSchema = z.object({
  id: z.number(),
  code: z.string(),
  name: z.string(),
  city: z.string(),
  address: z.string().nullish(),
  capacity: z.number(),
  active: z.boolean(),
})

export const ArtistaSchema = z.object({
  id: z.number(),
  stageName: z.string(),
  country: z.string().nullish(),
  genre: z.string().nullish(),
  active: z.boolean(),
})

export const EventoSchema = z.object({
  id: z.number(),
  eventCode: z.string(),
  name: z.string(),
  description: z.string().nullish(),
  category: CategoriaSchema,
  status: EstadoEventoSchema,
  eventDate: z.string(),
  minimumAge: z.number(),
  venueCode: z.string(),
  venueName: z.string().nullish(),
  artists: z.array(z.string()),
})

export const EventoResumenSchema = z.object({
  id: z.number(),
  eventCode: z.string(),
  name: z.string(),
  category: CategoriaSchema,
  status: EstadoEventoSchema,
  eventDate: z.string(),
  venueName: z.string().nullish(),
})

export const UsuarioSchema = z.object({
  id: z.number(),
  username: z.string(),
  email: z.string(),
  active: z.boolean(),
  firstName: z.string(),
  lastName: z.string(),
  phone: z.string().nullish(),
  city: z.string().nullish(),
  birthDate: z.string().nullish(),
})

export const TicketSchema = z.object({
  id: z.number(),
  ticketCode: z.string(),
  type: TipoTicketSchema,
  price: z.number(),
  status: EstadoTicketSchema,
  purchaseDate: z.string(),
  userEmail: z.string(),
  eventCode: z.string(),
  eventName: z.string(),
})

export const AuthSchema = z.object({
  accessToken: z.string().min(1),
  tokenType: z.literal('Bearer'),
  expiresIn: z.number().int().positive().max(86_400),
})

export const UsuarioAutenticadoSchema = z.object({
  id: z.number(),
  username: z.string(),
  email: z.string(),
  roles: z.array(RolSchema),
})

export const ErrorRespuestaSchema = z.object({
  status: z.number(),
  message: z.string(),
  details: z.record(z.string(), z.string()).default({}),
})

export type Categoria = z.infer<typeof CategoriaSchema>
export type EstadoEvento = z.infer<typeof EstadoEventoSchema>
export type TipoTicket = z.infer<typeof TipoTicketSchema>
export type EstadoTicket = z.infer<typeof EstadoTicketSchema>
export type Rol = z.infer<typeof RolSchema>
export type Sede = z.infer<typeof SedeSchema>
export type Artista = z.infer<typeof ArtistaSchema>
export type Evento = z.infer<typeof EventoSchema>
export type EventoResumen = z.infer<typeof EventoResumenSchema>
export type Usuario = z.infer<typeof UsuarioSchema>
export type Ticket = z.infer<typeof TicketSchema>
export type UsuarioAutenticado = z.infer<typeof UsuarioAutenticadoSchema>
