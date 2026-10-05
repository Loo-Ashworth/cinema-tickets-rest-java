package uk.gov.dwp.engineering.recruitment.calculation;

import org.junit.jupiter.api.Test;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TicketPriceCalculatorTest {
    
    private final TicketPriceCalculator classUnderTest = new TicketPriceCalculator();

    @Test
    void givenSingleAdultTicket_whenCalculatingTotalPrice_thenChargesAdultPrice() {
        Map<TicketType, Integer> ticketCounts = Map.of(TicketType.ADULT, 1);

        BigDecimal total = classUnderTest.calculateTotalPrice(ticketCounts);

        assertEquals(0, new BigDecimal("25.99").compareTo(total));
    }

    @Test
    void givenSingleChildTicket_whenCalculatingTotalPrice_thenChargesChildPrice() {
        Map<TicketType, Integer> ticketCounts = Map.of(TicketType.CHILD, 1);

        BigDecimal total = classUnderTest.calculateTotalPrice(ticketCounts);

        assertEquals(0, new BigDecimal("17.50").compareTo(total));
    }

    @Test
    void givenSingleInfantTicket_whenCalculatingTotalPrice_thenTotalIsZero() {
        Map<TicketType, Integer> ticketCounts = Map.of(TicketType.INFANT, 1);

        BigDecimal total = classUnderTest.calculateTotalPrice(ticketCounts);

        assertEquals(0, BigDecimal.ZERO.compareTo(total));
    }

    @Test
    void givenOneOfEachTicketType_whenCalculatingTotalPrice_thenSumsAllPrices() {
        Map<TicketType, Integer> ticketCounts = Map.of(
                TicketType.ADULT, 1,
                TicketType.CHILD, 1,
                TicketType.INFANT, 1);

        BigDecimal total = classUnderTest.calculateTotalPrice(ticketCounts);

        assertEquals(0, new BigDecimal("43.49").compareTo(total));
    }

    @Test
    void givenThreeOfEachTicketType_whenCalculatingTotalPrice_thenSumsAllPrices() {
        Map<TicketType, Integer> ticketCounts = Map.of(
                TicketType.ADULT, 3,
                TicketType.CHILD, 3,
                TicketType.INFANT, 3);

        BigDecimal total = classUnderTest.calculateTotalPrice(ticketCounts);

        assertEquals(0, new BigDecimal("130.47").compareTo(total));
    }
}