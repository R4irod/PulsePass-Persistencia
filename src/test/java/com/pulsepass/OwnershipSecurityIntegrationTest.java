package com.pulsepass;

import com.jayway.jsonpath.JsonPath;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.security.JwtService;
import com.pulsepass.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
class OwnershipSecurityIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("pulsepass_test")
                    .withUsername("pulsepass")
                    .withPassword("pulsepass");

    private static final String RAW = "UnaFraseSeguraLarga2026!";
    private static final String PUBLISHED_EVENT = "EVT-PUB-1";
    private static final String DRAFT_EVENT = "EVT-DRAFT-1";

    @Autowired private WebApplicationContext context;
    @Autowired private AuthService authService;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbcTemplate;

    // Datos compartidos: se crean una sola vez por clase.
    private static boolean initialized;
    private static String andreaEmail;
    private static String carlosEmail;
    private static String andreaToken;
    private static String carlosToken;
    private static String lauraToken;   // USER + STAFF
    private static String adminToken;   // USER + ADMIN
    private static String artistName;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        if (!initialized) {
            createFixtures();
            initialized = true;
        }
    }

    // ---------- fixtures ----------

    private UserResponse register(String username) {
        return authService.register(new RegisterUserRequest(
                username, username + "@test.com", "Nombre", "Apellido",
                "3001234567", "Santa Marta", LocalDate.of(1998, 5, 10), RAW));
    }

    private String bearerFor(Long userId) {
        return "Bearer " + jwtService.generateAccessToken(userId);
    }

    private void insertEvent(String code, String status, Long venueId) {
        jdbcTemplate.update(
                "insert into events (event_code, name, description, category, status, event_date, minimum_age, venue_id) "
                        + "values (?, ?, 'Test', 'MUSIC', ?, ?, 0, ?)",
                code, "Evento " + code, status, LocalDateTime.now().plusMonths(2), venueId);
    }

    private void createFixtures() {
        UserResponse andrea = register("andrea");
        UserResponse carlos = register("carlos");
        UserResponse laura = register("laura");
        UserResponse admina = register("admina");

        jdbcTemplate.update("insert into user_roles (user_id, role) values (?, 'STAFF')", laura.id());
        jdbcTemplate.update("insert into user_roles (user_id, role) values (?, 'ADMIN')", admina.id());

        andreaEmail = andrea.email();
        carlosEmail = carlos.email();
        andreaToken = bearerFor(andrea.id());
        carlosToken = bearerFor(carlos.id());
        lauraToken = bearerFor(laura.id());
        adminToken = bearerFor(admina.id());

        jdbcTemplate.update("insert into venues (code, name, city, address, capacity, active) "
                + "values ('VEN-TEST-01', 'Test Venue', 'Santa Marta', 'Cra 1 #2-3', 500, true)");
        Long venueId = jdbcTemplate.queryForObject(
                "select id from venues where code = 'VEN-TEST-01'", Long.class);
        insertEvent(PUBLISHED_EVENT, "PUBLISHED", venueId);
        insertEvent(DRAFT_EVENT, "DRAFT", venueId);

        artistName = jdbcTemplate.queryForObject(
                "select stage_name from artists order by id limit 1", String.class);
        jdbcTemplate.update(
                "insert into event_artists (event_id, artist_id) "
                        + "select e.id, a.id from events e, artists a "
                        + "where e.event_code in (?, ?) and a.stage_name = ?",
                PUBLISHED_EVENT, DRAFT_EVENT, artistName);
    }

    /** Compra un ticket del evento publicado y devuelve su código. */
    private String buyTicket(String token, String email) throws Exception {
        String body = "{\"userEmail\":\"" + email + "\",\"eventCode\":\"" + PUBLISHED_EVENT
                + "\",\"type\":\"GENERAL\"}";

        String response = mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(response, "$.ticketCode");
    }

    private int ticketCount() {
        return jdbcTemplate.queryForObject("select count(*) from tickets", Integer.class);
    }

    private String ticketStatus(String ticketCode) {
        return jdbcTemplate.queryForObject(
                "select status from tickets where ticket_code = ?", String.class, ticketCode);
    }

    // ---------- compra ----------

    @Test
    void userCanBuyForSelf() throws Exception {
        String code = buyTicket(andreaToken, andreaEmail);

        assertThat(code).startsWith("TCK-");
    }

    @Test
    void userCannotBuyOnBehalfOfAnotherUser() throws Exception {
        int before = ticketCount();
        String body = "{\"userEmail\":\"" + carlosEmail + "\",\"eventCode\":\"" + PUBLISHED_EVENT
                + "\",\"type\":\"GENERAL\"}";

        mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, andreaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        assertThat(ticketCount()).as("no debe crearse ningún ticket").isEqualTo(before);
    }

    // ---------- lectura de tickets ----------

    @Test
    void userCannotReadSomeoneElsesTicketButOwnerStaffAndAdminCan() throws Exception {
        String carlosTicket = buyTicket(carlosToken, carlosEmail);

        mockMvc.perform(get("/api/tickets/" + carlosTicket).header(HttpHeaders.AUTHORIZATION, andreaToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/tickets/" + carlosTicket).header(HttpHeaders.AUTHORIZATION, carlosToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value(carlosTicket));
        mockMvc.perform(get("/api/tickets/" + carlosTicket).header(HttpHeaders.AUTHORIZATION, lauraToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tickets/" + carlosTicket).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void nonexistentTicketLooksLikeAForeignOneForUsersButIs404ForStaff() throws Exception {
        mockMvc.perform(get("/api/tickets/TCK-NOEXISTE").header(HttpHeaders.AUTHORIZATION, andreaToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tickets/TCK-NOEXISTE").header(HttpHeaders.AUTHORIZATION, lauraToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotListSomeoneElsesTickets() throws Exception {
        buyTicket(carlosToken, carlosEmail);

        mockMvc.perform(get("/api/tickets/by-user").param("email", carlosEmail)
                        .header(HttpHeaders.AUTHORIZATION, andreaToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tickets/by-user").param("email", carlosEmail)
                        .header(HttpHeaders.AUTHORIZATION, carlosToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tickets/by-user").param("email", carlosEmail)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
    }

    // ---------- cancelación ----------

    @Test
    void onlyOwnerOrAdminCanCancelAndForeignAttemptsDoNotChangeState() throws Exception {
        String carlosTicket = buyTicket(carlosToken, carlosEmail);

        mockMvc.perform(patch("/api/tickets/" + carlosTicket + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, andreaToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/tickets/" + carlosTicket + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, lauraToken))   // STAFF no cancela ajenos
                .andExpect(status().isForbidden());
        assertThat(ticketStatus(carlosTicket)).isEqualTo("PAID");

        mockMvc.perform(patch("/api/tickets/" + carlosTicket + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, carlosToken))
                .andExpect(status().isOk());
        assertThat(ticketStatus(carlosTicket)).isEqualTo("CANCELLED");
    }

    @Test
    void adminCanCancelSomeoneElsesTicket() throws Exception {
        String carlosTicket = buyTicket(carlosToken, carlosEmail);

        mockMvc.perform(patch("/api/tickets/" + carlosTicket + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        assertThat(ticketStatus(carlosTicket)).isEqualTo("CANCELLED");
    }

    // ---------- uso en el ingreso ----------

    @Test
    void staffMarksTicketAsUsedOnceAndUserCannot() throws Exception {
        String carlosTicket = buyTicket(carlosToken, carlosEmail);

        mockMvc.perform(patch("/api/tickets/" + carlosTicket + "/use")
                        .header(HttpHeaders.AUTHORIZATION, carlosToken))   // ni el dueño
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/tickets/" + carlosTicket + "/use")
                        .header(HttpHeaders.AUTHORIZATION, lauraToken))
                .andExpect(status().isOk());
        assertThat(ticketStatus(carlosTicket)).isEqualTo("USED");

        mockMvc.perform(patch("/api/tickets/" + carlosTicket + "/use")
                        .header(HttpHeaders.AUTHORIZATION, lauraToken))   // regla de negocio heredada
                .andExpect(status().isConflict());
    }

    // ---------- datos de usuarios ----------

    @Test
    void userCannotLookUpOtherUsersButCanLookUpSelf() throws Exception {
        mockMvc.perform(get("/api/users/by-email").param("email", carlosEmail)
                        .header(HttpHeaders.AUTHORIZATION, andreaToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/by-username").param("username", "carlos")
                        .header(HttpHeaders.AUTHORIZATION, andreaToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/by-email").param("email", andreaEmail)
                        .header(HttpHeaders.AUTHORIZATION, andreaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("andrea"));
        mockMvc.perform(get("/api/users/by-username").param("username", "andrea")
                        .header(HttpHeaders.AUTHORIZATION, andreaToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/by-email").param("email", carlosEmail)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void lookingUpANonexistentUserIsDeniedForUsersLikeAForeignOne() throws Exception {
        mockMvc.perform(get("/api/users/by-email").param("email", "nadie@test.com")
                        .header(HttpHeaders.AUTHORIZATION, andreaToken))
                .andExpect(status().isForbidden());
    }

    // ---------- eventos no publicados ----------

    @Test
    void nonPublishedEventsAreHiddenFromEveryoneButStaffAndAdmin() throws Exception {
        mockMvc.perform(get("/api/events/" + DRAFT_EVENT))                                   // anónimo
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/events/" + DRAFT_EVENT).header(HttpHeaders.AUTHORIZATION, andreaToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/events/EVT-NOEXISTE"))                                      // mismo 404
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/events/" + DRAFT_EVENT).header(HttpHeaders.AUTHORIZATION, lauraToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/events/" + DRAFT_EVENT).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/events/" + PUBLISHED_EVENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value(PUBLISHED_EVENT));
    }

    @Test
    void searchByArtistHidesNonPublishedEventsFromThePublic() throws Exception {
        mockMvc.perform(get("/api/events/by-artist").param("stageName", artistName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.eventCode=='" + PUBLISHED_EVENT + "')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.eventCode=='" + DRAFT_EVENT + "')]").isEmpty());

        mockMvc.perform(get("/api/events/by-artist").param("stageName", artistName)
                        .header(HttpHeaders.AUTHORIZATION, lauraToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.eventCode=='" + DRAFT_EVENT + "')]").isNotEmpty());
    }
}