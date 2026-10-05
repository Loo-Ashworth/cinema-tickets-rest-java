package uk.gov.dwp.engineering.recruitment.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

class BookingConfirmationTest {

  @Test
  void givenBookingConfirmation_whenGetAccount_thenExpectedValueReturned() {
    final BookingConfirmation bookingConfirmation = new BookingConfirmation(99L);
    assertEquals(99L, bookingConfirmation.accountId());
  }

  @Test
  void givenBookingConfirmation_whenGetAllValues_thenExpectedValuesReturned() {
    final BookingConfirmation bookingConfirmation = new BookingConfirmation(99L, new BigDecimal("25.99"), 1L, UUID.randomUUID());
    assertEquals(99L, bookingConfirmation.accountId());
    assertEquals(new BigDecimal("25.99"), bookingConfirmation.totalPrice());
    assertEquals(1L, bookingConfirmation.seatsRequired());
    assertNotNull(bookingConfirmation.bookingReference());
  }
}
