package com.pulsepass.security;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    private static final String HASH = "$2a$10$hash.ficticio.solo.para.pruebas";

    @Mock private UserRepository userRepository;
    @Mock private UserCredentialRepository credentialRepository;
    @Mock private UserRoleRepository roleRepository;

    @InjectMocks private CustomUserDetailsService service;

    private User user(Long id, String username, String email, boolean active) {
        User user = new User(username, email);
        ReflectionTestUtils.setField(user, "id", id);
        user.setActive(active);
        return user;
    }

    private List<String> authorityNames(SecurityPrincipal principal) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    @Test
    void loadsPrincipalByUsername() {
        User andrea = user(1L, "andrea", "andrea@test.com", true);
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(andrea));
        when(credentialRepository.findById(1L)).thenReturn(Optional.of(new UserCredential(andrea, HASH)));
        when(roleRepository.findByIdUserId(1L)).thenReturn(List.of(new UserRole(1L, Role.USER)));

        SecurityPrincipal principal = service.loadUserByUsername("andrea");

        assertThat(principal.getId()).isEqualTo(1L);
        assertThat(principal.getUsername()).isEqualTo("andrea");
        assertThat(principal.getPassword()).isEqualTo(HASH);
        assertThat(principal.isEnabled()).isTrue();
        assertThat(authorityNames(principal)).containsExactly("ROLE_USER");
    }

    @Test
    void loadsPrincipalByEmailWhenIdentifierContainsAt() {
        User andrea = user(1L, "andrea", "andrea@test.com", true);
        when(userRepository.findByEmailIgnoreCase("andrea@test.com")).thenReturn(Optional.of(andrea));
        when(credentialRepository.findById(1L)).thenReturn(Optional.of(new UserCredential(andrea, HASH)));
        when(roleRepository.findByIdUserId(1L)).thenReturn(List.of(new UserRole(1L, Role.USER)));

        SecurityPrincipal principal = service.loadUserByUsername("andrea@test.com");

        assertThat(principal.getId()).isEqualTo(1L);
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void unknownUserThrowsUsernameNotFound() {
        when(userRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nadie"))
                .isInstanceOf(UsernameNotFoundException.class);
        verifyNoInteractions(credentialRepository);
    }

    @Test
    void blankIdentifierThrowsWithoutQueryingDatabase() {
        assertThatThrownBy(() -> service.loadUserByUsername("   "))
                .isInstanceOf(UsernameNotFoundException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void legacyUserWithoutCredentialCannotLogin() {
        User legacy = user(2L, "legado", "legado@test.com", true);
        when(userRepository.findByUsername("legado")).thenReturn(Optional.of(legacy));
        when(credentialRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("legado"))
                .isInstanceOf(UsernameNotFoundException.class);
        verifyNoInteractions(roleRepository);
    }

    @Test
    void inactiveUserIsDisabled() {
        User inactive = user(3L, "inactivo", "inactivo@test.com", false);
        when(userRepository.findByUsername("inactivo")).thenReturn(Optional.of(inactive));
        when(credentialRepository.findById(3L)).thenReturn(Optional.of(new UserCredential(inactive, HASH)));
        when(roleRepository.findByIdUserId(3L)).thenReturn(List.of(new UserRole(3L, Role.USER)));

        SecurityPrincipal principal = service.loadUserByUsername("inactivo");

        assertThat(principal.isEnabled()).isFalse();
    }

    @Test
    void multipleRolesBecomeAuthoritiesWithRolePrefix() {
        User laura = user(4L, "laura", "laura@test.com", true);
        when(userRepository.findByUsername("laura")).thenReturn(Optional.of(laura));
        when(credentialRepository.findById(4L)).thenReturn(Optional.of(new UserCredential(laura, HASH)));
        when(roleRepository.findByIdUserId(4L)).thenReturn(
                List.of(new UserRole(4L, Role.USER), new UserRole(4L, Role.STAFF)));

        SecurityPrincipal principal = service.loadUserByUsername("laura");

        assertThat(authorityNames(principal)).containsExactlyInAnyOrder("ROLE_USER", "ROLE_STAFF");
    }

    @Test
    void loadUserByIdReturnsCurrentRolesWithoutPasswordHash() {
        User andrea = user(1L, "andrea", "andrea@test.com", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(andrea));
        when(roleRepository.findByIdUserId(1L)).thenReturn(List.of(new UserRole(1L, Role.ADMIN)));

        SecurityPrincipal principal = service.loadUserById(1L);

        assertThat(principal.getId()).isEqualTo(1L);
        assertThat(principal.getPassword()).isNull();
        assertThat(authorityNames(principal)).containsExactly("ROLE_ADMIN");
        verifyNoInteractions(credentialRepository);
    }

    @Test
    void loadUserByIdUnknownThrows() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserById(99L))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}