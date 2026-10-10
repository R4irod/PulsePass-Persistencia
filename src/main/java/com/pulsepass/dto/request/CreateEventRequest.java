package com.pulsepass.dto.request;

import com.pulsepass.domain.EventCategory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Validación ESTRUCTURAL (Bean Validation): campos obligatorios, longitudes y mínimos.
 * Las reglas de negocio (venue existente/activo, fecha futura, código único...)
 * viven en EventServiceImpl.
 */
public record CreateEventRequest(

        @NotBlank(message = "Event code is required")
        @Size(max = 30, message = "Event code must not exceed 30 characters")
        String eventCode,

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @NotNull(message = "Category is required")
        EventCategory category,

        @NotNull(message = "Event date is required")
        LocalDateTime eventDate,

        @NotNull(message = "Minimum age is required")
        @Min(value = 0, message = "Minimum age must be 0 or greater")
        Integer minimumAge,

        @NotBlank(message = "Venue code is required")
        @Size(max = 30, message = "Venue code must not exceed 30 characters")
        String venueCode
) {
}
