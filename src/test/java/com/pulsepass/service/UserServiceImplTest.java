package com.pulsepass.service;

import com.pulsepass.domain.User;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.impl.UserServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository repository;

    @Mock
    private UserMapper mapper;

    @InjectMocks
    private UserServiceImpl service;

    private RegisterUserRequest validRequest() {
        return new RegisterUserRequest(
                "andrea", "andrea@email.com", "Andrea", "Gomez",
                "3001234567", "Santa Marta", LocalDate.of(1998, 5, 10));
    }

    // ---------- TEST-USER-001: registro válido ----------
    @Test
    void shouldRegisterValidUser() {
        UserResponse response = new UserResponse(
                1L, "andrea", "andrea@email.com", true,
                "Andrea", "Gomez", "3001234567", "Santa Marta", LocalDate.of(1998, 5, 10));

        when(repository.existsByUsername("andrea")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(User.class))).thenReturn(response);

        UserResponse result = service.register(validRequest());

        assertThat(result).isEqualTo(response);
        verify(repository).save(any(User.class));
    }

    // ---------- TEST-USER-002: username duplicado ----------
    @Test
    void shouldThrowWhenUsernameAlreadyExists() {
        when(repository.existsByUsername("andrea")).thenReturn(true);

        assertThatThrownBy(() -> service.register(validRequest()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(repository, never()).save(any());
    }

    // ---------- TEST-USER-003: email duplicado ----------
    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        when(repository.existsByUsername("andrea")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(validRequest()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(repository, never()).save(any());
    }

    // ---------- TEST-USER-004: fecha de nacimiento futura ----------
    @Test
    void shouldThrowWhenBirthDateIsInFuture() {
        RegisterUserRequest request = new RegisterUserRequest(
                "andrea", "andrea@email.com", "Andrea", "Gomez",
                "3001234567", "Santa Marta", LocalDate.now().plusDays(1));

        when(repository.existsByUsername("andrea")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(repository, never()).save(any());
    }
}