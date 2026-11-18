import type { Rol } from '../../lib/esquemas'
import { RUTAS } from '../../lib/rutas'

export function enlacesPorRol(roles: Rol[]) {
  const enlaces: Array<{ a: string; texto: string }> = [
    { a: RUTAS.eventos, texto: 'Eventos' },
    { a: RUTAS.artistas, texto: 'Artistas' },
    { a: RUTAS.sedes, texto: 'Sedes' },
  ]
  if (roles.includes('USER')) {
    enlaces.push({ a: RUTAS.misTickets, texto: 'Mis tickets' })
  }
  if (roles.includes('STAFF') || roles.includes('ADMIN')) {
    enlaces.push(
      { a: RUTAS.validacion, texto: 'Validar ingreso' },
      { a: RUTAS.staffEventos, texto: 'Tickets pagados' },
    )
  }
  if (roles.includes('ADMIN')) {
    enlaces.push({ a: RUTAS.adminEventos, texto: 'Administrar eventos' })
    enlaces.push({ a: RUTAS.adminRoles, texto: 'Roles de usuarios' })
  }
  return enlaces
}
