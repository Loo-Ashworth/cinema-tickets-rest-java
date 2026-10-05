package uk.gov.dwp.engineering.recruitment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;
import uk.gov.dwp.engineering.recruitment.calculation.SeatNumberCalculator;
import uk.gov.dwp.engineering.recruitment.calculation.TicketPriceCalculator;
import uk.gov.dwp.engineering.recruitment.domain.Booking;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;
import uk.gov.dwp.engineering.recruitment.exception.RestExceptionHandler;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;
import uk.gov.dwp.engineering.recruitment.validation.AccountValidator;
import uk.gov.dwp.engineering.recruitment.validation.TicketRequestsValidator;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ExtendWith(MockitoExtension.class)
class CinemaTicketsControllerTest {

    @Mock
    PaymentService paymentService;

    @Mock
    SeatReservationService seatReservationService;

    @Mock
    CinemaTicketsService mockedCinemaTicketsService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String BOOKINGS_URL = "/cinema/bookings";

    @BeforeEach
    void setUp() {
        CinemaTicketsService realService = new CinemaTicketsServiceImpl(
                paymentService,
                seatReservationService,
                new AccountValidator(),
                new TicketRequestsValidator(),
                new SeatNumberCalculator(),
                new TicketPriceCalculator());

        mockMvc = MockMvcBuilders.standaloneSetup(new CinemaTicketsController(realService)).setControllerAdvice(new RestExceptionHandler()).build();
    }

    @Test
    void givenValidRequest_whenMakeBooking_thenDelegatesToServiceAndReturnsItsConfirmation() throws Exception {
        TicketRequest adultRequest = new TicketRequest(TicketType.ADULT, 1);
        Booking validBooking = new Booking(1L, adultRequest);
        UUID bookingReference = UUID.randomUUID();
        BookingConfirmation confirmation = new BookingConfirmation(1L, new BigDecimal("25.99"), 1L, bookingReference);
        when(mockedCinemaTicketsService.purchaseTickets(1L, adultRequest)).thenReturn(confirmation);
        MockMvc delegatingMockMvc = MockMvcBuilders.standaloneSetup(new CinemaTicketsController(mockedCinemaTicketsService))
                .setControllerAdvice(new RestExceptionHandler()).build();

        delegatingMockMvc.perform(post(BOOKINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBooking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingReference").value(bookingReference.toString()));

        verify(mockedCinemaTicketsService).purchaseTickets(1L, adultRequest);
    }

    @Test
    void givenValidBooking_whenMakeBooking_returnsHttpCreated() throws Exception {
        TicketRequest validRequest = new TicketRequest(TicketType.ADULT, 1);
        Booking validBooking = new Booking(1L, validRequest);

        mockMvc.perform(post(BOOKINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBooking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.totalPrice").value(25.99))
                .andExpect(jsonPath("$.seatsRequired").value(1))
                .andExpect(jsonPath("$.bookingReference").isNotEmpty());
    }

    @Test
    void givenMultipleTickets_whenMakeBooking__returnsHttpCreated() throws Exception {
        // BR: Multiple tickets can be purchased at any given time.

        TicketRequest validRequest = new TicketRequest(TicketType.ADULT, 1);
        TicketRequest validRequestTwo = new TicketRequest(TicketType.CHILD, 1);
        Booking validBooking = new Booking(1L, validRequest, validRequestTwo);

        mockMvc.perform(post(BOOKINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBooking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.totalPrice").value(43.49))
                .andExpect(jsonPath("$.seatsRequired").value(2))
                .andExpect(jsonPath("$.bookingReference").isNotEmpty());
    }

    @Test
    void givenZeroTickets_whenMakeBooking_thenReturnsBadRequest_withMessage() throws Exception {
        Booking zeroTicketsBooking = new Booking(1L, new TicketRequest(TicketType.ADULT, 0));

        mockMvc.perform(post(BOOKINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroTicketsBooking)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Ticket total must be greater than zero")));

        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void givenRequestedTicketsExceedsTwentyFive_whenMakeBooking_thenReturnsBadRequest_withMessage() throws Exception {
        // BR: Only a maximum of 25 tickets that can be purchased at a time.
        Booking twentySixTicketsBooking = new Booking(1L, new TicketRequest(TicketType.ADULT, 26));

        mockMvc.perform(post(BOOKINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(twentySixTicketsBooking)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("A maximum of 25 tickets that can be purchased at a time")));

        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void givenAnInvalidAccountId_whenMakeBooking_ThenReturnsBadRequest_withMessage() throws Exception {
        // BR: All accounts with an id greater than zero are valid.
        Booking zeroIdBooking = new Booking(0L, new TicketRequest(TicketType.ADULT, 1));

        mockMvc.perform(post(BOOKINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroIdBooking)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Account ID must be greater than zero")));

        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void givenAnInvalidTicketType_whenMakeBooking_ThenReturnsBadRequest_withMessage() throws Exception {
        // BR: There are 3 types of tickets i.e. INFANT, CHILD, and ADULT.
        Booking invalidTicketTypeBooking = new Booking(1L, new TicketRequest(null, 1));

        mockMvc.perform(post(BOOKINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTicketTypeBooking)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Unexpected Ticket Type")));

        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void givenAnInfantAndAdultTicketAreRequested_whenMakeBooking_thenOnlyOneSeatIsReserved() throws Exception {
        // BR: An INFANT does not pay for a ticket and are not allocated a seat. They will be sitting on an ADULT lap.
        TicketRequest adultRequest = new TicketRequest(TicketType.ADULT, 1);
        TicketRequest infantRequest = new TicketRequest(TicketType.INFANT, 1);
        Booking validBooking = new Booking(1L, adultRequest, infantRequest);

        Long expectedSeats = 1L;
        BigDecimal expectedPrice = new BigDecimal("25.99");

        mockMvc.perform(post(BOOKINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBooking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.totalPrice").value(25.99))
                .andExpect(jsonPath("$.seatsRequired").value(1))
                .andExpect(jsonPath("$.bookingReference").isNotEmpty());

        verify(seatReservationService).reserveSeats(1L, expectedSeats);
        verify(paymentService).debitAccount(1L, expectedPrice);
    }

    @Test
    void givenAnInfantOnlyTicket_whenMakeBooking_thenReturnsBadRequest_withMessage() throws Exception {
        // BR: CHILD and INFANT tickets cannot be purchased without purchasing an ADULT ticket.
        Booking infantOnlyBooking = new Booking(1L, new TicketRequest(TicketType.INFANT, 1));

        mockMvc.perform(post(BOOKINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(infantOnlyBooking)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("One adult ticket per infant ticket is required")));

        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void givenAChildOnlyTicket_whenMakeBooking_thenReturnsBadRequest_withMessage() throws Exception {
        // BR: CHILD and INFANT tickets cannot be purchased without purchasing an ADULT ticket.
        Booking childOnlyBooking = new Booking(1L, new TicketRequest(TicketType.CHILD, 1));

        mockMvc.perform(post(BOOKINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childOnlyBooking)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Children must be accompanied by at least one adult")));

        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void givenAChildAndInfantTicket_whenMakeBooking_thenReturnsBadRequest_withMessage() throws Exception {
        // BR: CHILD and INFANT tickets cannot be purchased without purchasing an ADULT ticket.
        TicketRequest childRequest = new TicketRequest(TicketType.CHILD, 1);
        TicketRequest infantRequest = new TicketRequest(TicketType.INFANT, 1);
        Booking noAdultBooking = new Booking(1L, childRequest, infantRequest);

        mockMvc.perform(post(BOOKINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noAdultBooking)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Children must be accompanied by at least one adult")));

        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void givenAValidSelectionOfTickets_whenMakeBooking_thenReturnsCreated() throws Exception {
        // BR: All accounts with an id greater than zero are valid.
        // Calculates the correct amount for the requested tickets and makes a payment request to the PaymentService
        // Calculates the correct number of seats to reserve and makes a seat reservation request to the SeatReservationService
        TicketRequest threeChildRequest = new TicketRequest(TicketType.CHILD, 3);
        TicketRequest twoAdultRequest = new TicketRequest(TicketType.ADULT, 2);
        TicketRequest oneInfantRequest = new TicketRequest(TicketType.INFANT, 1);

        BigDecimal expectedPrice = new BigDecimal("104.48");
        Long expectedSeats = 5L;

        Booking largeFamilyBooking = new Booking(1L, threeChildRequest, twoAdultRequest, oneInfantRequest);

        mockMvc.perform(post(BOOKINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(largeFamilyBooking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.totalPrice").value(104.48))
                .andExpect(jsonPath("$.seatsRequired").value(5))
                .andExpect(jsonPath("$.bookingReference").isNotEmpty());

        verify(seatReservationService).reserveSeats(1L, expectedSeats);
        verify(paymentService).debitAccount(1L, expectedPrice);
    }

    @Test
    void givenExactlyTwentyFiveAdultTickets_whenMakeBooking_thenReturnsCreated_withCorrectSeatsAndPrice() throws Exception {
        // BR: Only a maximum of 25 tickets that can be purchased at a time (boundary: exactly 25 is valid).
        Booking twentyFiveAdultsBooking = new Booking(1L, new TicketRequest(TicketType.ADULT, 25));

        BigDecimal expectedPrice = new BigDecimal("649.75");
        Long expectedSeats = 25L;

        mockMvc.perform(post(BOOKINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(twentyFiveAdultsBooking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.totalPrice").value(649.75))
                .andExpect(jsonPath("$.seatsRequired").value(25))
                .andExpect(jsonPath("$.bookingReference").isNotEmpty());

        verify(seatReservationService).reserveSeats(1L, expectedSeats);
        verify(paymentService).debitAccount(1L, expectedPrice);
    }

    @Test
    void givenExactlyTwentyFiveMixedTickets_whenMakeBooking_thenReturnsCreated_withCorrectSeatsAndPrice() throws Exception {
        // BR: Only a maximum of 25 tickets that can be purchased at a time (boundary: exactly 25 is valid).
        // BR: An INFANT does not pay for a ticket and are not allocated a seat.
        TicketRequest tenAdultRequest = new TicketRequest(TicketType.ADULT, 10);
        TicketRequest tenChildRequest = new TicketRequest(TicketType.CHILD, 10);
        TicketRequest fiveInfantRequest = new TicketRequest(TicketType.INFANT, 5);
        Booking mixedBooking = new Booking(1L, tenAdultRequest, tenChildRequest, fiveInfantRequest);

        BigDecimal expectedPrice = new BigDecimal("434.90");
        Long expectedSeats = 20L;

        mockMvc.perform(post(BOOKINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mixedBooking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.totalPrice").value(434.9))
                .andExpect(jsonPath("$.seatsRequired").value(20))
                .andExpect(jsonPath("$.bookingReference").isNotEmpty());

        verify(seatReservationService).reserveSeats(1L, expectedSeats);
        verify(paymentService).debitAccount(1L, expectedPrice);
    }

    @Test
    void givenTwoRequestsOfEachTicketType_whenMakeBooking_thenCountsAcrossRequests_withCorrectSeatsAndPrice() throws Exception {
        // BR: Multiple tickets can be purchased at any given time.
        TicketRequest adultOne = new TicketRequest(TicketType.ADULT, 1);
        TicketRequest adultTwo = new TicketRequest(TicketType.ADULT, 1);
        TicketRequest childOne = new TicketRequest(TicketType.CHILD, 1);
        TicketRequest childTwo = new TicketRequest(TicketType.CHILD, 1);
        TicketRequest infantOne = new TicketRequest(TicketType.INFANT, 1);
        TicketRequest infantTwo = new TicketRequest(TicketType.INFANT, 1);
        Booking separateRequestsBooking = new Booking(1L, adultOne, adultTwo, childOne, childTwo, infantOne, infantTwo);

        BigDecimal expectedPrice = new BigDecimal("86.98");
        Long expectedSeats = 4L;

        mockMvc.perform(post(BOOKINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(separateRequestsBooking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.totalPrice").value(86.98))
                .andExpect(jsonPath("$.seatsRequired").value(4))
                .andExpect(jsonPath("$.bookingReference").isNotEmpty());

        verify(seatReservationService).reserveSeats(1L, expectedSeats);
        verify(paymentService).debitAccount(1L, expectedPrice);
    }

    @Test
    void givenOneValidAndOneNegativeQuantityRequest_whenMakeBooking_thenReturnsBadRequest_andNothingIsBooked() throws Exception {
        // Assumption: any invalid TicketRequest invalidates the whole booking (no partial processing).
        Booking partiallyInvalidBooking = new Booking(1L,
                new TicketRequest(TicketType.ADULT, 5),
                new TicketRequest(TicketType.ADULT, -1));

        mockMvc.perform(post(BOOKINGS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partiallyInvalidBooking)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid ticket quantity")));

        verifyNoInteractions(paymentService, seatReservationService);
    }
}