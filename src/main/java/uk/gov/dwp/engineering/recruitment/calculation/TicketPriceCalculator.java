package uk.gov.dwp.engineering.recruitment.calculation;

import org.springframework.stereotype.Component;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class TicketPriceCalculator {
    private static final Map<TicketType, BigDecimal> TICKET_PRICES = Map.of(
            TicketType.INFANT, new BigDecimal("0"),
            TicketType.CHILD, new BigDecimal("17.50"),
            TicketType.ADULT, new BigDecimal("25.99")
    );

    public BigDecimal calculateTotalPrice(Map<TicketType, Integer> ticketCounts) {
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (TicketType ticketType : ticketCounts.keySet()) {
            BigDecimal numberOfTicketsForType = new BigDecimal(ticketCounts.get(ticketType));
            BigDecimal priceForType = TICKET_PRICES.get(ticketType);

            BigDecimal sumForTicketType = numberOfTicketsForType.multiply(priceForType);
            totalPrice = totalPrice.add(sumForTicketType);
        }
        return totalPrice;
    }
}
