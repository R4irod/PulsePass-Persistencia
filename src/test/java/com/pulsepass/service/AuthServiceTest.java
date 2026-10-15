package com.pulsepass.service;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.InvalidCredentialsException;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.SecurityPrincipal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String RAW = "UnaFraseSeguraLarga2026!";
    private static final String HASH = "$2a$10$hash.ficticio.solo.para.pruebas";

    @Mock private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private UserCredentialRepository credentialRepository;
    @Mock private UserRoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthService service;

    // ---------- helpers ----------

    private RegisterUserRequest registerRequest() {
        return new RegisterUserRequest(
                "andrea", "andrea@email.com", "Andrea", "Gomez",
                "3001234567", "Santa Marta", LocalDate.of(1998, 5, 10), RAW);
    }

    private UserResponse createdResponse() {
        return new UserResponse(7L, "andrea", "andrea@email.com", true,
                "Andrea", "Gomez", "3001234567", "Santa Marta", LocalDate.of(1998, 5, 10));
    }

    private SecurityPrincipal principal(String... roles) {
        return new SecurityPrincipal(7L, "andrea", "andrea@email.com", HASH, true,
                Stream.of(roles).map(SimpleGrantedAuthority::new).toList());
    }

    // ---------- registro ----------

    @Test
    void registerStoresHashedPasswordAndOnlyUserRole() {
        User user = new User("andrea", "andrea@email.com");
        ReflectionTestUtils.setField(user, "id", 7L);
        RegisterUserRequest request = registerRequest();

        when(userService.register(request)).thenReturn(createdResponse());
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(RAW)).thenReturn(HASH);

        UserResponse result = service.register(request);

        assertThat(result).isEqualTo(createdResponse());

        ArgumentCaptor<UserCredential> credential = ArgumentCaptor.forClass(UserCredential.class);
        verify(credentialRepository).save(credential.capture());
        assertThat(credential.getValue().getPasswordHash()).isEqualTo(HASH).isNotEqualTo(RAW);

        ArgumentCaptor<UserRole> role = ArgumentCaptor.forClass(UserRole.class);
        verify(roleRepository).save(role.capture());
        assertThat(role.getValue().getUserId()).isEqualTo(7L);
        assertThat(role.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void registerDoesNotTouchCredentialsWhenUserCreationFails() {
        RegisterUserRequest request = registerRequest();
        when(userService.register(request)).thenThrow(new DuplicateResourceException("Username already exists"));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verifyNoInteractions(credentialRepository, roleRepository, passwordEncoder);
    }

    // ---------- login ----------

    @Test
    void loginReturnsBearerTokenWithConfiguredTtl() {
        SecurityPrincipal principal = principal("ROLE_USER");
        Authentication authenticated =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(jwtService.generateAccessToken(7L)).thenReturn("jwt-ficticio");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);

        AuthResponse response = service.login(new LoginRequest("andrea", RAW));

        assertThat(response.accessToken()).isEqualTo("jwt-ficticio");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);

        ArgumentCaptor<Authentication> sent = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(sent.capture());
        assertThat(sent.getValue().getName()).isEqualTo("andrea");
        assertThat(sent.getValue().getCredentials()).isEqualTo(RAW);
    }

    static Stream<AuthenticationException> authenticationFailures() {
        return Stream.of(
                new BadCredentialsException("x"),
                new DisabledException("x"),
                new UsernameNotFoundException("x"));
    }

    @ParameterizedTest
    @MethodSource("authenticationFailures")
    void everyLoginFailureLooksTheSameAndNeverIssuesToken(AuthenticationException failure) {
        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.login(new LoginRequest("andrea", "clave-incorrecta")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");

        verifyNoInteractions(jwtService);
    }

    // ---------- me ----------

    @Test
    void meReturnsMinimalIdentityWithRolesWithoutPrefix() {
        AuthenticatedUserResponse response = service.me(principal("ROLE_USER", "ROLE_STAFF"));

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.username()).isEqualTo("andrea");
        assertThat(response.email()).isEqualTo("andrea@email.com");
        assertThat(response.roles()).containsExactlyInAnyOrder("USER", "STAFF");
    }
}