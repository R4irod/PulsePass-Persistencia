import { afterEach, beforeEach, test } from 'node:test'
import assert from 'node:assert/strict'
import { z } from 'zod'
import { LoginSchema } from '../src/funcionalidades/auth/tipos'
import { RegistroSchema } from '../src/funcionalidades/auth/registro'
import { destinoSeguro } from '../src/funcionalidades/auth/navegacion'
import { enlacesPorRol } from '../src/funcionalidades/auth/enlaces'
import {
  guardarSesion,
  leerSesionGuardada,
  eliminarSesionGuardada,
} from '../src/funcionalidades/auth/sesion'
import {
  http,
  ApiError,
  fijarToken,
  registrarAlExpirar,
} from '../src/lib/http'
import { iniciarSesion, registrarUsuario } from '../src/funcionalidades/auth/api'
import { mostrarErrorFormulario } from '../src/funcionalidades/auth/erroresFormulario'

const fetchOriginal = globalThis.fetch
let memoria: Map<string, string>
const usuario = {
  username: 'alejandro', email: 'alejandro@example.test',
  firstName: 'Alejandro', lastName: 'Pinto',
  phone: '', city: '', birthDate: '2000-04-12',
  password: 'una-clave-de-prueba',
}

beforeEach(() => {
  memoria = new Map()
  Object.defineProperty(globalThis, 'sessionStorage', {
    configurable: true,
    value: {
      getItem: (clave: string) => memoria.get(clave) ?? null,
      setItem: (clave: string, valor: string) => memoria.set(clave, valor),
      removeItem: (clave: string) => memoria.delete(clave),
    },
  })
})
afterEach(() => {
  globalThis.fetch = fetchOriginal
  fijarToken(null)
  registrarAlExpirar(null)
})

test('login recorta el identificador, pero conserva la contraseña exacta', () => {
  const datos = LoginSchema.parse({ identifier: '  alejandro ', password: ' clave ' })
  assert.equal(datos.identifier, 'alejandro')
  assert.equal(datos.password, ' clave ')
})

test('el registro acepta datos válidos y no añade roles al cuerpo', () => {
  const datos = RegistroSchema.parse({ ...usuario, roles: ['ADMIN'] })
  assert.ok(!('roles' in datos))
})

test('el registro rechaza contraseñas cortas y espacios', () => {
  for (const password of ['corta', ' '.repeat(12)]) {
    assert.equal(RegistroSchema.safeParse({ ...usuario, password }).success, false)
  }
})

test('el límite BCrypt se comprueba en bytes UTF-8', () => {
  assert.equal(RegistroSchema.safeParse({
    ...usuario, password: 'á'.repeat(36),
  }).success, true)
  assert.equal(RegistroSchema.safeParse({
    ...usuario, password: 'á'.repeat(37),
  }).success, false)
})

test('el registro rechaza fechas futuras e inexistentes', () => {
  for (const birthDate of ['2999-01-01', '2025-02-30', '']) {
    assert.equal(RegistroSchema.safeParse({ ...usuario, birthDate }).success, false)
  }
})

test('el registro rechaza usuarios con espacios o arroba', () => {
  for (const username of ['ale jandro', 'ale@jandro']) {
    assert.equal(RegistroSchema.safeParse({ ...usuario, username }).success, false)
  }
})

test('restaurar no reinicia la fecha de vencimiento', () => {
  const guardada = guardarSesion('token-de-prueba', 900)
  assert.deepEqual(leerSesionGuardada(), guardada)
  assert.deepEqual(leerSesionGuardada(), guardada)
})

test('datos corruptos y sesiones vencidas se eliminan', () => {
  for (const contenido of [
    '{incorrecto',
    JSON.stringify({ accessToken: 'prueba', expiraEn: 1 }),
    JSON.stringify({ accessToken: '', expiraEn: Date.now() + 900_000 }),
  ]) {
    memoria.set('pulsepass.sesion', contenido)
    assert.equal(leerSesionGuardada(), null)
    assert.equal(memoria.size, 0)
  }
})

test('cerrar elimina la sesión persistida', () => {
  guardarSesion('token-de-prueba', 900)
  eliminarSesionGuardada()
  assert.equal(leerSesionGuardada(), null)
})

test('el almacenamiento bloqueado no impide mantener sesión en memoria', () => {
  Object.defineProperty(globalThis, 'sessionStorage', {
    configurable: true,
    get() { throw new Error('Almacenamiento bloqueado') },
  })
  assert.equal(guardarSesion('prueba', 900).accessToken, 'prueba')
  assert.equal(leerSesionGuardada(), null)
  assert.doesNotThrow(eliminarSesionGuardada)
})

test('el retorno permite rutas internas y conserva consulta y fragmento', () => {
  assert.equal(destinoSeguro('/cuenta?origen=menu#perfil'), '/cuenta?origen=menu#perfil')
})

test('el retorno rechaza destinos externos y bucles de autenticación', () => {
  for (const valor of [
    '//externo.test', 'https://externo.test', '/\\externo.test',
    '/login', '/registro/', '/%6Cogin', null, '/%incorrecto',
  ]) {
    assert.equal(destinoSeguro(valor), '/')
  }
})

test('los enlaces distinguen USER, STAFF y ADMIN', () => {
  assert.ok(enlacesPorRol(['USER']).some(e => e.a === '/mis-tickets'))
  assert.ok(!enlacesPorRol(['USER']).some(e => e.a === '/admin/eventos'))
  assert.ok(enlacesPorRol(['STAFF']).some(e => e.a === '/staff/validacion'))
  assert.ok(!enlacesPorRol(['STAFF']).some(e => e.a === '/admin/eventos'))
  assert.ok(enlacesPorRol(['ADMIN']).some(e => e.a === '/admin/eventos'))
})

test('el login no envía Bearer y un 401 no expira otra sesión', async () => {
  fijarToken('token-anterior')
  let expiraciones = 0
  registrarAlExpirar(() => { expiraciones += 1 })
  globalThis.fetch = async (url, opciones) => {
    assert.equal(url, '/api/auth/login')
    assert.equal(new Headers(opciones?.headers).get('Authorization'), null)
    return new Response('{}', { status: 401 })
  }
  await assert.rejects(
    iniciarSesion({ identifier: 'alejandro', password: 'incorrecta' }),
    ApiError,
  )
  assert.equal(expiraciones, 0)
})

test('un 401 con el token actual avisa de expiración', async () => {
  fijarToken('token-actual')
  let expiraciones = 0
  registrarAlExpirar(() => { expiraciones += 1 })
  globalThis.fetch = async (_url, opciones) => {
    assert.equal(new Headers(opciones?.headers).get('Authorization'), 'Bearer token-actual')
    return new Response('{}', { status: 401 })
  }
  await assert.rejects(http('/auth/me', z.unknown()), ApiError)
  assert.equal(expiraciones, 1)
})

test('un 401 antiguo no cierra una sesión nueva', async () => {
  fijarToken('anterior')
  let resolver!: (respuesta: Response) => void
  let expiraciones = 0
  registrarAlExpirar(() => { expiraciones += 1 })
  globalThis.fetch = () => new Promise(resolve => { resolver = resolve })
  const pendiente = http('/auth/me', z.unknown())
  fijarToken('nuevo')
  resolver(new Response('{}', { status: 401 }))
  await assert.rejects(pendiente, ApiError)
  assert.equal(expiraciones, 0)
})

test('el registro usa POST /users y no envía roles ni Bearer', async () => {
  fijarToken('anterior')
  globalThis.fetch = async (url, opciones) => {
    assert.equal(url, '/api/users')
    assert.equal(opciones?.method, 'POST')
    assert.equal(new Headers(opciones?.headers).get('Authorization'), null)
    const cuerpo = JSON.parse(String(opciones?.body))
    assert.equal(cuerpo.email, usuario.email)
    assert.ok(!('roles' in cuerpo))
    return Response.json({ ...usuario, id: 5, active: true }, { status: 201 })
  }
  const respuesta = await registrarUsuario(usuario)
  assert.equal(respuesta.id, 5)
  assert.ok(!('password' in respuesta))
})

test('las respuestas inválidas y los errores de red se controlan', async () => {
  globalThis.fetch = async () => Response.json({ incorrecto: true })
  await assert.rejects(
    http('/auth/me', z.object({ id: z.number() })),
    (error: unknown) => error instanceof ApiError && error.status === 500,
  )
  globalThis.fetch = async () => { throw new Error('Desconectado') }
  await assert.rejects(
    http('/auth/me', z.unknown()),
    (error: unknown) => error instanceof ApiError && error.status === 0,
  )
})

test('los duplicados no revelan si existe el usuario o el correo', () => {
  const mensajes: string[] = []
  mostrarErrorFormulario(
    new ApiError(409, 'Email already exists'),
    (_campo, error) => mensajes.push(String(error.message)),
    ['email'],
    'registro',
  )
  assert.deepEqual(mensajes, ['El usuario o correo ya está registrado.'])
})

test('los detalles de campo se muestran en español', () => {
  const mensajes: string[] = []
  mostrarErrorFormulario(
    new ApiError(400, 'Revisa los datos.', { passwordWithinBcryptLimit: 'Too long' }),
    (_campo, error) => mensajes.push(String(error.message)),
    ['password'],
    'registro',
  )
  assert.equal(mensajes[0], 'Revisa este dato; el servidor no lo aceptó.')
})
