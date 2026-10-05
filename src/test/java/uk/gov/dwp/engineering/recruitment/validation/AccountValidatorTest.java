package uk.gov.dwp.engineering.recruitment.validation;

import org.junit.jupiter.api.Test;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

import static org.junit.jupiter.api.Assertions.*;

public class AccountValidatorTest {

    private final AccountValidator classUnderTest = new AccountValidator();

    @Test
    void givenNullAccountId_whenValidatingAccountId_thenThrowsInvalidBookingException() {
        InvalidBookingException exception = assertThrows(InvalidBookingException.class,
                () -> classUnderTest.validateAccountId(null));

        assertEquals("Account ID must not be null", exception.getMessage());
    }

    @Test
    void givenZeroAccountId_whenValidatingAccountId_thenThrowsInvalidBookingException() {
        InvalidBookingException exception = assertThrows(InvalidBookingException.class,
                () -> classUnderTest.validateAccountId(0L));

        assertEquals("Account ID must be greater than zero", exception.getMessage());
    }

    @Test
    void givenNegativeAccountId_whenValidatingAccountId_thenThrowsInvalidBookingException() {
        InvalidBookingException exception = assertThrows(InvalidBookingException.class,
                () -> classUnderTest.validateAccountId(-1L));

        assertEquals("Account ID must be greater than zero", exception.getMessage());
    }

    @Test
    void givenPositiveAccountId_whenValidatingAccountId_thenDoesNotThrow() {
        assertDoesNotThrow(() -> classUnderTest.validateAccountId(1L));
    }

}
