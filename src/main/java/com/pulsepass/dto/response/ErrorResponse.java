package com.pulsepass.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Contrato uniforme de error para TODA la API (PRD sección 13).
 * details: vacío salvo en errores de validación, donde mapea campo -> mensaje.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> details
) {
}
