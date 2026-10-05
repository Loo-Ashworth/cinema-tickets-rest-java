package uk.gov.dwp.engineering.recruitment.validation;

import org.springframework.stereotype.Component;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Component
public class TicketRequestsValidator {

    private record Counts(int adults, int children, int infants) {
        int total() { return adults + children + infants; }
    }

    private record Rule(Predicate<Counts> isViolated, String message) {}

    private static final List<Rule> RULES = List.of(
            new Rule(count -> count.total() == 0, "Ticket total must be greater than zero"),
            new Rule(count -> count.total() > 25, "A maximum of 25 tickets that can be purchased at a time"),
            new Rule(count -> count.children() > 0 && count.adults() < 1, "Children must be accompanied by at least one adult"),
            new Rule(count -> count.infants() > count.adults(), "One adult ticket per infant ticket is required")
    );

    public Map<TicketType, Integer> countTickets(TicketRequest[] ticketRequests) {
        int totalAdults = 0;
        int totalInfants = 0;
        int totalChildren = 0;

        for (TicketRequest x : ticketRequests) {
            if (x.type() == null) {
                throw new InvalidBookingException("Unexpected Ticket Type");
            }
            if (x.ticketCount() < 0) {
                throw new InvalidBookingException("Invalid ticket quantity");
            }
            switch (x.type()) {
                case INFANT -> totalInfants += x.ticketCount();
                case CHILD -> totalChildren += x.ticketCount();
                case ADULT -> totalAdults += x.ticketCount();
            }
        }
        return Map.of(
                TicketType.ADULT, totalAdults,
                TicketType.CHILD, totalChildren,
                TicketType.INFANT, totalInfants);
    }

    public void validateTicketRequests(Map<TicketType, Integer> ticketCounts) throws InvalidBookingException {
        Counts counts = new Counts(
                ticketCounts.get(TicketType.ADULT),
                ticketCounts.get(TicketType.CHILD),
                ticketCounts.get(TicketType.INFANT));

        for (Rule rule : RULES) {
            if (rule.isViolated().test(counts)) {
                throw new InvalidBookingException(rule.message());
            }
        }
    }
}