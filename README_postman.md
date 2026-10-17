INTEGRANTES: MATEO CAMPUZANO 2024214063 - ALEJANDRO PINTO 2024214077

# PulsePass — Fase 4: Capa de Seguridad (Spring Security + JWT)

PulsePass es una plataforma de eventos y venta de entradas (conciertos, festivales, conferencias, deportes). El proyecto se construyó por fases:

| Fase | Contenido |
|---|---|
| 1. Persistencia | Entidades JPA, PostgreSQL, Flyway, repositories |
| 2. Servicios | Reglas de negocio, DTOs (`record`), MapStruct, transacciones, excepciones |
| 3. Controladores REST | 20 operaciones HTTP, Bean Validation, `GlobalExceptionHandler`, `ErrorResponse` |
| **4. Seguridad (esta fase)** | Autenticación con contraseña (BCrypt), JWT firmado (JJWT), roles, propiedad de recursos y errores 401/403 uniformes |

La Fase 4 **no reemplaza** las anteriores: conserva las 20 rutas, los DTOs de respuesta y las reglas de negocio, y agrega un perímetro HTTP seguro con los cambios mínimos necesarios (contraseña en el registro, tablas de credenciales y roles, y control de propiedad de tickets y datos de usuario).

```text
Cliente HTTP
     ↓  Authorization: Bearer <jwt>
Spring Security Filter Chain
     ├─ JwtAuthenticationFilter   (valida el JWT y carga usuario + roles ACTUALES desde la base)
     ├─ authorizeHttpRequests     (reglas por ruta y rol)
     ├─ AuthenticationEntryPoint  → 401 ErrorResponse
     └─ AccessDeniedHandler       → 403 ErrorResponse
     ↓
Controller  (delgado, sin lógica de seguridad ni de negocio)
     ↓
Service     (reglas de negocio + @PreAuthorize de propiedad vía AuthorizationService)
     ↓
Repository / PostgreSQL
```

## Stack

- Java 21, Spring Boot 4.1, Spring MVC, Spring Security 7
- JJWT 0.13.0 (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) — JWT firmado con HS256
- Spring Data JPA / Hibernate, PostgreSQL, Flyway
- MapStruct, Bean Validation
- JUnit 5, Mockito, AssertJ, MockMvc, `spring-security-test`, Testcontainers

## Requisitos para la ejecucion

- JDK 21
- PostgreSQL con una base de datos creada (por ejemplo `pulsepass`)
- Docker (solo para las pruebas de integración con Testcontainers)
- Maven (se incluye el wrapper `mvnw` / `mvnw.cmd`)

## Variables de entorno

| Variable | Obligatoria | Descripción |
|---|---|---|
| `PULSEPASS_JWT_SECRET_BASE64` | **Sí** | Clave HMAC en Base64 de **al menos 32 bytes**. La aplicación **no arranca** si falta o es débil. |
| `PULSEPASS_JWT_ISSUER` | No | Emisor del token. Por defecto `pulsepass-api`. |
| `DB_URL` | No | Por defecto `jdbc:postgresql://localhost:5432/pulsepass` |
| `DB_USER` | No | Por defecto `postgres` |
| `DB_PASSWORD` | No | Por defecto `postgres` |

La duración del token de acceso es `PT15M` (15 minutos) y se define en `application.yml` (`pulsepass.security.jwt.access-token-ttl`).

Generar una clave segura:

```powershell
# PowerShell (Windows)
$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

```bash
# Linux / macOS
openssl rand -base64 32
```

El archivo `.env.example` es una plantilla **sin secretos**. **Nunca** se versiona una clave real (`.env` y `dev-env.ps1` están en `.gitignore`). Las pruebas usan una clave ficticia exclusiva en `src/test/resources/config/application.yml`.

## Ejecutar la aplicación

PowerShell (Windows):

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/pulsepass"
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "<tu contraseña>"
$env:PULSEPASS_JWT_SECRET_BASE64 = "<clave base64 de 32+ bytes>"

.\mvnw.cmd spring-boot:run
```

Linux / macOS:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pulsepass
export DB_USER=postgres
export DB_PASSWORD=<tu contraseña>
export PULSEPASS_JWT_SECRET_BASE64=$(openssl rand -base64 32)

./mvnw spring-boot:run
```

También se puede ejecutar desde el IDE definiendo las mismas variables en la configuración de ejecución. La API queda en `http://localhost:8080`. Al arrancar, Flyway aplica las migraciones `V1` a `V4` y Hibernate valida el esquema (`ddl-auto: validate`).

## Modelo de identidad y migración

La entidad `User` se conserva como entidad de negocio. La identidad de login se vincula a su **ID persistente**, no a un correo editable. La migración `V4__security_credentials_roles.sql` agrega:

```text
user_credentials (1:1 con users)      user_roles (N:1 con users)
 ├─ user_id  (PK y FK)                 ├─ user_id (FK)
 ├─ password_hash (NOT NULL, BCrypt)   └─ role    (USER | STAFF | ADMIN)  PK(user_id, role)
 └─ password_changed_at
```

- Las migraciones `V1`–`V3` no se modificaron.
- Los usuarios anteriores a la Fase 4 reciben el rol `USER` pero **no** tienen credencial: no pueden iniciar sesión hasta una activación segura. No se asignan contraseñas por defecto.
- El registro público **nunca** crea `STAFF` ni `ADMIN`.

## Flujo de autenticación

1. `POST /api/users` crea la cuenta: usuario + hash BCrypt + rol `USER`, en **una sola transacción** (`AuthService.register`). La contraseña debe tener entre 12 y 72 caracteres (y máximo 72 bytes). El username no puede contener `@` ni espacios. La respuesta **no** incluye contraseña ni hash.
2. `POST /api/auth/login` con `{"identifier": "usuario o correo", "password": "..."}` devuelve:
   ```json
   { "accessToken": "<jwt>", "tokenType": "Bearer", "expiresIn": 900 }
   ```
   Cualquier fallo (contraseña incorrecta, usuario inexistente, usuario inactivo, usuario sin credencial) devuelve el mismo **401 genérico**, sin revelar si el usuario existe.
3. `GET /api/auth/me` (con Bearer) devuelve `{id, username, email, roles}` del principal. Sin secretos.
4. Las demás rutas protegidas se llaman con `Authorization: Bearer <accessToken>`.

El registro **no** emite token: el flujo es **registro → login**.

### El JWT

| Claim | Contenido |
|---|---|
| `sub` | ID interno del usuario |
| `iss` | `pulsepass-api` (o el valor configurado) |
| `iat` | Fecha de emisión |
| `exp` | Vencimiento (15 minutos) |
| `jti` | Identificador aleatorio |

- El token **no** lleva roles ni contraseña. **Los roles y el estado activo se leen de la base de datos en cada request**: desactivar a un usuario o quitarle un rol surte efecto en el siguiente request, aunque su token siga vigente.
- `JwtService` verifica criptográficamente la firma antes de confiar en los claims, exige HS256 y rechaza `alg=none`, tokens sin firma, vencidos, con otro issuer, manipulados, sin `sub`/`exp`/`jti`, o con `iat` futuro.
- El token solo se acepta por el encabezado `Authorization`. Un JWT en la query string se ignora.
- La API es **stateless**: sin sesiones ni `JSESSIONID`. CSRF está deshabilitado porque no hay autenticación por cookies.

## Roles y permisos (las 20 operaciones)

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

### Política anti-IDOR (propiedad de recursos)

- Un `USER` recibe **403** tanto si el ticket (o usuario) pertenece a otra persona como si **no existe**, para que no pueda descubrir qué códigos existen.
- Los eventos no publicados responden **404** al público (no se revela que existen borradores).
- La propiedad se decide siempre comparando el **ID persistente** del principal contra el dueño leído de la base. Nunca se confía en un `userEmail` o `userId` enviado por el cliente como identidad.
- `POST /api/tickets`: el `userEmail` del cuerpo debe pertenecer al usuario autenticado. Sin excepción para `ADMIN`.
- Las comprobaciones viven en `AuthorizationService` y se aplican con `@PreAuthorize` sobre los Services, no solo en los Controllers.

## Cobertura de pruebas por operación

Para cada una de las 20 operaciones: rol permitido, rol prohibido, anónimo y, cuando existe propiedad, mismo propietario frente a un tercero.

| # | Operación | Permitido | Prohibido / anónimo | Propietario vs. tercero | Pruebas |
|---|---|---|---|---|---|
| 1 | GET venue por código | todos | — | n/a | `SecurityMatrixTest` (matriz) |
| 2 | GET venues activos | todos | — | n/a | `SecurityMatrixTest` (matriz) |
| 3 | POST evento | ADMIN | anónimo 401; USER 403 sin invocar el Service | n/a | `SecurityMatrixTest` (matriz, `userOnAdminRoute…`) |
| 4 | GET evento por código | todos si está publicado; STAFF/ADMIN todos | borrador → 404 para público | n/a | `OwnershipSecurityIntegrationTest.nonPublishedEventsAreHiddenFromEveryoneButStaffAndAdmin` |
| 5 | GET eventos publicados | todos | — | n/a | `SecurityMatrixTest.anonymousCanReadPublishedEvents` |
| 6 | PATCH publicar evento | ADMIN | anónimo 401; USER y STAFF 403 | n/a | `SecurityMatrixTest` (matriz) |
| 7 | POST artista a evento | ADMIN | anónimo 401; USER y STAFF 403 | n/a | `SecurityMatrixTest` (matriz) |
| 8 | GET eventos por artista | todos (público solo publicados) | — | n/a | `OwnershipSecurityIntegrationTest.searchByArtistHides…` |
| 9 | GET artista por id | todos | — | n/a | `SecurityMatrixTest` (matriz) |
| 10 | GET artista por nombre | todos | — | n/a | `SecurityMatrixTest` (matriz) |
| 11 | GET artistas activos | todos | — | n/a | `SecurityMatrixTest` (matriz) |
| 12 | POST registro de usuario | todos | contraseña corta/ausente → 400 | n/a | `AuthServiceTest`, `AuthServiceIntegrationTest`, `UserControllerTest` |
| 13 | GET usuario por email | propio; ADMIN cualquiera | anónimo 401; USER/STAFF ajeno 403 | propio sí, ajeno 403 | `AuthorizationServiceTest`, `OwnershipSecurityIntegrationTest.userCannotLookUpOtherUsers…` |
| 14 | GET usuario por username | propio; ADMIN cualquiera | anónimo 401; USER/STAFF ajeno 403 | propio sí, ajeno 403 | igual que la anterior |
| 15 | POST comprar ticket | USER con su propio email | anónimo 401; STAFF/ADMIN 403 | email ajeno 403 y no se crea ticket | `OwnershipSecurityIntegrationTest.userCanBuyForSelf`, `userCannotBuyOnBehalfOfAnotherUser`; `SecurityMatrixTest` |
| 16 | GET ticket por código | dueño; STAFF/ADMIN | anónimo 401 | USER ajeno o inexistente 403 | `OwnershipSecurityIntegrationTest.userCannotReadSomeoneElsesTicketButOwnerStaffAndAdminCan`, `nonexistentTicketLooksLikeAForeignOne…` |
| 17 | GET tickets por usuario | dueño del email; ADMIN | anónimo 401 | USER ajeno 403 | `OwnershipSecurityIntegrationTest.userCannotListSomeoneElsesTickets` |
| 18 | GET tickets pagados de un evento | STAFF, ADMIN | anónimo 401; USER 403 | n/a | `SecurityMatrixTest` (matriz) |
| 19 | PATCH cancelar ticket | dueño; ADMIN | anónimo 401 | USER o STAFF ajeno 403 y el estado no cambia | `OwnershipSecurityIntegrationTest.onlyOwnerOrAdminCanCancelAndForeignAttemptsDoNotChangeState`, `adminCanCancelSomeoneElsesTicket` |
| 20 | PATCH usar ticket | STAFF, ADMIN | anónimo 401; USER 403 | n/a (segundo uso → 409 por regla de negocio) | `OwnershipSecurityIntegrationTest.staffMarksTicketAsUsedOnceAndUserCannot`; `SecurityMatrixTest` |

Además: `JwtServiceTest` (firma, claims, expiración, issuer, manipulación, algoritmo), `JwtAuthenticationFilterTest` (sin Bearer, inválido, doble encabezado, usuario inactivo o borrado, no sobrescribe autenticación), `SecurityMatrixTest` (token vencido, firma alterada, Bearer malformado nunca da 500, JWT en query string ignorado, rol revocado efectivo en el siguiente request) y `CustomUserDetailsServiceTest` / `PasswordEncoderConfigTest` / `JwtPropertiesTest`.

## Errores

Todos los errores usan el mismo contrato `ErrorResponse` (`timestamp, status, error, message, details`). Los errores de seguridad se producen antes del `DispatcherServlet`, por lo que `RestAuthenticationEntryPoint` y `RestAccessDeniedHandler` escriben la misma estructura JSON.

| Caso | HTTP | Responsable |
|---|---|---|
| JSON/Bean Validation inválido | 400 | `GlobalExceptionHandler` |
| Sin Bearer en ruta protegida, JWT vencido/manipulado, login incorrecto | 401 (`WWW-Authenticate: Bearer`) | EntryPoint / `AuthService` |
| Usuario válido sin rol o sin propiedad | 403 | AccessDeniedHandler / `@PreAuthorize` |
| Recurso inexistente (o borrador para el público) | 404 | `GlobalExceptionHandler` |
| Duplicado o regla de negocio | 409 | `GlobalExceptionHandler` |
| Error inesperado | 500 sin stacktrace | `GlobalExceptionHandler` |

```json
{
  "timestamp": "2026-10-16T15:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "No tiene permisos para realizar esta operación",
  "details": {}
}
```

## Cuentas de prueba y datos de demostración

El registro público nunca crea `STAFF` ni `ADMIN`. Para desarrollo:

1. Registrar las cuentas por la API (`POST /api/users`): `andrea`, `carlos`, `laura`, `miguel`.
2. Ejecutar `scripts/demo-seed.sql` sobre la base de datos (pgAdmin o `psql`). Crea el venue `VEN-SMR-01`, el evento `CMF-2026` en borrador, y asigna `laura` = `STAFF` y `miguel` = `ADMIN`. Se puede ejecutar varias veces.

Contraseña usada en la demostración (solo desarrollo): `UnaFraseSegura2026!`.

## Demostración con Postman

La colección está en `docs/postman/PulsePass-Fase4.postman_collection.json` (Postman → *Import*). Variable de colección: `baseUrl` (por defecto `http://localhost:8080`). Los tokens y códigos de ticket se guardan solos.

| Orden | Carpeta | Qué hace |
|---|---|---|
| 1 | A. Registro | Crea los 4 usuarios; prueba duplicado (409) y contraseña corta (400) |
| 2 | *(SQL)* | Ejecutar `scripts/demo-seed.sql` |
| 3 | B. Login y `/me` | Obtiene los 4 tokens (`expiresIn = 900`), login incorrecto → 401, roles en `/me` |
| 4 | C. Escenario de demostración | 25 peticiones: rutas públicas, 401/403, propiedad de tickets, uso por STAFF, token alterado |
| 5 | *(SQL)* | `UPDATE users SET active = false WHERE username = 'carlos';` |
| 6 | D. Usuario desactivado | El token aún vigente de Carlos y su login dan 401 |

Los tokens duran 15 minutos: si pasa más tiempo entre B y C, ejecutar B otra vez. Con *Run folder* de Postman se obtiene el reporte de la ejecución.

Nota sobre el orden respecto al PRD: el evento debe estar **publicado** antes de comprar, por lo que en la demostración el ADMIN publica y asocia a Solar Beat antes de que Andrea compre.

## Ejecutar las pruebas

```bash
./mvnw test                          # todo (requiere Docker para Testcontainers)
./mvnw test -Dtest="!*IntegrationTest"   # solo unitarias y MockMvc, sin Docker
```

| Tipo | Clases | Necesita |
|---|---|---|
| Unitarias de seguridad | `JwtServiceTest`, `JwtPropertiesTest`, `JwtAuthenticationFilterTest`, `CustomUserDetailsServiceTest`, `PasswordEncoderConfigTest`, `AuthServiceTest`, `AuthorizationServiceTest` | nada |
| MockMvc con filtros reales | `SecurityMatrixTest` (matriz ruta × rol) y `controller/*ControllerTest` | nada |
| Unitarias de negocio (Fase 2) | `*ServiceImplTest` | nada |
| Integración con PostgreSQL real | `PersistenceIntegrationTest`, `SecurityPersistenceIntegrationTest`, `AuthServiceIntegrationTest`, `OwnershipSecurityIntegrationTest` | Docker |

Las pruebas **no** deshabilitan los filtros de seguridad (`addFilters = false`): validan el comportamiento real de la cadena. Los tests previos de negocio siguen vigentes.

## Estructura

```text
src/main/java/com/pulsepass
├── controller/        Venue, Event, Artist, User, Ticket + AuthController (nuevo)
├── service/           Services existentes + AuthService, AuthorizationService (nuevos)
├── security/          SecurityConfig, JwtProperties, JwtService, JwtAuthenticationFilter,
│                      CustomUserDetailsService, SecurityPrincipal, PasswordEncoderConfig,
│                      AuthenticationManagerConfig, RestAuthenticationEntryPoint,
│                      RestAccessDeniedHandler, SecurityErrorWriter, InvalidJwtException
├── domain/            Entidades existentes + UserCredential, UserRole, Role
├── repository/        Repositories existentes + UserCredentialRepository, UserRoleRepository
├── dto/               request/ (+ LoginRequest) y response/ (+ AuthResponse, AuthenticatedUserResponse)
├── exception/         GlobalExceptionHandler + InvalidCredentialsException
└── mapper/            MapStruct (sin cambios)

src/main/resources/db/migration/   V1..V3 (previas) + V4__security_credentials_roles.sql
src/test/java/com/pulsepass/       security/, controller/, service/ y pruebas de integración
scripts/demo-seed.sql              Datos de demostración (venue, evento en borrador, roles)
docs/postman/                      Colección de Postman de la demostración
```

## Decisiones de diseño

- **Autenticación, autorización y negocio separados.** `SecurityConfig` solo declara filtro y rutas; `JwtService` solo emite y valida tokens; `AuthorizationService` compara identidad con propietario; los Services conservan las reglas de negocio.
- **Denegar por defecto.** Solo son públicas las rutas explícitas; el resto cae en `anyRequest().authenticated()`.
- **Roles y estado revalidados en cada request**, en lugar de confiar en claims del token.
- **403 uniforme para recursos ajenos o inexistentes** (anti-enumeración) y 404 para borradores ante el público.
- **Credenciales separadas del usuario** (`user_credentials`) para que el hash nunca pueda serializarse por accidente desde los mappers.
- **Logs sin secretos.** Se registran logins correctos y fallidos, tokens rechazados y accesos denegados, sin contraseñas ni tokens.

## Límites conocidos (no implementado, deliberadamente)

- Sin refresh tokens, logout con revocación (`jti` denylist), MFA, recuperación ni cambio de contraseña. El logout del cliente no invalida tokens ya emitidos: vencen a los 15 minutos.
- Sin límite de intentos de login ni bloqueo contra fuerza bruta (en producción: rate limiting, monitoreo y alertas).
- Clave HMAC simétrica sin rotación; en producción usar un gestor de secretos, identificador de clave y rotación.
- `STAFF` es **global**: puede ver y marcar como usada cualquier entrada, porque el dominio no modela asignación de personal a eventos. Debe restringirse en producción (futura entidad `EventStaffAssignment`).
- Los usuarios anteriores a la Fase 4 no pueden iniciar sesión hasta una activación segura.
- Solo `PUBLISHED` es público: `SOLD_OUT`, `CANCELLED` y `FINISHED` se tratan como no publicados.
- Falta un *request ID* de correlación en los logs.
- HTTPS es obligatorio fuera de localhost; CORS no está configurado.
