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
