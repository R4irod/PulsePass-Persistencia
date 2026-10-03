package com.pulsepass.dto.request;

import com.pulsepass.domain.TicketType;

public record PurchaseTicketRequest(
        String userEmail,
        String eventCode,
        TicketType type
) {
}