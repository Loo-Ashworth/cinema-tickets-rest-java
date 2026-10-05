package uk.gov.dwp.engineering.recruitment.calculation;

import org.junit.jupiter.api.Test;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SeatNumberCalculatorTest {

    private final SeatNumberCalculator classUnderTest = new SeatNumberCalculator();

    @Test
    void givenSingleAdultTicket_whenCalculatingSeatsRequired_thenRequiresOneSeat() {
        Map<TicketType, Integer> ticketCounts = Map.of(
                TicketType.ADULT, 1,
                TicketType.CHILD, 0,
                TicketType.INFANT, 0);

        Long seats = classUnderTest.calculateSeatsRequired(ticketCounts);

        assertEquals(1L, seats);
    }

    @Test
    void givenSingleChildTicket_whenCalculatingSeatsRequired_thenRequiresOneSeat() {
        Map<TicketType, Integer> ticketCounts = Map.of(
                TicketType.ADULT, 0,
                TicketType.CHILD, 1,
                TicketType.INFANT, 0);

        Long seats = classUnderTest.calculateSeatsRequired(ticketCounts);

        assertEquals(1L, seats);
    }

    @Test
    void givenSingleInfantTicket_whenCalculatingSeatsRequired_thenRequiresNoSeats() {
        Map<TicketType, Integer> ticketCounts = Map.of(
                TicketType.ADULT, 0,
                TicketType.CHILD, 0,
                TicketType.INFANT, 1);

        Long seats = classUnderTest.calculateSeatsRequired(ticketCounts);

        assertEquals(0L, seats);
    }

    @Test
    void givenOneOfEachTicketType_whenCalculatingSeatsRequired_thenInfantDoesNotNeedSeat() {
        Map<TicketType, Integer> ticketCounts = Map.of(
                TicketType.ADULT, 1,
                TicketType.CHILD, 1,
                TicketType.INFANT, 1);

        Long seats = classUnderTest.calculateSeatsRequired(ticketCounts);

        assertEquals(2L, seats);
    }

    @Test
    void givenThreeOfEachTicketType_whenCalculatingSeatsRequired_thenInfantsDoNotNeedSeats() {
        Map<TicketType, Integer> ticketCounts = Map.of(
                TicketType.ADULT, 3,
                TicketType.CHILD, 3,
                TicketType.INFANT, 3);

        Long seats = classUnderTest.calculateSeatsRequired(ticketCounts);

        assertEquals(6L, seats);
    }

}
