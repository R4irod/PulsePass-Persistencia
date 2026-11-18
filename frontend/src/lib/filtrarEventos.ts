import type { EventoResumen } from './esquemas'
import type { FiltrosCatalogo } from './catalogo'

const normalizar = (texto: string) => texto
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .toLocaleLowerCase('es-CO')

export function filtrarEventos(eventos: EventoResumen[], filtros: FiltrosCatalogo) {
  // Por artista, STAFF/ADMIN puede recibir borradores: la cartelera conserva publicados.
  // El servidor sigue siendo responsable de aplicar los permisos.
  return eventos.filter(evento => evento.status === 'PUBLISHED'
    && (!filtros.categoria || evento.category === filtros.categoria)
    && normalizar(evento.name).includes(normalizar(filtros.q.trim())))
}
