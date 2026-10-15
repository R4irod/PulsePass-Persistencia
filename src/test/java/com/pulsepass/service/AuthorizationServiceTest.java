package com.pulsepass.service;

import com.pulsepass.domain.User;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.SecurityPrincipal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TicketRepository ticketRepository;

    @InjectMocks private AuthorizationService service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long id, String... roles) {
        SecurityPrincipal principal = new SecurityPrincipal(id, "u" + id, "u" + id + "@test.com", null, true,
                Stream.of(roles).map(SimpleGrantedAuthority::new).toList());
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    private User user(Long id) {
        User user = new User("u" + id, "u" + id + "@test.com");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    // ---------- sin identidad ----------

    @Test
    void noAuthenticationMeansNoPermissionAndNoDatabaseAccess() {
        assertThat(service.isAdmin()).isFalse();
        assertThat(service.isStaffOrAdmin()).isFalse();
        assertThat(service.canViewTicket("TCK-1")).isFalse();
        assertThat(service.canPurchaseFor("u1@test.com")).isFalse();
        verifyNoInteractions(userRepository, ticketRepository);
    }

    @Test
    void anonymousTokenIsNotAnIdentity() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(service.currentPrincipal()).isEmpty();
        assertThat(service.canViewTicket("TCK-1")).isFalse();
    }

    // ---------- roles ----------

    @Test
    void rolesAreRecognizedFromAuthorities() {
        authenticateAs(1L, "ROLE_USER");
        assertThat(service.isStaffOrAdmin()).isFalse();
        assertThat(service.isAdmin()).isFalse();

        authenticateAs(2L, "ROLE_STAFF");
        assertThat(service.isStaffOrAdmin()).isTrue();
        assertThat(service.isAdmin()).isFalse();

        authenticateAs(3L, "ROLE_ADMIN");
        assertThat(service.isStaffOrAdmin()).isTrue();
        assertThat(service.isAdmin()).isTrue();
    }

    // ---------- tickets ----------

    @Test
    void ownerCanViewAndCancelOwnTicket() {
        authenticateAs(1L, "ROLE_USER");
        when(ticketRepository.findOwnerIdByTicketCode("TCK-1")).thenReturn(Optional.of(1L));

        assertThat(service.canViewTicket("TCK-1")).isTrue();
        assertThat(service.canCancelTicket("TCK-1")).isTrue();
    }

    @Test
    void userCannotViewOrCancelSomeoneElsesTicket() {
        authenticateAs(1L, "ROLE_USER");
        when(ticketRepository.findOwnerIdByTicketCode("TCK-2")).thenReturn(Optional.of(2L));

        assertThat(service.canViewTicket("TCK-2")).isFalse();
        assertThat(service.canCancelTicket("TCK-2")).isFalse();
    }

    @Test
    void nonexistentTicketIsDeniedForUserSameAsForeignOne() {
        authenticateAs(1L, "ROLE_USER");
        when(ticketRepository.findOwnerIdByTicketCode("TCK-NOPE")).thenReturn(Optional.empty());

        assertThat(service.canViewTicket("TCK-NOPE")).isFalse();
    }

    @Test
    void staffCanViewAnyTicketButCannotCancelSomeoneElses() {
        authenticateAs(5L, "ROLE_STAFF");

        assertThat(service.canViewTicket("TCK-2")).isTrue();
        verifyNoInteractions(ticketRepository); // no hace falta consultar al dueño

        when(ticketRepository.findOwnerIdByTicketCode("TCK-2")).thenReturn(Optional.of(2L));
        assertThat(service.canCancelTicket("TCK-2")).isFalse();
    }

    @Test
    void adminCanViewAndCancelAnyTicketWithoutOwnerLookup() {
        authenticateAs(9L, "ROLE_ADMIN");

        assertThat(service.canViewTicket("TCK-2")).isTrue();
        assertThat(service.canCancelTicket("TCK-2")).isTrue();
        verifyNoInteractions(ticketRepository);
    }

    // ---------- compra ----------

    @Test
    void userCanPurchaseOnlyForOwnEmail() {
        authenticateAs(1L, "ROLE_USER");
        when(userRepository.findByEmailIgnoreCase("u1@test.com")).thenReturn(Optional.of(user(1L)));
        when(userRepository.findByEmailIgnoreCase("u2@test.com")).thenReturn(Optional.of(user(2L)));
        when(userRepository.findByEmailIgnoreCase("nadie@test.com")).thenReturn(Optional.empty());

        assertThat(service.canPurchaseFor("u1@test.com")).isTrue();
        assertThat(service.canPurchaseFor("u2@test.com")).isFalse();
        assertThat(service.canPurchaseFor("nadie@test.com")).isFalse();
    }

    @Test
    void adminHasNoExceptionToBuyForSomeoneElse() {
        authenticateAs(9L, "ROLE_ADMIN");
        when(userRepository.findByEmailIgnoreCase("u2@test.com")).thenReturn(Optional.of(user(2L)));

        assertThat(service.canPurchaseFor("u2@test.com")).isFalse();
    }

    @Test
    void blankOrNullEmailIsDeniedWithoutQueries() {
        authenticateAs(1L, "ROLE_USER");

        assertThat(service.canPurchaseFor(null)).isFalse();
        assertThat(service.canPurchaseFor("  ")).isFalse();
        verifyNoInteractions(userRepository);
    }

    // ---------- usuarios y tickets por email ----------

    @Test
    void userCanReadOnlyOwnDataByEmailAndUsername() {
        authenticateAs(1L, "ROLE_USER");
        when(userRepository.findByEmailIgnoreCase("u1@test.com")).thenReturn(Optional.of(user(1L)));
        when(userRepository.findByEmailIgnoreCase("u2@test.com")).thenReturn(Optional.of(user(2L)));
        when(userRepository.findByUsername("u1")).thenReturn(Optional.of(user(1L)));
        when(userRepository.findByUsername("u2")).thenReturn(Optional.of(user(2L)));

        assertThat(service.canReadUserByEmail("u1@test.com")).isTrue();
        assertThat(service.canReadUserByEmail("u2@test.com")).isFalse();
        assertThat(service.canReadUserByUsername("u1")).isTrue();
        assertThat(service.canReadUserByUsername("u2")).isFalse();
        assertThat(service.canListTicketsOf("u1@test.com")).isTrue();
        assertThat(service.canListTicketsOf("u2@test.com")).isFalse();
    }

    @Test
    void adminCanReadOthersWithoutLookups() {
        authenticateAs(9L, "ROLE_ADMIN");

        assertThat(service.canReadUserByEmail("u2@test.com")).isTrue();
        assertThat(service.canReadUserByUsername("u2")).isTrue();
        assertThat(service.canListTicketsOf("u2@test.com")).isTrue();
        verifyNoInteractions(userRepository);
    }

    @Test
    void staffIsNotAllowedToReadOthersUserData() {
        authenticateAs(5L, "ROLE_STAFF");
        when(userRepository.findByEmailIgnoreCase("u2@test.com")).thenReturn(Optional.of(user(2L)));

        assertThat(service.canReadUserByEmail("u2@test.com")).isFalse();
    }
}