package com.pulsepass.dto.request;

import com.pulsepass.domain.TicketType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Validación ESTRUCTURAL (Bean Validation). Usuario activo, evento publicado,
 * edad mínima, capacidad y precio se resuelven en TicketServiceImpl.
 * Nota: el cliente NO envía precio; lo calcula el Service.
 */
public record PurchaseTicketRequest(

        @NotBlank(message = "User email is required")
        @Email(message = "User email must be valid")
        String userEmail,

        @NotBlank(message = "Event code is required")
        String eventCode,

        @NotNull(message = "Ticket type is required")
        TicketType type
) {
}
