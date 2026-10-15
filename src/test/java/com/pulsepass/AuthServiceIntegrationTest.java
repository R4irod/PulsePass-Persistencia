package com.pulsepass;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.InvalidCredentialsException;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.security.JwtService;
import com.pulsepass.service.AuthService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
class AuthServiceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("pulsepass_test")
                    .withUsername("pulsepass")
                    .withPassword("pulsepass");

    private static final String RAW = "UnaFraseSeguraLarga2026!";

    @Autowired private AuthService authService;
    @Autowired private JwtService jwtService;
    @Autowired private UserCredentialRepository credentialRepository;
    @Autowired private UserRoleRepository roleRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    // Nombres únicos: el contenedor se comparte entre los tests de esta clase.
    private static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    private static RegisterUserRequest request(String username) {
        return new RegisterUserRequest(
                username, username + "@test.com", "Andrea", "Gomez",
                "3001234567", "Santa Marta", LocalDate.of(1998, 5, 10), RAW);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }

    @Test
    void registerPersistsUserHashedCredentialAndOnlyUserRole() {
        UserResponse created = authService.register(request(unique("andrea")));

        UserCredential credential = credentialRepository.findById(created.id()).orElseThrow();
        assertThat(credential.getPasswordHash()).startsWith("$2").isNotEqualTo(RAW);

        assertThat(roleRepository.findByIdUserId(created.id()))
                .extracting(UserRole::getRole)
                .containsExactly(Role.USER);
    }

    @Test
    void registeredUserCanLoginByUsernameOrEmailAndTokenCarriesUserId() {
        String username = unique("login");
        UserResponse created = authService.register(request(username));

        AuthResponse byUsername = authService.login(new LoginRequest(username, RAW));
        AuthResponse byEmail = authService.login(new LoginRequest(username + "@test.com", RAW));

        assertThat(byUsername.tokenType()).isEqualTo("Bearer");
        assertThat(byUsername.expiresIn()).isEqualTo(900L);
        assertThat(jwtService.validateAndExtractUserId(byUsername.accessToken())).isEqualTo(created.id());
        assertThat(jwtService.validateAndExtractUserId(byEmail.accessToken())).isEqualTo(created.id());
    }

    @Test
    void wrongPasswordIsRejected() {
        String username = unique("wrong");
        authService.register(request(username));

        assertThatThrownBy(() -> authService.login(new LoginRequest(username, "OtraFraseLarga2026!")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void unknownUserIsRejectedWithTheSameException() {
        assertThatThrownBy(() -> authService.login(new LoginRequest(unique("nadie"), RAW)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void inactiveUserCannotLoginEvenWithCorrectPassword() {
        String username = unique("inactivo");
        authService.register(request(username));
        jdbcTemplate.update("update users set active = false where username = ?", username);

        assertThatThrownBy(() -> authService.login(new LoginRequest(username, RAW)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void legacyUserWithoutCredentialCannotLogin() {
        String username = unique("legado");
        jdbcTemplate.update(
                "insert into users (username, email, active) values (?, ?, true)",
                username, username + "@test.com");

        assertThatThrownBy(() -> authService.login(new LoginRequest(username, RAW)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectedRegistrationLeavesNoExtraCredentialsOrRoles() {
        String username = unique("dup");
        authService.register(request(username));
        int credentials = count("user_credentials");
        int roles = count("user_roles");

        assertThatThrownBy(() -> authService.register(request(username)))
                .isInstanceOf(DuplicateResourceException.class);

        assertThat(count("user_credentials")).isEqualTo(credentials);
        assertThat(count("user_roles")).isEqualTo(roles);
    }
}