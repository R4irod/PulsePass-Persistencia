package com.pulsepass.dto.request;

import java.time.LocalDate;

public record RegisterUserRequest(
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        String city,
        LocalDate birthDate
) {
}