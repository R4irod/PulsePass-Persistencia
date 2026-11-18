import { test } from 'node:test'
import assert from 'node:assert/strict'
import { filtrarEventos } from '../src/lib/filtrarEventos.ts'
import { fechaEvento, numero } from '../src/lib/formato.ts'
import { RUTAS } from '../src/lib/rutas.ts'
import type { EventoResumen } from '../src/lib/esquemas.ts'

const eventos: EventoResumen[] = [
  {
    id: 1,
    eventCode: 'MUS-1',
    name: 'Música del Caribe',
    category: 'MUSIC',
    status: 'PUBLISHED',
    eventDate: '2026-12-12T20:00:00',
    venueName: 'Marina'
  },
  {
    id: 2,
    eventCode: 'CUL-1',
    name: 'Festival del Caribe',
    category: 'CULTURE',
    status: 'PUBLISHED',
    eventDate: '2026-12-13T20:00:00',
    venueName: null
  },
  {
    id: 3,
    eventCode: 'BOR-1',
    name: 'Música sin publicar',
    category: 'MUSIC',
    status: 'DRAFT',
    eventDate: '2026-12-14T20:00:00',
    venueName: 'Marina'
  },
]

test('la cartelera no expone borradores devueltos a un administrador', () => {
  assert.deepEqual(
    filtrarEventos(eventos, { q: '', categoria: '', artista: '' }).map(e => e.eventCode),
    ['MUS-1', 'CUL-1']
  )
})

test('la búsqueda admite mayúsculas, espacios y texto sin tildes', () => {
  assert.deepEqual(
    filtrarEventos(eventos, { q: ' MUSICA ', categoria: '', artista: '' }).map(e => e.eventCode),
    ['MUS-1']
  )
})

test('texto y categoría se aplican juntos', () => {
  assert.deepEqual(
    filtrarEventos(eventos, { q: 'caribe', categoria: 'CULTURE', artista: '' }).map(e => e.eventCode),
    ['CUL-1']
  )
})

test('una búsqueda sin coincidencias devuelve un listado vacío', () => {
  assert.equal(
    filtrarEventos(eventos, { q: 'inexistente', categoria: '', artista: '' }).length,
    0
  )
})

test('filtrar no modifica la respuesta almacenada en la caché', () => {
  const original = structuredClone(eventos)
  filtrarEventos(eventos, { q: 'música', categoria: '', artista: '' })
  assert.deepEqual(eventos, original)
})

test('los códigos con caracteres reservados permanecen en un solo segmento', () => {
  assert.equal(RUTAS.evento('FEST/1?#'), '/eventos/FEST%2F1%3F%23')
  assert.equal(RUTAS.sede('SEDE A/B'), '/sedes/SEDE%20A%2FB')
})

test('una fecha inválida no rompe la página', () => {
  assert.equal(fechaEvento('no-es-fecha'), 'Fecha no disponible')
})

test('LocalDateTime conserva el día y hora local del evento', () => {
  const esperado = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(2026, 11, 12, 20, 0, 0))
  assert.equal(fechaEvento('2026-12-12T20:00:00'), esperado)
})

test('la capacidad utiliza separadores colombianos', () => {
  assert.equal(numero(3000), '3.000')
})
