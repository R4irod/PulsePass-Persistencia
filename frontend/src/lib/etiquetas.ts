export type EstadoVisual =
  | 'DRAFT'
  | 'PUBLISHED'
  | 'SOLD_OUT'
  | 'CANCELLED'
  | 'FINISHED'
  | 'RESERVED'
  | 'PAID'
  | 'USED'

// Estados de eventos y de tickets
export const etiquetaEstado: Record<EstadoVisual, string> = {
  DRAFT: 'Borrador',
  PUBLISHED: 'Publicado',
  SOLD_OUT: 'Agotado',
  CANCELLED: 'Cancelado',
  FINISHED: 'Finalizado',
  RESERVED: 'Reservado',
  PAID: 'Pagado',
  USED: 'Usado',
}

export const etiquetaCategoria = {
  MUSIC: 'Música',
  SPORTS: 'Deportes',
  TECHNOLOGY: 'Tecnología',
  EDUCATION: 'Educación',
  CULTURE: 'Cultura',
  ENTERTAINMENT: 'Entretenimiento',
} as const

export const etiquetaTipoTicket = {
  GENERAL: 'General',
  VIP: 'VIP',
  BACKSTAGE: 'Backstage',
  STUDENT: 'Estudiante',
} as const

export const etiquetaRol = {
  USER: 'Usuario',
  STAFF: 'Personal',
  ADMIN: 'Administrador',
} as const