package uk.gov.dwp.engineering.recruitment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CinemaTicketsServiceImplTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private SeatReservationService seatReservationService;

    @Mock
    private AccountValidator accountValidator;

    @Mock
    private TicketRequestsValidator ticketRequestsValidator;

    @Mock
    private SeatNumberCalculator seatNumberCalculator;

    @Mock
    private TicketPriceCalculator ticketPriceCalculator;

    private CinemaTicketsServiceImpl classUnderTest;

    @BeforeEach
    void setUp() {
        this.classUnderTest = new CinemaTicketsServiceImpl(
                paymentService,
                seatReservationService,
                accountValidator,
                ticketRequestsValidator,
                seatNumberCalculator,
                ticketPriceCalculator);
    }

    @Test
    void givenInvalidAccountId_whenPurchaseTickets_thenThrowsException_andDoesNothingElse() {
        TicketRequest validRequest = new TicketRequest(TicketType.ADULT, 1);
        InvalidBookingException expected = new InvalidBookingException("Account ID must be greater than zero");
        doThrow(expected).when(accountValidator).validateAccountId(-1L);

        Exception exception = assertThrows(InvalidBookingException.class, () -> classUnderTest.purchaseTickets(-1L, validRequest));

        assertSame(expected, exception);
        verifyNoInteractions(ticketRequestsValidator, seatNumberCalculator, ticketPriceCalculator, seatReservationService, paymentService);
    }

    @Test
    void givenTicketsCannotBeCounted_whenPurchaseTickets_thenThrowsException_andDoesNotReserveOrPay() {
        TicketRequest invalidRequest = new TicketRequest(TicketType.ADULT, -1);
        InvalidBookingException expected = new InvalidBookingException("Invalid ticket quantity");
        when(ticketRequestsValidator.countTickets(new TicketRequest[]{invalidRequest})).thenThrow(expected);

        Exception exception = assertThrows(InvalidBookingException.class, () -> classUnderTest.purchaseTickets(1L, invalidRequest));

        assertSame(expected, exception);
        verify(ticketRequestsValidator, never()).validateTicketRequests(any());
        verifyNoInteractions(seatNumberCalculator, ticketPriceCalculator, seatReservationService, paymentService);
    }

    @Test
    void givenInvalidTicketRequests_whenPurchaseTickets_thenThrowsException_andDoesNotReserveOrPay() {
        TicketRequest zeroTicketsRequest = new TicketRequest(TicketType.ADULT, 0);
        Map<TicketType, Integer> ticketCounts = Map.of(
                TicketType.ADULT, 0,
                TicketType.CHILD, 0,
                TicketType.INFANT, 0);
        InvalidBookingException expected = new InvalidBookingException("Ticket total must be greater than zero");
        when(ticketRequestsValidator.countTickets(new TicketRequest[]{zeroTicketsRequest})).thenReturn(ticketCounts);
        doThrow(expected).when(ticketRequestsValidator).validateTicketRequests(ticketCounts);

        Exception exception = assertThrows(InvalidBookingException.class, () -> classUnderTest.purchaseTickets(1L, zeroTicketsRequest));

        assertSame(expected, exception);
        verifyNoInteractions(seatNumberCalculator, ticketPriceCalculator, seatReservationService, paymentService);
    }

    @Test
    void givenValidRequest_whenPurchaseTickets_thenReservesSeatsAndDebitsAccountWithCalculatedValues() {
        TicketRequest singleAdultRequest = new TicketRequest(TicketType.ADULT, 1);
        TicketRequest singleChildRequest = new TicketRequest(TicketType.CHILD, 1);
        Map<TicketType, Integer> ticketCounts = Map.of(
                TicketType.ADULT, 1,
                TicketType.CHILD, 1,
                TicketType.INFANT, 0);
        BigDecimal expectedPrice = new BigDecimal("43.49");
        Long expectedSeats = 2L;
        when(ticketRequestsValidator.countTickets(new TicketRequest[]{singleAdultRequest, singleChildRequest})).thenReturn(ticketCounts);
        when(seatNumberCalculator.calculateSeatsRequired(ticketCounts)).thenReturn(expectedSeats);
        when(ticketPriceCalculator.calculateTotalPrice(ticketCounts)).thenReturn(expectedPrice);

        classUnderTest.purchaseTickets(1L, singleAdultRequest, singleChildRequest);

        verify(seatReservationService).reserveSeats(1L, expectedSeats);
        verify(paymentService).debitAccount(1L, expectedPrice);
    }

    @Test
    void givenValidRequest_whenPurchaseTickets_thenReturnsConfirmationWithBookingDetails() {
        TicketRequest singleAdultRequest = new TicketRequest(TicketType.ADULT, 1);
        TicketRequest singleChildRequest = new TicketRequest(TicketType.CHILD, 1);
        Map<TicketType, Integer> ticketCounts = Map.of(
                TicketType.ADULT, 1,
                TicketType.CHILD, 1,
                TicketType.INFANT, 0);
        BigDecimal expectedPrice = new BigDecimal("43.49");
        Long expectedSeats = 2L;
        when(ticketRequestsValidator.countTickets(new TicketRequest[]{singleAdultRequest, singleChildRequest})).thenReturn(ticketCounts);
        when(seatNumberCalculator.calculateSeatsRequired(ticketCounts)).thenReturn(expectedSeats);
        when(ticketPriceCalculator.calculateTotalPrice(ticketCounts)).thenReturn(expectedPrice);

        BookingConfirmation confirmation = classUnderTest.purchaseTickets(1L, singleAdultRequest, singleChildRequest);

        assertEquals(1L, confirmation.accountId());
        assertEquals(expectedPrice, confirmation.totalPrice());
        assertEquals(expectedSeats, confirmation.seatsRequired());
        assertNotNull(confirmation.bookingReference());
    }

    @Test
    void givenValidRequest_whenPurchaseTickets_thenValidatesBeforeReservingAndReservesBeforePaying() {
        TicketRequest singleAdultRequest = new TicketRequest(TicketType.ADULT, 1);
        Map<TicketType, Integer> ticketCounts = Map.of(
                TicketType.ADULT, 1,
                TicketType.CHILD, 0,
                TicketType.INFANT, 0);
        BigDecimal expectedPrice = new BigDecimal("25.99");
        Long expectedSeats = 1L;
        when(ticketRequestsValidator.countTickets(new TicketRequest[]{singleAdultRequest})).thenReturn(ticketCounts);
        when(seatNumberCalculator.calculateSeatsRequired(ticketCounts)).thenReturn(expectedSeats);
        when(ticketPriceCalculator.calculateTotalPrice(ticketCounts)).thenReturn(expectedPrice);
        InOrder order = inOrder(accountValidator, ticketRequestsValidator, seatReservationService, paymentService);

        classUnderTest.purchaseTickets(1L, singleAdultRequest);

        order.verify(accountValidator).validateAccountId(1L);
        order.verify(ticketRequestsValidator).countTickets(new TicketRequest[]{singleAdultRequest});
        order.verify(ticketRequestsValidator).validateTicketRequests(ticketCounts);
        order.verify(seatReservationService).reserveSeats(1L, expectedSeats);
        order.verify(paymentService).debitAccount(1L, expectedPrice);
    }

    @Test
    void givenSeatReservationFails_whenPurchaseTickets_thenDoesNotDebitAccount() {
        TicketRequest singleAdultRequest = new TicketRequest(TicketType.ADULT, 1);
        Map<TicketType, Integer> ticketCounts = Map.of(
                TicketType.ADULT, 1,
                TicketType.CHILD, 0,
                TicketType.INFANT, 0);
        Long expectedSeats = 1L;
        RuntimeException expected = new RuntimeException("Reservation failed");
        when(ticketRequestsValidator.countTickets(new TicketRequest[]{singleAdultRequest})).thenReturn(ticketCounts);
        when(seatNumberCalculator.calculateSeatsRequired(ticketCounts)).thenReturn(expectedSeats);
        when(ticketPriceCalculator.calculateTotalPrice(ticketCounts)).thenReturn(new BigDecimal("25.99"));
        doThrow(expected).when(seatReservationService).reserveSeats(1L, expectedSeats);

        Exception exception = assertThrows(RuntimeException.class, () -> classUnderTest.purchaseTickets(1L, singleAdultRequest));

        assertSame(expected, exception);
        verify(paymentService, never()).debitAccount(anyLong(), any());
    }
}