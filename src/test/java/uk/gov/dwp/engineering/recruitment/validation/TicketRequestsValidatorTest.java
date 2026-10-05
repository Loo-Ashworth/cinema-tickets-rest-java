package uk.gov.dwp.engineering.recruitment.validation;

import org.junit.jupiter.api.Test;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TicketRequestsValidatorTest {

    private final TicketRequestsValidator classUnderTest = new TicketRequestsValidator();

    @Test
    void givenNegativeTicketQuantity_whenCountingTickets_thenThrowsInvalidQuantity() {
        TicketRequest[] negativeTicketQuantity = {new TicketRequest(TicketType.ADULT, -1)};

        InvalidBookingException exception = assertThrows(InvalidBookingException.class,
                () -> classUnderTest.countTickets(negativeTicketQuantity));

        assertEquals("Invalid ticket quantity", exception.getMessage());
    }

    @Test
    void givenNullTicketType_whenCountingTickets_thenThrowsUnexpectedTicketType() {
        TicketRequest[] nullTicketType = {new TicketRequest(null, 1)};

        InvalidBookingException exception = assertThrows(InvalidBookingException.class,
                () -> classUnderTest.countTickets(nullTicketType));

        assertEquals("Unexpected Ticket Type", exception.getMessage());
    }

    @Test
    void givenZeroTickets_whenValidatingTicketRequests_thenThrowsTotalMustBeGreaterThanZero() {
        Map<TicketType, Integer> zeroTicketCount = classUnderTest.countTickets(new TicketRequest[]{
                new TicketRequest(TicketType.ADULT, 0)});

        InvalidBookingException exception = assertThrows(InvalidBookingException.class,
                () -> classUnderTest.validateTicketRequests(zeroTicketCount));

        assertEquals("Ticket total must be greater than zero", exception.getMessage());
    }

    @Test
    void givenMoreThan25Tickets_whenValidatingTicketRequests_thenThrowsMaximumExceeded() {
        Map<TicketType, Integer> overTwentyFiveTickets = classUnderTest.countTickets(new TicketRequest[]{
                new TicketRequest(TicketType.ADULT, 26)});

        InvalidBookingException exception = assertThrows(InvalidBookingException.class,
                () -> classUnderTest.validateTicketRequests(overTwentyFiveTickets));

        assertEquals("A maximum of 25 tickets that can be purchased at a time", exception.getMessage());
    }

    @Test
    void givenMultipleRequestsExceeding25_whenValidatingTicketRequests_thenThrowsMaximumExceeded() {
        Map<TicketType, Integer> multiOverTwentyFiveTickets = classUnderTest.countTickets(new TicketRequest[]{
                new TicketRequest(TicketType.ADULT, 13),
                new TicketRequest(TicketType.ADULT, 13)});

        InvalidBookingException exception = assertThrows(InvalidBookingException.class,
                () -> classUnderTest.validateTicketRequests(multiOverTwentyFiveTickets));

        assertEquals("A maximum of 25 tickets that can be purchased at a time", exception.getMessage());
    }

    @Test
    void givenInfantTicketOnly_whenValidatingTicketRequests_thenThrowsAdultPerInfantRequired() {
        Map<TicketType, Integer> infantTicketOnly = classUnderTest.countTickets(new TicketRequest[]{
                new TicketRequest(TicketType.INFANT, 1)});

        InvalidBookingException exception = assertThrows(InvalidBookingException.class,
                () -> classUnderTest.validateTicketRequests(infantTicketOnly));

        assertEquals("One adult ticket per infant ticket is required", exception.getMessage());
    }

    @Test
    void givenMoreInfantsThanAdults_whenValidatingTicketRequests_thenThrowsAdultPerInfantRequired() {
        Map<TicketType, Integer> tooManyInfants = classUnderTest.countTickets(new TicketRequest[]{
                new TicketRequest(TicketType.ADULT, 1),
                new TicketRequest(TicketType.INFANT, 2)});

        InvalidBookingException exception = assertThrows(InvalidBookingException.class,
                () -> classUnderTest.validateTicketRequests(tooManyInfants));

        assertEquals("One adult ticket per infant ticket is required", exception.getMessage());
    }

    @Test
    void givenChildTicketWithoutAdult_whenValidatingTicketRequests_thenThrowsChildrenNeedAdult() {
        Map<TicketType, Integer> childOnly = classUnderTest.countTickets(new TicketRequest[]{
                new TicketRequest(TicketType.CHILD, 1)});

        InvalidBookingException exception = assertThrows(InvalidBookingException.class,
                () -> classUnderTest.validateTicketRequests(childOnly));

        assertEquals("Children must be accompanied by at least one adult", exception.getMessage());
    }

    @Test
    void givenChildAndInfantTicketsWithoutAdult_whenValidatingTicketRequests_thenThrowsChildrenNeedAdult() {
        Map<TicketType, Integer> noAdultTicket = classUnderTest.countTickets(new TicketRequest[]{
                new TicketRequest(TicketType.CHILD, 1),
                new TicketRequest(TicketType.INFANT, 1)});

        InvalidBookingException exception = assertThrows(InvalidBookingException.class,
                () -> classUnderTest.validateTicketRequests(noAdultTicket));

        assertEquals("Children must be accompanied by at least one adult", exception.getMessage());
    }

    @Test
    void givenOneAdult_whenValidatingTicketRequests_thenDoesNotThrow() {
        Map<TicketType, Integer> oneAdultTicket = classUnderTest.countTickets(new TicketRequest[]{
                new TicketRequest(TicketType.ADULT, 1)});

        assertDoesNotThrow(() -> classUnderTest.validateTicketRequests(oneAdultTicket));
    }

    @Test
    void givenOneAdultAndOneInfant_whenValidatingTicketRequests_thenDoesNotThrow() {
        Map<TicketType, Integer> oneAdultOneInfant = classUnderTest.countTickets(new TicketRequest[]{
                new TicketRequest(TicketType.ADULT, 1),
                new TicketRequest(TicketType.INFANT, 1)});

        assertDoesNotThrow(() -> classUnderTest.validateTicketRequests(oneAdultOneInfant));
    }

    @Test
    void givenOneAdultAndOneChild_whenValidatingTicketRequests_thenDoesNotThrow() {
        Map<TicketType, Integer> oneAdultOneChild = classUnderTest.countTickets(new TicketRequest[]{
                new TicketRequest(TicketType.ADULT, 1),
                new TicketRequest(TicketType.CHILD, 1)});

        assertDoesNotThrow(() -> classUnderTest.validateTicketRequests(oneAdultOneChild));
    }

    @Test
    void givenOneOfEachTicketType_whenValidatingTicketRequests_thenDoesNotThrow() {
        Map<TicketType, Integer> oneOfEach = classUnderTest.countTickets(new TicketRequest[]{
                new TicketRequest(TicketType.ADULT, 1),
                new TicketRequest(TicketType.CHILD, 1),
                new TicketRequest(TicketType.INFANT, 1)});

        assertDoesNotThrow(() -> classUnderTest.validateTicketRequests(oneOfEach));
    }

    @Test
    void givenTwoOfEachTypeOnSeparateRequests_whenValidatingTicketRequests_thenDoesNotThrow() {
        Map<TicketType, Integer> twoOfEachSeparate = classUnderTest.countTickets(new TicketRequest[]{
                new TicketRequest(TicketType.ADULT, 1),
                new TicketRequest(TicketType.ADULT, 1),
                new TicketRequest(TicketType.CHILD, 1),
                new TicketRequest(TicketType.CHILD, 1),
                new TicketRequest(TicketType.INFANT, 1),
                new TicketRequest(TicketType.INFANT, 1)});

        assertEquals(2, twoOfEachSeparate.get(TicketType.ADULT));
        assertEquals(2, twoOfEachSeparate.get(TicketType.CHILD));
        assertEquals(2, twoOfEachSeparate.get(TicketType.INFANT));
        assertDoesNotThrow(() -> classUnderTest.validateTicketRequests(twoOfEachSeparate));
    }

    @Test
    void givenExactly25AdultTickets_whenValidatingTicketRequests_thenDoesNotThrow() {
        Map<TicketType, Integer> twentyFiveAdults = classUnderTest.countTickets(new TicketRequest[]{
                new TicketRequest(TicketType.ADULT, 25)});

        assertDoesNotThrow(() -> classUnderTest.validateTicketRequests(twentyFiveAdults));
    }

    @Test
    void givenExactly25MixedTickets_whenValidatingTicketRequests_thenDoesNotThrow() {
        Map<TicketType, Integer> twentyFiveMixed = classUnderTest.countTickets(new TicketRequest[]{
                new TicketRequest(TicketType.ADULT, 10),
                new TicketRequest(TicketType.CHILD, 10),
                new TicketRequest(TicketType.INFANT, 5)});

        assertDoesNotThrow(() -> classUnderTest.validateTicketRequests(twentyFiveMixed));
    }

    @Test
    void givenValidAndNullTicketTypeRequests_whenCountingTickets_thenThrowsUnexpectedTicketType() {
        TicketRequest[] oneValidOneInvalid = {
                new TicketRequest(TicketType.ADULT, 1),
                new TicketRequest(null, 5)};

        InvalidBookingException exception = assertThrows(InvalidBookingException.class,
                () -> classUnderTest.countTickets(oneValidOneInvalid));

        assertEquals("Unexpected Ticket Type", exception.getMessage());
    }

    @Test
    void givenValidAndNegativeQuantityRequests_whenCountingTickets_thenThrowsInvalidQuantity() {
        TicketRequest[] oneValidOneNegative = {
                new TicketRequest(TicketType.ADULT, 5),
                new TicketRequest(TicketType.ADULT, -1)};

        InvalidBookingException exception = assertThrows(InvalidBookingException.class,
                () -> classUnderTest.countTickets(oneValidOneNegative));

        assertEquals("Invalid ticket quantity", exception.getMessage());
    }
}
