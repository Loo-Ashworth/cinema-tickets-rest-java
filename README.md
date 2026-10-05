# Cinema Tickets Exercise

```
Implementation Built with Java Version 23.0.2
```

This repo contains my solution to the task.

## Assumptions based on Constraints and Instructions
| Constraint                                                   | My Assumptions                           |
|--------------------------------------------------------------|------------------------------------------|
| There are 3 types of tickets i.e. INFANT, CHILD, and ADULT   | Null/Other Types are Invalid             |
| Only a maximum of 25 tickets that can be purchased at a time | Less than 1 and more than 25 are Invalid |
| All Accounts with an id greater than zero are valid          | Null, Negative and Zero are Invalid      |

## Potential Ambiguity

### Max 25 Tickets Or Seats?
The notes state only a maximum of 25 tickets can be purchased at a time. My implementation takes this literally and does not assume the maximum number of seats bookable is 25.

(For example 25 Adults with 25 Infants would still only be 25 seats but 50 tickets, and is rejected.)

I would typically double-check a detail such as this.

## Additional Assumptions
| Assumption                                                                  | Rationale                                                                    | Test                                                                                                                                                                                                            |
|-----------------------------------------------------------------------------|------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Any invalid TicketRequest in TicketRequests[] should Invalidate the Booking | Partial Processing is not explicitly defined                                 | [Unit](src/test/java/uk/gov/dwp/engineering/recruitment/validation/TicketRequestsValidatorTest.java#L27), [Acceptance](src/test/java/uk/gov/dwp/engineering/recruitment/CinemaTicketsControllerTest.java#L339)  |
| Negative Ticket Quantity is Invalid (e.g. -1)                               | Would result in corrupted counts                                             | [Unit](src/test/java/uk/gov/dwp/engineering/recruitment/validation/TicketRequestsValidatorTest.java#L17), [Acceptance](src/test/java/uk/gov/dwp/engineering/recruitment/CinemaTicketsControllerTest.java#L339)  |
| Seats should be reserved before funds are debited                           | We would not want to charge the customer before confirming seat availability | [Unit](src/test/java/uk/gov/dwp/engineering/recruitment/CinemaTicketsServiceImplTest.java#L155)                                                                                                                 |
| Infants are free and do not occupy a seat                                   | They sit on an adult's lap                                                   | [Unit](src/test/java/uk/gov/dwp/engineering/recruitment/calculation/SeatNumberCalculatorTest.java#L39), [Unit](src/test/java/uk/gov/dwp/engineering/recruitment/calculation/TicketPriceCalculatorTest.java#L34) |

### Not Implemented
Based on the following guidance:
```
-   The `PaymentService` implementation is an external provider with no defects.
-   You do not need to worry about how the actual payment happens or integrating that service.
-   The payment will always go through once a payment request has been made to the `PaymentService`.
-   The `SeatReservationService` implementation is an external provider with no defects.
-   You do not need to worry about how the seat reservation algorithm works or integrating that service.
-   The seats will always be reserved once a reservation request has been made to the `SeatReservationService`.
```
I have not implemented:
* Success (StatusCode) validation for PaymentService.debitAccount() - Assumptions explain this will always be successful at this stage of development.
* Success (StatusCode) validation for SeatReservationService.reserveSeats() - As above.

## Design

The service is just orchestrating. Each responsibility (validation / calculation) lives in its own class injected as a dependency to the service

| Component                | Responsibility                                                                       |
|--------------------------|--------------------------------------------------------------------------------------|
| AccountValidator         | Validates the account ID                                                             |
| TicketRequestsValidator  | Counts tickets across all requests, then validates the business rules                |
| TicketPriceCalculator    | Calculates the total price from the ticket counts                                    |
| SeatNumberCalculator     | Calculates the seats required (infants excluded)                                     |
| CinemaTicketsServiceImpl | Orchestrates the above, reserves seats, takes payment, returns a BookingConfirmation |

### Validation Rules
Account and ticket validation rules are each defined as a list of rules (a condition plus a message). Adding or removing a rule is a one-line change making them easy to extend.

Rule order is deliberate and fail-fast: the first violated rule is the one reported. 

### Booking flow
Implemented using TDD with BDD-style given_when_then naming and the following fail-fast sequence:

1) Validate the account ID
2) Count the tickets across all requests and validate the business rules
3) Reject as soon as any rule is violated, before any third-party service is called
4) Calculate the total price and the seats required
5) Reserve seats first, so we never charge for seats we don't have
6) Debit the account
7) Return a BookingConfirmation containing the account ID, total price, seats required and a generated booking reference

### Response Shape
I extended the BookingConfirmation to be more informative without breaking the existing single arg constructor.

## Testing Approach
Each layer has a distinct job, to avoid excessive duplicated coverage:

| Layer      | Test class                   | Purpose                                                                                                                             |
|------------|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| Unit       | AccountValidatorTest         | Null, zero, negative and valid account IDs                                                                                          |
| Unit       | TicketRequestsValidatorTest  | Counting, invalid requests, and every business rule and valid/boundary cases                                                        |
| Unit       | TicketPriceCalculatorTest    | Price per ticket type, infants free                                                                                                 |
| Unit       | SeatNumberCalculatorTest     | Seats per ticket type, infants excluded                                                                                             |
| Unit       | CinemaTicketsServiceImplTest | Orchestration only, with dependencies mocked: call order, no seats reserved or  charge debited on any failure and response contents |
| Acceptance | CinemaTicketsControllerTest  | Business rules and rejected requests over REST via MockMvc, wired with the real classes                                             |

### Invalid Requests
* Account ID of -1Long [Unit Test](src/test/java/uk/gov/dwp/engineering/recruitment/validation/AccountValidatorTest.java#L29)
* Account ID of 0 [Unit Test](src/test/java/uk/gov/dwp/engineering/recruitment/validation/AccountValidatorTest.java#L21)
* Account ID of Null [Unit Test](src/test/java/uk/gov/dwp/engineering/recruitment/validation/AccountValidatorTest.java#L13)
* Ticket Quantity Total Less than 0 [Unit Test](src/test/java/uk/gov/dwp/engineering/recruitment/validation/TicketRequestsValidatorTest.java#L17)
* Ticket Quantity Total of 0 [Unit Test](src/test/java/uk/gov/dwp/engineering/recruitment/validation/TicketRequestsValidatorTest.java#L37)
* Ticket Quantity total Greater than 25 [Unit Test](src/test/java/uk/gov/dwp/engineering/recruitment/validation/TicketRequestsValidatorTest.java#L48)
* Multiple TicketRequests Exceeding 25 [Unit Test](src/test/java/uk/gov/dwp/engineering/recruitment/validation/TicketRequestsValidatorTest.java#L59)
* Infant Ticket Only [Unit Test](src/test/java/uk/gov/dwp/engineering/recruitment/validation/TicketRequestsValidatorTest.java#L71)
* Any Quantity of Infant Ticket without an equivalent number of Adult Ticket [Unit Test](src/test/java/uk/gov/dwp/engineering/recruitment/validation/TicketRequestsValidatorTest.java#L82)
* Any Quantity of Child Ticket without at least 1 Adult Ticket [Unit Test](src/test/java/uk/gov/dwp/engineering/recruitment/validation/TicketRequestsValidatorTest.java#L94)
* Combination of Child Ticket and Infant Ticket without Adult Ticket [Unit Test](src/test/java/uk/gov/dwp/engineering/recruitment/validation/TicketRequestsValidatorTest.java#L105)

### Minimum Valid Cases
* 1 Adult [Unit Test](src/test/java/uk/gov/dwp/engineering/recruitment/validation/TicketRequestsValidatorTest.java#L117)
* 1 Adult & 1 Infant [Unit Test](src/test/java/uk/gov/dwp/engineering/recruitment/validation/TicketRequestsValidatorTest.java#L125)
* 1 Adult & 1 Child [Unit Test](src/test/java/uk/gov/dwp/engineering/recruitment/validation/TicketRequestsValidatorTest.java#L134)
* 1 Adult, 1 Infant & 1 Child [Unit Test](src/test/java/uk/gov/dwp/engineering/recruitment/validation/TicketRequestsValidatorTest.java#L143)
* 2 Adults on Separate Requests, 2 Children on Separate Requests, 2 Infants in Separate Requests [Unit Test](src/test/java/uk/gov/dwp/engineering/recruitment/validation/TicketRequestsValidatorTest.java#L153)
* Exactly 25 Adult Tickets [Unit Test](src/test/java/uk/gov/dwp/engineering/recruitment/validation/TicketRequestsValidatorTest.java#L169)
* Exactly 25 Mixed Tickets [Unit Test](src/test/java/uk/gov/dwp/engineering/recruitment/validation/TicketRequestsValidatorTest.java#L177)

### Business rules and task guidance covered
Covered via MockMvc on the controller, using the real service, validators and calculators with only the third-party services mocked.

| Business Rule                                                                                              | Test                                                                                                                                                                                                          |
|------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| There are 3 types of tickets i.e. INFANT, CHILD, and ADULT. (Enforced by enum, null checked)               | [Acceptance](src/test/java/uk/gov/dwp/engineering/recruitment/CinemaTicketsControllerTest.java#L158)                                                                                                          |
| The ticket prices are based on the type of ticket.                                                         | [Unit](src/test/java/uk/gov/dwp/engineering/recruitment/calculation/TicketPriceCalculatorTest.java#L43), [Acceptance](src/test/java/uk/gov/dwp/engineering/recruitment/CinemaTicketsControllerTest.java#L239) |
| The ticket purchaser declares how many and what type of tickets they want to buy.                          | Various tests e.g. [1](src/test/java/uk/gov/dwp/engineering/recruitment/CinemaTicketsControllerTest.java#L312)                                                                                                |
| Multiple tickets can be purchased at any given time.                                                       | [Acceptance](src/test/java/uk/gov/dwp/engineering/recruitment/CinemaTicketsControllerTest.java#L312)                                                                                                          |
| Only a maximum of 25 tickets can be purchased at a time.                                                   | [Acceptance](src/test/java/uk/gov/dwp/engineering/recruitment/CinemaTicketsControllerTest.java#L130)                                                                                                          |
| An INFANT does not pay for a ticket and is not allocated a seat.                                           | [Acceptance](src/test/java/uk/gov/dwp/engineering/recruitment/CinemaTicketsControllerTest.java#L172)                                                                                                          |
| CHILD and INFANT tickets cannot be purchased without purchasing an ADULT ticket.                           | [Acceptance](src/test/java/uk/gov/dwp/engineering/recruitment/CinemaTicketsControllerTest.java#L195)                                                                                                          |
| Calculates the correct amount and makes a payment request to the PaymentService.                           | [Acceptance](src/test/java/uk/gov/dwp/engineering/recruitment/CinemaTicketsControllerTest.java#L239)                                                                                                          |
| Calculates the correct number of seats and makes a seat reservation request to the SeatReservationService. | [Acceptance](src/test/java/uk/gov/dwp/engineering/recruitment/CinemaTicketsControllerTest.java#L239)                                                                                                          |
| Rejects any invalid ticket purchase requests.                                                              | Various tests e.g. [1](src/test/java/uk/gov/dwp/engineering/recruitment/CinemaTicketsControllerTest.java#L195)                                                                                                |

### Controller test approach
The instructions state:

```
Ensure appropriate error handling and testing is implemented
for the ‘CinemaTicketsController’ referring to all business
rules and rejected ticket requests.
```

I would normally mock and unit test a controller and include an @SpringBoot test with a rest template to test end-to-end journeys such as this, but the ask is to include coverage at the controller level, MockMvc allows us to simulate this with actual wiring instead of mocks.

To demonstrate the exception handling and error paths, the BAD_REQUEST handling was added to RestExceptionHandler. The controller tests pass and the CinemaTicketsServiceImpl and its real validators and calculators, to MockMvc. MockMvc simulates HTTP requests and wires the actual ControllerAdvice without the weight of starting a web server.

The third-party services remain mocked, which lets the controller tests verify price and seat calculations as well as the JSON response body (account ID, total price, seats required, booking reference).

[One](src/test/java/uk/gov/dwp/engineering/recruitment/CinemaTicketsControllerTest.java#L65) additional test mocks CinemaTicketsService entirely and confirms the REST call delegates to it and returns its confirmation, which isolates the controller's own responsibility.

## Potential Future Iterations
1) Add a @SpringBootTest integration test calling the controller over HTTP, to cover the full context and exception handling.
2) TicketType (were it permitted to modify) may be a better source of truth for prices than the map in TicketPriceCalculator.
3) Handle failures from third-party services, such as outages and error responses.

## Maven Run Commands

Wrapped with Maven Wrapper for convenience.

#### Run Tests
```bash
./mvnw test
```
#### Run Application
```bash
./mvnw spring-boot:run
```