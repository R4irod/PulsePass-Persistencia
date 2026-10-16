INTEGRANTES: MATEO CAMPUZANO 2024214063 - ALEJANDRO PINTO 2024214077

# PulsePass — Capa de Controladores REST

PulsePass es una plataforma de eventos y venta de entradas (conciertos, festivales, conferencias, deportes). El proyecto se construyó por fases: **Persistencia** (entidades, Flyway, repositories) → **Servicios** (reglas de negocio, DTOs, MapStruct, excepciones) → **Controladores** (esta fase).

Esta fase agrega únicamente la frontera HTTP: cinco controllers REST delgados, validación de entrada con Bean Validation, un `GlobalExceptionHandler` que traduce excepciones de dominio a respuestas HTTP uniformes, y pruebas de contrato con `@WebMvcTest` + `MockMvc`. No se modificó el modelo, ni los repositories, ni la lógica de los Services.

```text
HTTP Request
     ↓
Controller        (traduce HTTP ↔ Service; sin reglas de negocio)
     ↓
Service           (reglas de negocio — fase 2, sin cambios)
     ↓
Repository / PostgreSQL

Service Response / Exception
     ↓
Controller / GlobalExceptionHandler
     ↓
HTTP Response
```

## Stack

- Java 21
- Spring Boot 4.1 (Spring MVC)
- Bean Validation (`spring-boot-starter-validation`)
- Spring Data JPA / Hibernate, Flyway, PostgreSQL (fases anteriores)
- MapStruct (fase 2)
- JUnit 5 + Mockito + AssertJ + MockMvc (`@WebMvcTest`, `@MockitoBean`)

Dependencias agregadas al `pom.xml` en esta fase:

| Dependencia | Para qué |
|---|---|
| `spring-boot-starter-webmvc` | Spring MVC, `@RestController`, Jackson |
| `spring-boot-starter-validation` | `@Valid`, `@NotBlank`, `@Email`, `@Min`, `@Size` |
| `spring-boot-starter-webmvc-test` (test) | `@WebMvcTest`, `MockMvc` |

## Endpoints

20 operaciones, una por cada método público de los 5 Services.

### Venues — `VenueController`

| Método | Endpoint | Service | Éxito |
|---|---|---|---|
| GET | `/api/venues/{code}` | `findByCode` | 200 |
| GET | `/api/venues/active` | `findActiveVenues` | 200 |

### Eventos — `EventController`

| Método | Endpoint | Service | Éxito |
|---|---|---|---|
| POST | `/api/events` | `create` | **201** |
| GET | `/api/events/{eventCode}` | `findByCode` | 200 |
| GET | `/api/events/published` | `findPublishedEvents` | 200 |
| PATCH | `/api/events/{eventCode}/publish` | `publish` | 200 |
| POST | `/api/events/{eventCode}/artists/{artistId}` | `addArtist` | 200 |
| GET | `/api/events/by-artist?stageName=...` | `findByArtist` | 200 |

### Artistas — `ArtistController`

| Método | Endpoint | Service | Éxito |
|---|---|---|---|
| GET | `/api/artists/{id}` | `findById` | 200 |
| GET | `/api/artists/by-stage-name?stageName=...` | `findByStageName` | 200 |
| GET | `/api/artists/active` | `findActiveArtists` | 200 |

### Usuarios — `UserController`

| Método | Endpoint | Service | Éxito |
|---|---|---|---|
| POST | `/api/users` | `register` | **201** |
| GET | `/api/users/by-email?email=...` | `findByEmail` | 200 |
| GET | `/api/users/by-username?username=...` | `findByUsername` | 200 |

### Tickets — `TicketController`

| Método | Endpoint | Service | Éxito |
|---|---|---|---|
| POST | `/api/tickets` | `purchase` | **201** |
| GET | `/api/tickets/{ticketCode}` | `findByCode` | 200 |
| GET | `/api/tickets/by-user?email=...` | `findByUserEmail` | 200 |
| GET | `/api/events/{eventCode}/tickets/paid` | `findPaidTicketsByEvent` | 200 |
| PATCH | `/api/tickets/{ticketCode}/cancel` | `cancel` | 200 |
| PATCH | `/api/tickets/{ticketCode}/use` | `markAsUsed` | 200 |

### Ejemplos con `curl` (escenario de referencia: CMF-2026)

```bash
# Crear el evento (nace en DRAFT)
curl -i -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{"eventCode":"CMF-2026","name":"Caribbean Music Fest 2026","description":"Festival",
       "category":"MUSIC","eventDate":"2030-12-15T20:00:00","minimumAge":18,"venueCode":"VEN-SMR-01"}'

# Publicarlo y asociarle un artista
curl -i -X PATCH http://localhost:8080/api/events/CMF-2026/publish
curl -i -X POST  http://localhost:8080/api/events/CMF-2026/artists/1

# Registrar un usuario y comprar una entrada VIP (el precio lo calcula el Service, no el cliente)
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"andrea","email":"andrea@email.com","firstName":"Andrea","lastName":"Gomez",
       "phone":"3001234567","city":"Santa Marta","birthDate":"1999-05-20"}'

curl -i -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{"userEmail":"andrea@email.com","eventCode":"CMF-2026","type":"VIP"}'
```

## Validación de entrada (Bean Validation)

La validación **estructural** ocurre en el Controller (`@Valid @RequestBody`), antes de llegar al Service. Las reglas de negocio siguen siendo exclusivas del Service.

| Request | Validaciones |
|---|---|
| `CreateEventRequest` | `eventCode`, `name`, `venueCode` `@NotBlank`; `category`, `eventDate` `@NotNull`; `minimumAge` `@NotNull` + `@Min(0)`; `description` `@Size(max=2000)`; longitudes alineadas con las columnas de Flyway |
| `RegisterUserRequest` | `username`, `firstName`, `lastName` `@NotBlank`; `email` `@NotBlank` + `@Email`; `birthDate` `@NotNull`; `phone`/`city` con `@Size` |
| `PurchaseTicketRequest` | `userEmail` `@NotBlank` + `@Email`; `eventCode` `@NotBlank`; `type` `@NotNull` |

| Lo valida el Controller | Lo valida el Service |
|---|---|
| campo obligatorio, formato de email, longitud, valor mínimo, JSON mal formado | usuario activo, evento existente/publicado, edad mínima, capacidad, transiciones de estado, duplicados |

## Manejo de errores

Todos los errores usan el mismo contrato (`ErrorResponse`), producido por **un único** `GlobalExceptionHandler` con `@RestControllerAdvice`. Ningún controller tiene `try/catch`.

```json
{
  "timestamp": "2026-10-09T20:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Event not found: CMF-2026",
  "details": {}
}
```

En errores de validación, `details` mapea cada campo inválido a su mensaje:

```json
{
  "timestamp": "2026-10-09T20:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": {
    "eventCode": "Event code is required",
    "venueCode": "Venue code is required"
  }
}
```

| Excepción / condición | HTTP |
|---|---|
| `MethodArgumentNotValidException` (Bean Validation) | 400 |
| JSON mal formado o enum desconocido (`HttpMessageNotReadableException`) | 400 |
| Query param faltante / path variable con tipo inválido | 400 |
| `ResourceNotFoundException` | 404 |
| `DuplicateResourceException` | 409 |
| `BusinessRuleException` | 409 |
| Excepción inesperada | 500 (mensaje genérico; el detalle solo va al log, nunca al cliente) |

Además del mapeo del PRD, el handler cubre tres casos del propio Spring MVC para que no terminen disfrazados de 500 por el handler genérico: ruta inexistente (404), método HTTP no soportado (405) y `Content-Type` no soportado (415). Todos devuelven `ErrorResponse`.

## Decisiones de diseño

- **`GET /api/events/{eventCode}/tickets/paid` vive en `TicketController`**, no en `EventController`: invoca a `TicketService`, y así cada controller depende únicamente del Service que necesita. Por eso `TicketController` usa el prefijo `/api` y declara el segmento completo en cada método.
- **Rutas literales vs. variables** (`/active`, `/published`, `/by-artist`… contra `/{code}`, `/{id}`): Spring siempre prefiere la coincidencia literal, así que no hay ambigüedad.
- **Nombres explícitos** en `@PathVariable("x")` / `@RequestParam("x")`: no dependen de que el compilador conserve los nombres de parámetros.
- **El cliente nunca envía precio ni entidades**: solo identificadores de negocio (`venueCode`, `eventCode`, `userEmail`), tal como pide el PRD.
- **`ResponseEntity` en todos los endpoints**, con 201 para creaciones y 200 para consultas/actualizaciones.

## Ejecutar la aplicación

Requiere PostgreSQL (por defecto `jdbc:postgresql://localhost:5432/pulsepass`, usuario/clave `postgres`; configurable con `DB_URL`, `DB_USER`, `DB_PASSWORD`).

```bash
mvn spring-boot:run
```

La API queda en `http://localhost:8080`.

## Ejecutar las pruebas

```bash
mvn clean test
```

Hay tres tipos de prueba conviviendo:

| Tipo | Clases | Necesita |
|---|---|---|
| Contrato HTTP (esta fase) | `controller/*ControllerTest` — `@WebMvcTest` + `MockMvc` + `@MockitoBean` | nada (ni PostgreSQL ni Docker) |
| Unit tests de Service | `service/*ServiceImplTest` — Mockito | nada |
| Integración de persistencia | `PersistenceIntegrationTest` — Testcontainers | Docker |

Solo las pruebas de controllers:

```bash
mvn -Dtest='*ControllerTest' test
```

Los tests de controller **no** levantan el contexto completo: `@WebMvcTest` carga solo la capa web (controller + `GlobalExceptionHandler`) y los Services son mocks. Verifican status HTTP, `Content-Type`, campos JSON con `jsonPath`, la interacción con el Service (`verify`) y que un request inválido **nunca** llega al Service (`verify(..., never())`).

### Trazabilidad de pruebas

| Controller | Tests del PRD | Cubiertos en |
|---|---|---|
| Venue | VEN-001 .. 003 | `VenueControllerTest` (+ prueba de 500 sin filtrar detalles internos) |
| Event | EVT-001 .. 009 | `EventControllerTest` (+ edad mínima negativa, JSON mal formado, conflicto al crear, artista duplicado, `artistId` no numérico, param faltante) |
| Artist | ART-001 .. 004 | `ArtistControllerTest` (+ `stageName` inexistente, id no numérico) |
| User | USR-001 .. 005 | `UserControllerTest` (+ campos faltantes, email duplicado, email inexistente) |
| Ticket | TKT-001 .. 011 | `TicketControllerTest` (+ email inválido, tipo de ticket desconocido, ticket inexistente) |

## Estructura

```text
src/main/java/com/pulsepass
├── controller/                 ← NUEVO
│   ├── VenueController.java
│   ├── EventController.java
│   ├── ArtistController.java
│   ├── UserController.java
│   └── TicketController.java
├── dto/
│   ├── request/                (+ anotaciones Bean Validation)
│   └── response/               (+ ErrorResponse)
├── exception/                  (+ GlobalExceptionHandler)
├── service/ ...                (sin cambios)
├── repository/ ...             (sin cambios)
└── domain/ ...                 (sin cambios)

src/test/java/com/pulsepass
├── controller/                 ← NUEVO
│   ├── VenueControllerTest.java
│   ├── EventControllerTest.java
│   ├── ArtistControllerTest.java
│   ├── UserControllerTest.java
│   └── TicketControllerTest.java
├── service/ ...
└── PersistenceIntegrationTest.java
```

## Fuera de alcance

Autenticación/autorización (Spring Security, JWT), OpenAPI/Swagger, paginación, versionado de API (`/api/v1`), CORS avanzado, pruebas E2E y Testcontainers en pruebas de controller. Ningún endpoint está protegido: en este MVP todos son conceptualmente públicos.

## Fase 4 — Seguridad (Spring Security + JWT)

La API es **stateless**: cada request lleva `Authorization: Bearer <jwt>`. No hay sesiones ni cookies.
Las contraseñas se guardan con **BCrypt**; los tokens son JWT **HS256** de 15 minutos firmados con JJWT.

### Variables de entorno

| Variable | Obligatoria | Descripción |
|---|---|---|
| `PULSEPASS_JWT_SECRET_BASE64` | Sí | Clave HMAC en Base64 de **al menos 32 bytes**. La app no arranca sin ella. |
| `PULSEPASS_JWT_ISSUER` | No | Emisor del token (por defecto `pulsepass-api`). |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | No | Conexión a PostgreSQL. |

Generar una clave (PowerShell): `$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)`
Linux/Mac: `openssl rand -base64 32`. Ver `.env.example`. **Nunca** se versiona un secreto real.

### Flujo de autenticación

1. `POST /api/users` crea la cuenta (usuario + hash BCrypt + rol `USER`, en una sola transacción). Contraseña de 12 a 72 caracteres. El username no puede contener `@` ni espacios.
2. `POST /api/auth/login` con `{"identifier": "usuario o correo", "password": "..."}` devuelve `{"accessToken", "tokenType": "Bearer", "expiresIn": 900}`.
3. `GET /api/auth/me` devuelve `{id, username, email, roles}` del token enviado.

El token solo lleva `sub` (ID del usuario), `iss`, `iat`, `exp` y `jti`. Los **roles y el estado activo se leen de la base en cada request**: desactivar a un usuario o quitarle un rol surte efecto de inmediato, aunque su token siga vigente.

### Roles y permisos (las 20 operaciones)

| Operación | Anónimo | USER | STAFF | ADMIN |
|---|---|---|---|---|
| GET `/api/venues/{code}`, `/api/venues/active` | ✔ | ✔ | ✔ | ✔ |
| GET `/api/artists/{id}`, `/by-stage-name`, `/active` | ✔ | ✔ | ✔ | ✔ |
| GET `/api/events/published` | ✔ | ✔ | ✔ | ✔ |
| GET `/api/events/{eventCode}`, `/api/events/by-artist` | solo publicados | solo publicados | todos | todos |
| POST `/api/events` | 401 | 403 | 403 | ✔ |
| PATCH `/api/events/{eventCode}/publish` | 401 | 403 | 403 | ✔ |
| POST `/api/events/{eventCode}/artists/{artistId}` | 401 | 403 | 403 | ✔ |
| POST `/api/users` (rol inicial siempre `USER`) | ✔ | ✔ | ✔ | ✔ |
| GET `/api/users/by-email`, `/by-username` | 401 | solo los propios | solo los propios | cualquiera |
| POST `/api/tickets` | 401 | ✔ solo con su propio email | 403 (sin rol USER) | 403 (sin rol USER) |
| GET `/api/tickets/{ticketCode}` | 401 | solo propios | cualquiera | cualquiera |
| GET `/api/tickets/by-user?email=` | 401 | solo el propio | solo el propio | cualquiera |
| GET `/api/events/{eventCode}/tickets/paid` | 401 | 403 | ✔ | ✔ |
| PATCH `/api/tickets/{ticketCode}/cancel` | 401 | solo propios | solo propios | cualquiera |
| PATCH `/api/tickets/{ticketCode}/use` | 401 | 403 | ✔ | ✔ |

Política anti-IDOR: un `USER` recibe **403** tanto si el ticket (o usuario) es de otra persona como si no existe, para que no pueda descubrir qué códigos existen. Los eventos no publicados responden **404** al público.
La comparación de propiedad se hace siempre por **ID persistente**, nunca por un email o ID enviado por el cliente.

### Errores

Todos usan el mismo `ErrorResponse` (`timestamp, status, error, message, details`): 400 validación, 401 sin identidad válida (con `WWW-Authenticate: Bearer`), 403 sin permiso, 404, 409 regla de negocio, 500 sin trazas.

### Cuentas de prueba

El registro público **nunca** crea `STAFF` ni `ADMIN`. Para desarrollo: registrar las cuentas por la API y ejecutar `scripts/demo-seed.sql`, que asigna `laura` = STAFF y `miguel` = ADMIN y crea el evento `CMF-2026` en borrador.

### Verificación

```bash
./mvnw test                       # todo (requiere Docker para Testcontainers)
./mvnw test -Dtest="!*ControllerTest"
```

Pruebas de seguridad: `JwtServiceTest`, `JwtPropertiesTest`, `JwtAuthenticationFilterTest`, `AuthorizationServiceTest`, `AuthServiceTest`, `SecurityMatrixTest` (matriz ruta × rol con filtros reales), `OwnershipSecurityIntegrationTest` y `AuthServiceIntegrationTest` (PostgreSQL real).

### Límites conocidos (no implementado, deliberadamente)

- Sin refresh tokens, logout con revocación (`jti` denylist), MFA, recuperación de contraseña ni cambio de contraseña. El logout del cliente no invalida tokens ya emitidos: vencen a los 15 minutos.
- Sin límite de intentos de login ni bloqueo contra fuerza bruta (en producción: rate limiting, monitoreo y alertas).
- Clave HMAC simétrica sin rotación; en producción usar un gestor de secretos, identificadores de clave y rotación.
- STAFF es **global**: puede ver y marcar como usada cualquier entrada, porque el dominio no modela asignación de personal a eventos (futura entidad `EventStaffAssignment`).
- Los usuarios anteriores a la fase 4 no tienen credenciales y no pueden iniciar sesión hasta una activación segura; no se asignan contraseñas por defecto.
- Solo `PUBLISHED` es público: `SOLD_OUT`, `CANCELLED` y `FINISHED` se tratan como no publicados.
- Trazabilidad: se registran logins correctos y fallidos, tokens rechazados y accesos denegados, sin credenciales. Falta un *request ID* de correlación.
- HTTPS es obligatorio fuera de localhost; CORS no está configurado.