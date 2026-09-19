INTEGRANTES: MATEO CAMPUZANO 2024214063 - ALEJANDRO PINTO 2024214077

# PulsePass

Capa de persistencia para PulsePass, una plataforma de eventos y venta de entradas (conciertos, festivales, conferencias, deportes). Es un caso de estudio académico armado a partir de un PRD real: venues que albergan eventos, artistas que participan en esos eventos, usuarios que se registran y compran tickets.

Igual que en el laboratorio anterior, acá solo está la capa de datos: entidades JPA, migraciones Flyway, repositories con sus consultas, y pruebas de integración contra Postgres real con Testcontainers. No hay controllers, API REST, service ni frontend — el PRD lo deja explícito como fuera de alcance.

## Stack

- Java 21
- Spring Boot 4.1
- Spring Data JPA / Hibernate
- Flyway
- PostgreSQL
- Testcontainers

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
