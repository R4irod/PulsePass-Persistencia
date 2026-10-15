package com.pulsepass.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Validación ESTRUCTURAL (Bean Validation). La unicidad de username/email y que
 * birthDate no sea futura son reglas de negocio y viven en UserServiceImpl.
 */
public record RegisterUserRequest(

        @NotBlank(message = "Username is required")
        @Size(max = 50, message = "Username must not exceed 50 characters")
        @Pattern(regexp = "^[^@\\s]*$", message = "Username must not contain '@' or whitespace")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @NotBlank(message = "First name is required")
        @Size(max = 80, message = "First name must not exceed 80 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 80, message = "Last name must not exceed 80 characters")
        String lastName,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @NotNull(message = "Birth date is required")
        LocalDate birthDate,

        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 72, message = "Password must be between 12 and 72 characters")
        String password
) {

    // BCrypt solo usa los primeros 72 BYTES; con tildes o emojis 72 caracteres pueden pasarse.
    @AssertTrue(message = "Password must not exceed 72 bytes")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    // Sin la contraseña: evita que termine en logs por accidente (SEC003).
    @Override
    public String toString() {
        return "RegisterUserRequest[username=" + username + ", email=" + email + ", password=***]";
    }
}