package com.pulsepass.service;

import com.pulsepass.domain.*;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminRoleServiceTest {
    @Test
    void addingRolePreservesExistingRolesAndDoesNotDuplicate() {
        var users = mock(UserRepository.class);
        var roles = mock(UserRoleRepository.class);
        var user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(roles.findByIdUserId(1L)).thenReturn(List.of(
                new UserRole(1L, Role.USER), new UserRole(1L, Role.STAFF)));
        var service = new AdminRoleService(users, roles);

        assertEquals(List.of(Role.USER, Role.STAFF), service.add(1L, Role.STAFF).roles());
        verify(roles).save(argThat(role -> role.getRole() == Role.STAFF));
        when(roles.existsById(new UserRoleId(1L, Role.STAFF))).thenReturn(true);
        service.add(1L, Role.STAFF);
        verify(roles, times(1)).save(any());
        verify(roles, never()).delete(any());
    }

    @Test
    void missingAccountDoesNotCreateRole() {
        var users = mock(UserRepository.class);
        var roles = mock(UserRoleRepository.class);
        when(users.findById(99L)).thenReturn(Optional.empty());
        var service = new AdminRoleService(users, roles);
        assertThrows(ResourceNotFoundException.class, () -> service.add(99L, Role.ADMIN));
        verifyNoInteractions(roles);
    }
}
