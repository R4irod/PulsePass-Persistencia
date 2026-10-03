package com.pulsepass.dto.response;

import java.time.LocalDate;

public record UserResponse(
        Long id,
        String username,
        String email,
        boolean active,
        String firstName,
        String lastName,
        String phone,
        String city,
        LocalDate birthDate
) {
}