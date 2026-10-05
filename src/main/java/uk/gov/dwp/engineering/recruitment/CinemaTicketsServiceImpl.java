package uk.gov.dwp.engineering.recruitment;

import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.calculation.SeatNumberCalculator;
import uk.gov.dwp.engineering.recruitment.calculation.TicketPriceCalculator;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;
import uk.gov.dwp.engineering.recruitment.validation.AccountValidator;
import uk.gov.dwp.engineering.recruitment.validation.TicketRequestsValidator;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class CinemaTicketsServiceImpl implements CinemaTicketsService {

    private final PaymentService paymentService;

    private final SeatReservationService seatReservationService;

    private final AccountValidator accountValidator;

    private final TicketRequestsValidator ticketRequestsValidator;

    private final SeatNumberCalculator seatNumberCalculator;

    private final TicketPriceCalculator ticketPriceCalculator;

    public CinemaTicketsServiceImpl(PaymentService paymentService, SeatReservationService seatReservationService, AccountValidator accountValidator, TicketRequestsValidator ticketRequestsValidator, SeatNumberCalculator seatNumberCalculator, TicketPriceCalculator ticketPriceCalculator) {
        this.paymentService = paymentService;
        this.seatReservationService = seatReservationService;
        this.accountValidator = accountValidator;
        this.ticketRequestsValidator = ticketRequestsValidator;
        this.seatNumberCalculator = seatNumberCalculator;
        this.ticketPriceCalculator = ticketPriceCalculator;
    }

    @Override
    public BookingConfirmation purchaseTickets(final Long accountId, final TicketRequest... ticketRequests) throws InvalidBookingException {
        accountValidator.validateAccountId(accountId);
        Map<TicketType, Integer> ticketCounts = ticketRequestsValidator.countTickets(ticketRequests);
        ticketRequestsValidator.validateTicketRequests(ticketCounts);

        Long seatsRequired = seatNumberCalculator.calculateSeatsRequired(ticketCounts);
        BigDecimal totalPrice = ticketPriceCalculator.calculateTotalPrice(ticketCounts);

        seatReservationService.reserveSeats(accountId, seatsRequired);
        paymentService.debitAccount(accountId, totalPrice);

        return new BookingConfirmation(accountId, totalPrice, seatsRequired, UUID.randomUUID());
    }
}
