package com.pulsepass.controller;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private static final String VALID_REGISTER_JSON = """
            {
              "username": "andrea",
              "email": "andrea@email.com",
              "firstName": "Andrea",
              "lastName": "Gomez",
              "phone": "3001234567",
              "city": "Santa Marta",
              "birthDate": "1999-05-20"
            }
            """;

    private UserResponse andrea() {
        return new UserResponse(
                1L, "andrea", "andrea@email.com", true,
                "Andrea", "Gomez", "3001234567", "Santa Marta", LocalDate.of(1999, 5, 20));
    }

    // TEST-CTRL-USR-001 — registrar válido -> 201
    @Test
    void shouldReturn201WhenRegisteringValidUser() throws Exception {
        when(userService.register(any(RegisterUserRequest.class))).thenReturn(andrea());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTER_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@email.com"))
                .andExpect(jsonPath("$.active").value(true));

        verify(userService).register(any(RegisterUserRequest.class));
    }

    // TEST-CTRL-USR-002 — email inválido -> 400; el Service NO se invoca
    @Test
    void shouldReturn400AndNotCallServiceWhenEmailIsInvalid() throws Exception {
        String json = VALID_REGISTER_JSON.replace("andrea@email.com", "not-an-email");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.email").value("Email must be valid"));

        verify(userService, never()).register(any());
    }

    @Test
    void shouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.username").value("Username is required"))
                .andExpect(jsonPath("$.details.email").value("Email is required"))
                .andExpect(jsonPath("$.details.firstName").value("First name is required"))
                .andExpect(jsonPath("$.details.lastName").value("Last name is required"))
                .andExpect(jsonPath("$.details.birthDate").value("Birth date is required"));

        verify(userService, never()).register(any());
    }

    // TEST-CTRL-USR-003 — username duplicado -> 409
    @Test
    void shouldReturn409WhenUsernameIsDuplicated() throws Exception {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists."));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTER_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Username already exists."));
    }

    // AC-CTRL-008 — email duplicado -> 409 sin exponer detalles internos
    @Test
    void shouldReturn409WhenEmailIsDuplicated() throws Exception {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Email already exists."));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTER_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists."))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    // TEST-CTRL-USR-004 — consultar por email -> 200
    @Test
    void shouldReturn200WhenFindingUserByEmail() throws Exception {
        when(userService.findByEmail("andrea@email.com")).thenReturn(andrea());

        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@email.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("andrea@email.com"))
                .andExpect(jsonPath("$.firstName").value("Andrea"));

        verify(userService).findByEmail("andrea@email.com");
    }

    @Test
    void shouldReturn404WhenUserEmailDoesNotExist() throws Exception {
        when(userService.findByEmail("ghost@email.com"))
                .thenThrow(new ResourceNotFoundException("User not found: ghost@email.com"));

        mockMvc.perform(get("/api/users/by-email").param("email", "ghost@email.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found: ghost@email.com"));
    }

    // TEST-CTRL-USR-005 — consultar por username -> 200
    @Test
    void shouldReturn200WhenFindingUserByUsername() throws Exception {
        when(userService.findByUsername("andrea")).thenReturn(andrea());

        mockMvc.perform(get("/api/users/by-username").param("username", "andrea"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(userService).findByUsername("andrea");
    }
}
