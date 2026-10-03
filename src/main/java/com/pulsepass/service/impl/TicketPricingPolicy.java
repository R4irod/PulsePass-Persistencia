package com.pulsepass.service.impl;

import com.pulsepass.domain.TicketType;

import java.math.BigDecimal;

final class TicketPricingPolicy {

    private static final BigDecimal BASE_PRICE = new BigDecimal("100.00");
    private static final BigDecimal STUDENT_DISCOUNT = new BigDecimal("0.50");
    private static final BigDecimal VIP_MULTIPLIER = new BigDecimal("2.00");
    private static final BigDecimal BACKSTAGE_MULTIPLIER = new BigDecimal("3.00");

    private TicketPricingPolicy() {
    }

    static BigDecimal calculatePrice(TicketType type) {
        return switch (type) {
            case GENERAL -> BASE_PRICE;
            case STUDENT -> BASE_PRICE.multiply(STUDENT_DISCOUNT);
            case VIP -> BASE_PRICE.multiply(VIP_MULTIPLIER);
            case BACKSTAGE -> BASE_PRICE.multiply(BACKSTAGE_MULTIPLIER);
        };
    }
}