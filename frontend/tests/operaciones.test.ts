import { afterEach, test } from 'node:test'
import assert from 'node:assert/strict'
import { RUTAS } from '../src/lib/rutas'
import { ApiError, fijarToken } from '../src/lib/http'
import { CrearEventoSchema, crearEvento, publicarEvento, asociarArtista, leerEventosRecientes, recordarEvento } from '../src/funcionalidades/eventos/admin'
import { comprarTicket, cancelarTicket, usarTicket, obtenerMisTickets, obtenerTicketsPagados } from '../src/funcionalidades/tickets/api'

const fetchOriginal = globalThis.fetch
const almacenamientoOriginal = Object.getOwnPropertyDescriptor(globalThis, 'sessionStorage')
const datos = {
  eventCode: 'CONCIERTO', name: 'Concierto', description: '', category: 'MUSIC' as const,
  eventDate: '2099-12-20T18:00', minimumAge: 0, venueCode: 'SEDE',
}
const evento = { ...datos, id: 1, status: 'DRAFT', artists: [] }
const ticket = {
  id: 1, ticketCode: 'T /1', type: 'GENERAL', price: 50000, status: 'PAID',
  purchaseDate: '2026-11-17T10:00', userEmail: 'persona@example.test',
  eventCode: 'CONCIERTO', eventName: 'Concierto',
}

afterEach(() => {
  globalThis.fetch = fetchOriginal
  fijarToken(null)
  if (almacenamientoOriginal) Object.defineProperty(globalThis, 'sessionStorage', almacenamientoOriginal)
  else Reflect.deleteProperty(globalThis, 'sessionStorage')
})

test('crear rechaza fecha pasada, edad inválida y campos vacíos antes de enviar', () => {
  let peticiones = 0
  globalThis.fetch = async () => { peticiones++; return Response.json(evento) }
  for (const cambio of [
    { eventDate: '2020-01-01T10:00' }, { eventDate: 'incorrecta' },
    { minimumAge: -1 }, { minimumAge: 2.5 }, { name: ' ' },
    { venueCode: '' }, { eventCode: 'x'.repeat(31) },
  ]) {
    assert.equal(CrearEventoSchema.safeParse({ ...datos, ...cambio }).success, false)
    assert.throws(() => crearEvento({ ...datos, ...cambio }))
  }
  assert.equal(peticiones, 0)
})

test('crear envía los datos limpios sin aceptar un estado impuesto por el cliente', async () => {
  fijarToken('token-prueba')
  globalThis.fetch = async (url, opciones) => {
    assert.equal(url, '/api/events')
    assert.equal(opciones?.method, 'POST')
    assert.equal(new Headers(opciones?.headers).get('Authorization'), 'Bearer token-prueba')
    assert.deepEqual(JSON.parse(String(opciones?.body)), datos)
    return Response.json(evento, { status: 201 })
  }
  await crearEvento({ ...datos, name: ' Concierto ', ...{ status: 'PUBLISHED' } })
})

test('publicar y asociar artistas usan códigos escapados y la respuesta actualizada', async () => {
  const llamadas: unknown[] = []
  globalThis.fetch = async (url, opciones) => {
    llamadas.push([url, opciones?.method, opciones?.body])
    return Response.json({ ...evento, status: 'PUBLISHED', artists: ['Artista'] })
  }
  assert.equal((await publicarEvento('A /B')).status, 'PUBLISHED')
  assert.deepEqual((await asociarArtista('A /B', 3)).artists, ['Artista'])
  assert.deepEqual(llamadas, [
    ['/api/events/A%20%2FB/publish', 'PATCH', undefined],
    ['/api/events/A%20%2FB/artists/3', 'POST', undefined],
  ])
})

test('el código nuevo abre gestión y nunca el formulario de creación', () => {
  assert.notEqual(RUTAS.adminEvento('nuevo'), RUTAS.adminNuevo)
  assert.equal(RUTAS.adminEvento('nuevo'), RUTAS.patronAdminEvento.replace(':codigo', 'nuevo'))
  assert.ok(RUTAS.adminEvento('A /B').endsWith('/A%20%2FB'))
})

test('los accesos recientes no se duplican y toleran almacenamiento bloqueado', () => {
  let valor = '[]'
  Object.defineProperty(globalThis, 'sessionStorage', { configurable: true, value: {
    getItem: () => valor,
    setItem: (_clave: string, nuevo: string) => { valor = nuevo },
  } })
  recordarEvento('A')
  recordarEvento('B')
  recordarEvento('A')
  assert.deepEqual(leerEventosRecientes(), ['A', 'B'])
  valor = 'datos corruptos'
  assert.deepEqual(leerEventosRecientes(), [])
  Object.defineProperty(globalThis, 'sessionStorage', { configurable: true, get() { throw new Error('Bloqueado') } })
  assert.doesNotThrow(() => recordarEvento('C'))
  assert.deepEqual(leerEventosRecientes(), [])
})

test('comprar no permite enviar un precio o estado elegidos por el cliente', async () => {
  globalThis.fetch = async (url, opciones) => {
    assert.equal(url, '/api/tickets')
    assert.equal(opciones?.method, 'POST')
    assert.deepEqual(JSON.parse(String(opciones?.body)), {
      userEmail: ticket.userEmail, eventCode: 'CONCIERTO', type: 'GENERAL',
    })
    return Response.json(ticket)
  }
  await comprarTicket({ userEmail: ticket.userEmail, eventCode: 'CONCIERTO', type: 'GENERAL', ...{ price: 0, status: 'USED' } })
})

test('cancelación y validación mandan PATCH y reciben el nuevo estado', async () => {
  globalThis.fetch = async (url, opciones) => {
    assert.equal(opciones?.method, 'PATCH')
    assert.equal(opciones?.body, undefined)
    assert.ok(String(url).startsWith('/api/tickets/T%20%2F1/'))
    return Response.json({ ...ticket, status: String(url).endsWith('/cancel') ? 'CANCELLED' : 'USED' })
  }
  assert.equal((await cancelarTicket(ticket.ticketCode)).status, 'CANCELLED')
  assert.equal((await usarTicket(ticket.ticketCode)).status, 'USED')
})

test('los listados codifican correo y código y aceptan resultados vacíos', async () => {
  const rutas: unknown[] = []
  globalThis.fetch = async url => { rutas.push(url); return Response.json([]) }
  assert.deepEqual(await obtenerMisTickets('persona+1@example.test'), [])
  assert.deepEqual(await obtenerTicketsPagados('A B'), [])
  assert.deepEqual(rutas, ['/api/tickets/by-user?email=persona%2B1%40example.test', '/api/events/A%20B/tickets/paid'])
})

test('un rechazo de permisos o conflicto no se presenta como éxito ni se reintenta', async () => {
  for (const status of [403, 409]) {
    let intentos = 0
    globalThis.fetch = async () => { intentos++; return Response.json({}, { status }) }
    await assert.rejects(publicarEvento('CONCIERTO'), error => error instanceof ApiError && error.status === status)
    assert.equal(intentos, 1)
  }
})
