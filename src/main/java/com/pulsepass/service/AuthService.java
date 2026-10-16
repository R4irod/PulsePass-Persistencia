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
import com.pulsepass.exception.InvalidCredentialsException;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.SecurityPrincipal;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.springframework.web.server.ServerWebExchangeExtensionsKt.principal;

/**
 * Orquesta registro y login. No reemplaza a UserService: las reglas de negocio
 * del usuario (unicidad, edad) siguen viviendo allí.
 */
@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String ROLE_PREFIX = "ROLE_";

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    public AuthService(UserService userService,
                       UserRepository userRepository,
                       UserCredentialRepository credentialRepository,
                       UserRoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * FR-SECAUTH-001: usuario + credencial BCrypt + ROLE_USER en UNA transacción.
     * UserServiceImpl.register() se une a esta transacción (REQUIRED); si algo falla
     * después de crear el usuario, todo se revierte.
     * El rol inicial es siempre USER: el cliente no puede pedir otro (NFR-SEC009).
     */
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        UserResponse created = userService.register(request);

        User user = userRepository.findById(created.id())
                .orElseThrow(() -> new IllegalStateException("Usuario recién creado no encontrado"));

        credentialRepository.save(new UserCredential(user, passwordEncoder.encode(request.password())));
        roleRepository.save(new UserRole(user.getId(), Role.USER));

        return created;
    }

    /**
     * FR-SECAUTH-002/003/004: cualquier fallo se responde con la misma excepción.
     */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.identifier(), request.password()));
        } catch (AuthenticationException | IllegalArgumentException e) {
            log.warn("Login failed for identifier '{}'", sanitize(request.identifier()));
            throw new InvalidCredentialsException();
        }

        if (!(authentication.getPrincipal() instanceof SecurityPrincipal principal)) {
            throw new InvalidCredentialsException();
        }
        
        log.info("Login succeeded for userId={}", principal.getId());
        String token = jwtService.generateAccessToken(principal.getId());
        return new AuthResponse(token, TOKEN_TYPE, jwtService.getAccessTokenTtlSeconds());
    }

    /** FR-SECAUTH-010: información mínima del principal, sin secretos. */
    public AuthenticatedUserResponse me(SecurityPrincipal principal) {
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.startsWith(ROLE_PREFIX)
                        ? authority.substring(ROLE_PREFIX.length())
                        : authority)
                .toList();

        return new AuthenticatedUserResponse(
                principal.getId(), principal.getUsername(), principal.getEmail(), roles);
    }

    // Evita inyección de líneas falsas en los logs desde el identificador del login.
    private static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n\\t]", "_");
    }
}