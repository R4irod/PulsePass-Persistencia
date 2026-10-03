# PulsePass

Capa de persistencia para PulsePass, una plataforma de eventos y venta de entradas (conciertos, festivales, conferencias, deportes). Es un caso de estudio académico armado a partir de un PRD real: venues que albergan eventos, artistas que participan en esos eventos, usuarios que se registran y compran tickets.

El proyecto arrancó con solo la capa de persistencia (entidades, migraciones, repositories) y después se le sumó la capa de Service: cinco servicios (`Venue`, `Artist`, `User`, `Event`, `Ticket`), DTOs con `record`, mappers con MapStruct, tres excepciones de dominio, y todas las reglas de negocio del flujo de eventos y compra de entradas. Todavía no hay controllers, API REST, Spring Security ni frontend — eso queda fuera de alcance de este PRD.

## Stack

- Java 21
- Spring Boot 4.1
- Spring Data JPA / Hibernate
- Flyway
- PostgreSQL
- Testcontainers (para los tests de persistencia)
- MapStruct (para mapear Entity ↔ DTO)
- JUnit 5 + Mockito + AssertJ (para los tests de la capa Service)

## Modelo de dominio

Seis entidades:

- **Venue** — el lugar donde se hace un evento (código, nombre, ciudad, capacidad)
- **Event** — un evento concreto, asociado a un venue, con categoría y estado
- **Artist** — un artista que puede participar en varios eventos
- **User** — un usuario de la plataforma
- **UserProfile** — el perfil personal de ese usuario (nombre, teléfono, fecha de nacimiento)
- **Ticket** — una entrada comprada por un usuario para un evento

Relaciones:

```text
Venue   1 --- N  Event      (Event tiene venue_id)
Event   N --- M  Artist     (tabla intermedia event_artists, PK compuesta)
User    1 --- 1  UserProfile (UserProfile tiene user_id UNIQUE)
User    1 --- N  Ticket     (Ticket tiene user_id)
Event   1 --- N  Ticket     (Ticket tiene event_id)
```

Sobre por qué `Ticket` es una entidad propia y no un simple `@ManyToMany` entre `User` y `Event`: porque un ticket tiene datos que le pertenecen solo a él — código, tipo (GENERAL/VIP/BACKSTAGE/STUDENT), precio, estado (RESERVED/PAID/CANCELLED/USED) y fecha de compra. Un `@ManyToMany` puro solo puede representar que "existe una asociación" entre dos entidades, no puede cargar atributos propios de esa asociación de forma limpia. En cambio `event_artists` sí es un `@ManyToMany` real, porque ahí la única información que importa es "este artista toca en este evento" — no hay ningún dato adicional que colgarle a esa asociación.

## Cómo correrlo

Necesitás Java 21, el wrapper de Maven (`mvnw` / `mvnw.cmd`, ya incluido) y Docker Desktop corriendo.

```bash
mvnw.cmd clean install
```

Si querés levantar la app apuntando a un Postgres propio (no es necesario para los tests):

```text
DB_URL=jdbc:postgresql://localhost:5432/pulsepass
DB_USER=postgres
DB_PASSWORD=postgres
```

## Cómo correr los tests

```bash
mvnw.cmd clean test
```

No hace falta Postgres instalado en la máquina. Testcontainers baja la imagen `postgres:18-alpine`, levanta un container, Flyway corre las tres migraciones ahí adentro, se ejecutan los tests, y el container se destruye al terminar. Solo se necesita Docker Desktop activo.

## Migraciones (Flyway)

- **V1** — crea las 7 tablas (`venues`, `events`, `artists`, `event_artists`, `users`, `user_profiles`, `tickets`) con sus PK, FK, `UNIQUE`, `CHECK` e índices.
- **V2** — inserta el catálogo inicial de 5 artistas de prueba (Solar Beat, Neon Waves, Caribbean Sound, Ocean Drive, Digital Pulse).
- **V3** — agrega `streaming_url` (nullable) a `events`, sin tocar V1. Mismo criterio que en cualquier proyecto real: una vez que una migración corrió en un ambiente compartido, no se edita — se agrega una nueva.

`ddl-auto` está en `validate`, no en `update` ni `create`. Hibernate arranca, compara las entidades contra lo que Flyway ya construyó, y si algo no calza, la aplicación ni levanta. No hay ninguna magia de autocreación de esquema de por medio.

## Testcontainers

```java
@Container
@ServiceConnection
static final PostgreSQLContainer postgres =
        new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("pulsepass_test")
            .withUsername("pulsepass")
            .withPassword("pulsepass");
```

El PRD es explícito en esto (NFR-004 y NFR-005): nada de H2, nada de depender de una instancia de Postgres instalada a mano. Las constraints reales de PostgreSQL (los `CHECK` de capacidad y precio, el `UNIQUE` de los códigos de negocio, las FK) se prueban contra el motor real, no contra una simulación que podría comportarse distinto.

## Query Methods

- `VenueRepository.findByCode(String code)`
- `ArtistRepository.findByStageName(String stageName)`
- `UserRepository.findByUsername(String username)`
- `UserRepository.findByEmailIgnoreCase(String email)`
- `EventRepository.findByEventCode(String eventCode)`
- `EventRepository.findByStatusOrderByEventDateAsc(EventStatus status)`
- `EventRepository.findByVenueCode(String code)`
- `TicketRepository.findByUserEmail(String email)`
- `TicketRepository.findByUserEmailAndStatus(String email, TicketStatus status)`
- `TicketRepository.findByEventEventCodeAndStatus(String eventCode, TicketStatus status)`
- `TicketRepository.findByEventEventDateAfterOrderByEventEventDateAsc(LocalDateTime date)`

Sumados para la capa Service:

- `VenueRepository.findByActiveTrueOrderByNameAsc()`
- `ArtistRepository.findByStageNameIgnoreCase(String stageName)`
- `ArtistRepository.findByActiveTrueOrderByStageNameAsc()`
- `EventRepository.existsByEventCode(String eventCode)`
- `UserRepository.existsByEmailIgnoreCase(String email)`
- `UserRepository.existsByUsername(String username)`
- `TicketRepository.findByTicketCode(String ticketCode)`
- `TicketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(String email)`

## Consultas JPQL (`@Query`)

**EventRepository** — eventos por artista (evita duplicados con `DISTINCT`, necesario porque un evento con varios artistas generaría filas repetidas sin él):

```java
@Query("""
    select distinct e
    from Event e
    join e.artists a
    where a.stageName = :stageName
    """)
List<Event> findByArtistStageName(@Param("stageName") String stageName);
```

Eventos por ciudad y artista (cruza `Event → Venue` y `Event → Artist`):

```java
@Query("""
    select distinct e
    from Event e
    join e.artists a
    where e.venue.city = :city
    and a.stageName = :stageName
    """)
List<Event> findByVenueCityAndArtistStageName(@Param("city") String city, @Param("stageName") String stageName);
```

Eventos recomendados — la consulta más cargada del proyecto: filtra por status fijo, fecha posterior, ciudad, y texto parcial del artista sin importar mayúsculas:

```java
@Query("""
    select distinct e
    from Event e
    join e.artists a
    where e.status = 'PUBLISHED'
    and e.eventDate > :afterDate
    and e.venue.city = :city
    and lower(a.stageName) like lower(concat('%', :artistText, '%'))
    order by e.eventDate asc
    """)
List<Event> findRecommended(@Param("afterDate") LocalDateTime afterDate, @Param("city") String city, @Param("artistText") String artistText);
```

**TicketRepository** — conteo de tickets pagados de un evento:

```java
@Query("""
    select count(t)
    from Ticket t
    where t.event.eventCode = :eventCode
    and t.status = :status
    """)
long countByEventEventCodeAndStatus(@Param("eventCode") String eventCode, @Param("status") TicketStatus status);
```

## Por qué JPQL en unas y Query Method en otras

El criterio que usé fue: si el filtro es directo (una o dos condiciones sobre campos simples o una sola relación), Query Method. Si hay que cruzar una relación N:M, combinar varias asociaciones a la vez, o controlar explícitamente el `DISTINCT`, JPQL. `findByEventEventCodeAndStatus`, por ejemplo, técnicamente cruza una relación (`Ticket → Event`), pero sigue siendo un filtro de dos condiciones exactas sin riesgo de duplicados, así que ahí Query Method sigue siendo la opción más simple y legible. En cambio `findByArtistStageName` necesita `DISTINCT` sí o sí porque atraviesa el N:M, y eso Spring Data no te lo deja controlar en un nombre de método derivado.

## La capa Service

Arriba de los repositories se sumó una capa Service, que orquesta repositories, aplica reglas de negocio, controla transacciones, y transforma entidades a DTOs. Ningún repository sabe nada de reglas de negocio — eso vive acá.

Estructura nueva dentro de `com.pulsepass`:

```text
dto/
├── request/      → CreateEventRequest, RegisterUserRequest, PurchaseTicketRequest
└── response/     → VenueResponse, ArtistResponse, UserResponse, EventResponse, EventSummaryResponse, TicketResponse
mapper/           → VenueMapper, ArtistMapper, UserMapper, EventMapper, TicketMapper (MapStruct)
exception/        → ResourceNotFoundException, BusinessRuleException, DuplicateResourceException
service/          → VenueService, ArtistService, UserService, EventService, TicketService
service/impl/     → las 5 implementaciones (@Service) + TicketPricingPolicy
```

### Los 5 servicios

- **VenueService** — consultar venue por código, listar venues activos.
- **ArtistService** — buscar por id o por nombre artístico, listar artistas activos.
- **UserService** — registrar usuario (crea `User` + `UserProfile` en la misma transacción), buscar por email o username.
- **EventService** — crear evento, publicarlo, asociarle artistas, consultar por código/artista/estado publicado.
- **TicketService** — comprar, cancelar, marcar como usado, consultar por código/usuario/evento. El más denso de los cinco: encadena validación de usuario, evento, edad mínima, capacidad, cálculo de precio y actualización de `SOLD_OUT`, todo en una sola transacción atómica.

### `EventMapper` y el mapeo de listas

La mayoría de los mappers son directos (`VenueMapper`, `ArtistMapper` no necesitan ningún `@Mapping` explícito porque los nombres de campo coinciden). `UserMapper` y `TicketMapper` navegan relaciones simples (`profile.firstName`, `user.email`, `event.eventCode`) con `@Mapping(target = ..., source = ...)`.

`EventMapper` es el único que necesita algo más: el DTO expone `artists` como `List<String>` (solo los nombres), pero la entidad tiene `Set<Artist>` (los objetos completos). Para esos casos, MapStruct permite declarar un método `default` dentro de la misma interfaz que enseña la conversión, y lo detecta solo por los tipos de entrada/salida:

```java
default List<String> mapArtists(Set<Artist> artists) {
    return artists.stream().map(Artist::getStageName).sorted().toList();
}
```

### Las tres excepciones

- **`ResourceNotFoundException`** — el recurso no existe (un `eventCode` que no está en la base).
- **`BusinessRuleException`** — el recurso existe, pero la operación no es válida en ese estado (un venue inactivo, un evento no publicado, un usuario que no cumple la edad mínima).
- **`DuplicateResourceException`** — conflicto de unicidad (un `username` o `eventCode` que ya existe). Es la única que no estaba en el laboratorio de persistencia de DeepBlue — se agregó porque acá sí hay operaciones de creación con códigos únicos de negocio (`register`, `create`), mientras que el laboratorio anterior solo tenía transiciones de estado sobre recursos ya existentes.

### Reglas de negocio por servicio

**`EventService.create`** encadena seis validaciones antes de guardar: `eventCode` único, venue existente, venue activo, fecha futura, edad mínima no negativa — recién ahí construye el `Event`, siempre en `DRAFT` (el request nunca controla el estado inicial).

**`EventService.publish`** valida que el evento esté en `DRAFT`, que la fecha siga siendo futura, y que el venue siga activo, antes de pasar a `PUBLISHED`.

**`EventService.addArtist`** valida que el evento no esté `CANCELLED` ni `FINISHED`, y que el artista no esté ya asociado (sin eso, se podría duplicar la misma asociación N:M).

**`TicketService.purchase`** es el flujo más largo del proyecto, siguiendo el diagrama del PRD al pie de la letra: usuario existe → usuario activo → evento existe → evento `PUBLISHED` → fecha futura → cumple edad mínima (calculada con `Period.between` sobre `UserProfile.birthDate` y la fecha del evento) → hay capacidad disponible → calcular precio → crear ticket → guardar → si se agotó el cupo, pasar el evento a `SOLD_OUT`. Cualquier paso que falle corta el flujo antes de `save(...)`.

**`TicketService.cancel`** solo permite `PAID → CANCELLED`, y no después de la fecha del evento. **`TicketService.markAsUsed`** solo permite `PAID → USED`. Ambos comparten la misma condición técnica (`status != PAID`) pero se mantienen como dos chequeos separados porque son reglas de negocio conceptualmente distintas — si el día de mañana una cambia sin la otra, no conviene que estén acopladas.

### `TicketPricingPolicy`: el precio, encapsulado y aparte

El PRD pide explícitamente que la estrategia de precio esté "encapsulada y probada", y que el precio nunca se confíe directamente desde el cliente. Por eso el cálculo vive en una clase propia, separada de `TicketServiceImpl`:

```java
static BigDecimal calculatePrice(TicketType type) {
    return switch (type) {
        case GENERAL -> BASE_PRICE;
        case STUDENT -> BASE_PRICE.multiply(STUDENT_DISCOUNT);
        case VIP -> BASE_PRICE.multiply(VIP_MULTIPLIER);
        case BACKSTAGE -> BASE_PRICE.multiply(BACKSTAGE_MULTIPLIER);
    };
}
```

Es una función pura (`static`, sin estado, sin dependencias) — mismo `TicketType` entra, mismo `BigDecimal` sale, siempre. Eso la hace trivial de testear de forma aislada, sin mocks, y de paso garantiza por diseño que el precio nunca sea negativo (todos los multiplicadores son positivos), sin necesitar un `if` explícito para esa regla.

### Transacciones

Todas las clases `*ServiceImpl` llevan `@Transactional(readOnly = true)` a nivel de clase. Los métodos que escriben (`register`, `create`, `publish`, `addArtist`, `purchase`, `cancel`, `markAsUsed`) lo pisan con `@Transactional` sin `readOnly`. La compra de tickets es el caso donde más importa: todo el flujo —validaciones, cálculo de precio, creación del ticket, actualización de `SOLD_OUT`— corre en una sola transacción, así que si cualquier paso falla, Spring hace rollback de todo. Nunca queda un ticket guardado a medias ni un evento mal marcado como agotado.

### Inyección por constructor

Todos los `ServiceImpl` reciben sus dependencias (repositories, mappers) por constructor, con campos `private final`. `EventServiceImpl`, por ejemplo, necesita tres repositories a la vez (`EventRepository`, `VenueRepository`, `ArtistRepository`) porque tiene que coordinar tres entidades distintas en sus distintas operaciones.

## Tests de la capa Service (unitarios, con Mockito)

Completamente separados de `PersistenceIntegrationTest`: acá no hay Postgres, no hay Testcontainers, no hay `@SpringBootTest`. Cada repository y cada mapper se reemplaza por un mock:

```java
@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {
    @Mock private UserRepository userRepository;
    @Mock private EventRepository eventRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private TicketMapper mapper;
    @InjectMocks private TicketServiceImpl service;
}
```

Cada test sigue Arrange-Act-Assert: se configura qué devuelve cada mock con `when(...).thenReturn(...)`, se llama al método del service, y se verifica con AssertJ. En los tests de reglas de negocio violadas, lo más importante no es solo que se lance la excepción correcta — es confirmar que nunca se llegó a persistir nada:

```java
verify(ticketRepository, never()).save(any());
```

El test del escenario "último ticket disponible → `SOLD_OUT`" usa además `ArgumentCaptor<Event>`, para capturar el objeto exacto que se pasó a `eventRepository.save(...)` e inspeccionar su estado (`getStatus()`) después de la llamada — necesario porque ahí no alcanza con verificar que se guardó algo, hay que confirmar *cómo* quedó.

Tests cubiertos: `UserServiceImplTest` (4), `EventServiceImplTest` (8), `TicketServiceImplTest` (12) — los 24 mínimos que pide el PRD, más los de `VenueService` y `ArtistService` que no tenían mínimo explícito pero se armaron igual por consistencia.

Corren con:

```bash
mvnw.cmd clean test
```

y no necesitan Docker corriendo — a diferencia de `PersistenceIntegrationTest`, que sí lo necesita.
