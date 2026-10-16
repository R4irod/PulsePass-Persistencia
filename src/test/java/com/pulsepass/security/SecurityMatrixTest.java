package com.pulsepass.security;

import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.exception.InvalidCredentialsException;
import com.pulsepass.service.ArtistService;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.EventService;
import com.pulsepass.service.TicketService;
import com.pulsepass.service.UserService;
import com.pulsepass.service.VenueService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.security.access.AccessDeniedException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@EnableConfigurationProperties(JwtProperties.class)
@Import({SecurityConfig.class, JwtService.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class SecurityMatrixTest {

    private static final Long USER_ID = 1L;
    private static final Long STAFF_ID = 2L;
    private static final Long ADMIN_ID = 3L;
    private static final Long INACTIVE_ID = 4L;
    private static final Long DELETED_ID = 99L;

    // Misma clave ficticia de src/test/resources/config/application.yml
    private static final String TEST_KEY_BASE64 = "dGVzdC1vbmx5LWtleS1kby1ub3QtdXNlLWluLXByb2Q=";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    @MockitoBean private CustomUserDetailsService userDetailsService;
    @MockitoBean private VenueService venueService;
    @MockitoBean private EventService eventService;
    @MockitoBean private ArtistService artistService;
    @MockitoBean private UserService userService;
    @MockitoBean private TicketService ticketService;
    @MockitoBean private AuthService authService;

    @BeforeEach
    void setUp() {
        when(userDetailsService.loadUserById(USER_ID)).thenReturn(principal(USER_ID, true, "ROLE_USER"));
        when(userDetailsService.loadUserById(STAFF_ID)).thenReturn(principal(STAFF_ID, true, "ROLE_STAFF"));
        when(userDetailsService.loadUserById(ADMIN_ID)).thenReturn(principal(ADMIN_ID, true, "ROLE_ADMIN"));
        when(userDetailsService.loadUserById(INACTIVE_ID)).thenReturn(principal(INACTIVE_ID, false, "ROLE_USER"));
        when(userDetailsService.loadUserById(DELETED_ID)).thenThrow(new UsernameNotFoundException("x"));
    }

    // ---------- helpers ----------

    private static SecurityPrincipal principal(Long id, boolean enabled, String... roles) {
        return new SecurityPrincipal(id, "u" + id, "u" + id + "@test.com", null, enabled,
                Stream.of(roles).map(SimpleGrantedAuthority::new).toList());
    }

    private String bearer(Long userId) {
        return "Bearer " + jwtService.generateAccessToken(userId);
    }

    private int statusOf(String method, String path, String authorization) throws Exception {
        var builder = request(HttpMethod.valueOf(method), path);
        if (authorization != null) {
            builder.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return mockMvc.perform(builder).andReturn().getResponse().getStatus();
    }

    private void check(String method, String path, String authorization, boolean allowed) throws Exception {
        int status = statusOf(method, path, authorization);
        String who = authorization == null ? "anónimo" : "autenticado";

        if (allowed) {
            assertThat(status).as("%s %s (%s) debería estar permitido", method, path, who).isNotIn(401, 403);
        } else {
            assertThat(status).as("%s %s (%s) debería estar denegado", method, path, who)
                    .isEqualTo(authorization == null ? 401 : 403);
        }
    }

    // ---------- matriz: método, ruta, anónimo, USER, STAFF, ADMIN ----------

    static Stream<Arguments> matrix() {
        return Stream.of(
                // Públicas
                Arguments.of("GET", "/api/venues/VEN-SMR-01", true, true, true, true),
                Arguments.of("GET", "/api/venues/active", true, true, true, true),
                Arguments.of("GET", "/api/events/published", true, true, true, true),
                Arguments.of("GET", "/api/artists/1", true, true, true, true),
                Arguments.of("GET", "/api/artists/by-stage-name?stageName=SolarBeat", true, true, true, true),
                Arguments.of("GET", "/api/artists/active", true, true, true, true),
                Arguments.of("POST", "/api/users", true, true, true, true),
                Arguments.of("POST", "/api/auth/login", true, true, true, true),

                // Cualquier usuario autenticado (la propiedad se afina en el Paso 7)
                Arguments.of("GET", "/api/events/CMF-2026", true, true, true, true),
                Arguments.of("GET", "/api/events/by-artist?stageName=SolarBeat", true, true, true, true),
                Arguments.of("GET", "/api/users/by-email?email=a@test.com", false, true, true, true),
                Arguments.of("GET", "/api/users/by-username?username=andrea", false, true, true, true),
                Arguments.of("GET", "/api/tickets/TKT-1", false, true, true, true),
                Arguments.of("GET", "/api/tickets/by-user?email=a@test.com", false, true, true, true),
                Arguments.of("PATCH", "/api/tickets/TKT-1/cancel", false, true, true, true),
                Arguments.of("GET", "/api/auth/me", false, true, true, true),

                // Por rol
                Arguments.of("POST", "/api/events", false, false, false, true),
                Arguments.of("PATCH", "/api/events/CMF-2026/publish", false, false, false, true),
                Arguments.of("POST", "/api/events/CMF-2026/artists/1", false, false, false, true),
                Arguments.of("GET", "/api/events/CMF-2026/tickets/paid", false, false, true, true),
                Arguments.of("PATCH", "/api/tickets/TKT-1/use", false, false, true, true),
                Arguments.of("POST", "/api/tickets", false, true, false, false)
        );
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("matrix")
    void permissionMatrixIsEnforced(String method, String path,
                                    boolean anonymous, boolean user, boolean staff, boolean admin) throws Exception {
        check(method, path, null, anonymous);
        check(method, path, bearer(USER_ID), user);
        check(method, path, bearer(STAFF_ID), staff);
        check(method, path, bearer(ADMIN_ID), admin);
    }

    // ---------- contrato de errores 401 / 403 ----------

    @Test
    void anonymousOnProtectedRouteGets401WithErrorResponseAndNoSession() throws Exception {
        mockMvc.perform(post("/api/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(cookie().doesNotExist("JSESSIONID"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.details").isMap());

        verifyNoInteractions(eventService);
    }

    @Test
    void userOnAdminRouteGets403WithErrorResponseAndServiceIsNotCalled() throws Exception {
        mockMvc.perform(post("/api/events")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.details").isMap());

        verifyNoInteractions(eventService);
    }

    @Test
    void anonymousCanReadPublishedEvents() throws Exception {
        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("JSESSIONID"));
    }
    
    @Test
    void methodSecurityDenialIsMappedTo403Json() throws Exception {
        when(ticketService.findByCode("TCK-AJENO")).thenThrow(new AccessDeniedException("denegado"));

        mockMvc.perform(get("/api/tickets/TCK-AJENO").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.details").isMap());
    }

    // ---------- tokens inválidos ----------

    private String expiredToken() {
        JwtProperties properties = new JwtProperties("pulsepass-api", TEST_KEY_BASE64, Duration.ofMinutes(15));
        Clock longAgo = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        return new JwtService(properties, longAgo).generateAccessToken(USER_ID);
    }

    @Test
    void expiredTokenGets401() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        verifyNoInteractions(userDetailsService);
    }

    @Test
    void tamperedSignatureGets401AndBusinessIsNotCalled() throws Exception {
        String[] parts = jwtService.generateAccessToken(ADMIN_ID).split("\\.");
        char first = parts[2].charAt(0);
        String tampered = parts[0] + "." + parts[1] + "." + (first == 'A' ? 'B' : 'A') + parts[2].substring(1);

        mockMvc.perform(post("/api/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(eventService, userDetailsService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer ", "Bearer not-a-jwt", "Bearer a.b.c"})
    void malformedBearerGets401NeverA500(String authorization) throws Exception {
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void invalidBearerIsRejectedEvenOnPublicRoutes() throws Exception {
        mockMvc.perform(get("/api/events/published").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInQueryStringIsIgnored() throws Exception {
        mockMvc.perform(get("/api/auth/me").queryParam("access_token", jwtService.generateAccessToken(USER_ID)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenOfInactiveUserGets401() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(INACTIVE_ID)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenOfDeletedUserGets401() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(DELETED_ID)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- los roles salen de la base, no del token ----------

    @Test
    void revokedRoleTakesEffectOnTheNextRequestWithTheSameToken() throws Exception {
        String token = bearer(ADMIN_ID);

        assertThat(statusOf("POST", "/api/events", token)).isNotIn(401, 403);

        // Se le retira ADMIN en la base de datos; el token es el mismo.
        when(userDetailsService.loadUserById(ADMIN_ID)).thenReturn(principal(ADMIN_ID, true, "ROLE_USER"));

        assertThat(statusOf("POST", "/api/events", token)).isEqualTo(403);
    }
    
        // T-SEC-005: login correcto -> 200 con Bearer (anónimo)
    @Test
    void loginReturnsBearerTokenResponse() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(new AuthResponse("jwt-ficticio", "Bearer", 900L));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"andrea\",\"password\":\"UnaFraseSeguraLarga2026!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    // T-SEC-019: un login fallido no filtra ni la contraseña ni trazas internas
    @Test
    void failedLoginReturns401WithoutLeakingDetails() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"andrea\",\"password\":\"clave-incorrecta-123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("clave-incorrecta-123").doesNotContain("trace").doesNotContain("Exception");
    }

    // T-SEC-018: autorizado pero con cuerpo inválido -> 400 de Bean Validation, el Service no se invoca
    @Test
    void invalidRequestFromAuthorizedUserGets400() throws Exception {
        mockMvc.perform(post("/api/events")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verifyNoInteractions(eventService);
    }
}