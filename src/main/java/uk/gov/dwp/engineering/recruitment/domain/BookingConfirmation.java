package uk.gov.dwp.engineering.recruitment.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingConfirmation(
        Long accountId,
        BigDecimal totalPrice,
        Long seatsRequired,
        UUID bookingReference
) {

    public BookingConfirmation(Long accountId) {
        this(accountId, null, null, null);
    }
}