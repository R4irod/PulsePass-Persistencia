package com.pulsepass.service;

import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.SecurityPrincipal;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Compara la identidad autenticada con el dueño del recurso, siempre por ID persistente.
 * Nunca confía en un ID o email enviado por el cliente como identidad (SEC006).
 * Se invoca desde @PreAuthorize("@authorizationService....").
 */
@Service
public class AuthorizationService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public AuthorizationService(UserRepository userRepository, TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    // ---------------------------------------------------------------- identidad

    public Optional<SecurityPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof SecurityPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty(); // anónimo u otro tipo de principal
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isStaffOrAdmin() {
        return hasRole("STAFF") || hasRole("ADMIN");
    }

    // ---------------------------------------------------------------- reglas

    /** FR-SECTKT-01: el email de la compra debe pertenecer al principal. Sin excepción para ADMIN. */
    public boolean canPurchaseFor(String userEmail) {
        return emailBelongsToPrincipal(userEmail);
    }

    /** FR-SECTKT-02: dueño, o STAFF/ADMIN. */
    public boolean canViewTicket(String ticketCode) {
        return isStaffOrAdmin() || ownsTicket(ticketCode);
    }

    /** FR-SECTKT-05: dueño, o ADMIN. STAFF no cancela tickets ajenos. */
    public boolean canCancelTicket(String ticketCode) {
        return isAdmin() || ownsTicket(ticketCode);
    }

    /** FR-SECTKT-03: dueño del email, o ADMIN. */
    public boolean canListTicketsOf(String email) {
        return isAdmin() || emailBelongsToPrincipal(email);
    }

    /** FR-SECUSR-02 */
    public boolean canReadUserByEmail(String email) {
        return isAdmin() || emailBelongsToPrincipal(email);
    }

    /** FR-SECUSR-03 */
    public boolean canReadUserByUsername(String username) {
        return isAdmin() || usernameBelongsToPrincipal(username);
    }

    // ---------------------------------------------------------------- internos

    private boolean hasRole(String role) {
        String authority = ROLE_PREFIX + role;
        return currentPrincipal()
                .map(principal -> principal.getAuthorities().stream()
                        .anyMatch(granted -> authority.equals(granted.getAuthority())))
                .orElse(false);
    }

    private boolean ownsTicket(String ticketCode) {
        if (ticketCode == null || ticketCode.isBlank()) {
            return false;
        }
        return currentPrincipal()
                .flatMap(principal -> ticketRepository.findOwnerIdByTicketCode(ticketCode)
                        .map(ownerId -> ownerId.equals(principal.getId())))
                .orElse(false); // ticket inexistente = mismo resultado que ajeno
    }

    private boolean emailBelongsToPrincipal(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return currentPrincipal()
                .flatMap(principal -> userRepository.findByEmailIgnoreCase(email.trim())
                        .map(user -> user.getId().equals(principal.getId())))
                .orElse(false);
    }

    private boolean usernameBelongsToPrincipal(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return currentPrincipal()
                .flatMap(principal -> userRepository.findByUsername(username.trim())
                        .map(user -> user.getId().equals(principal.getId())))
                .orElse(false);
    }
}