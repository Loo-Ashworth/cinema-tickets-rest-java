package uk.gov.dwp.engineering.recruitment.calculation;

import org.springframework.stereotype.Component;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

import java.util.Map;

@Component
public class SeatNumberCalculator {

    public Long calculateSeatsRequired(Map<TicketType, Integer> ticketCounts) {
        return (long) (ticketCounts.get(TicketType.CHILD) + ticketCounts.get(TicketType.ADULT));
    }
}
