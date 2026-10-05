package uk.gov.dwp.engineering.recruitment.validation;

import org.springframework.stereotype.Component;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Component
public class AccountValidator {

    private record Rule(Predicate<Long> isViolated, String message) {}

    private static final List<Rule> RULES = List.of(
            new Rule(Objects::isNull, "Account ID must not be null"),
            new Rule(accountId -> accountId <= 0L, "Account ID must be greater than zero")
    );

    public void validateAccountId(Long accountId) throws InvalidBookingException {
        for (Rule rule : RULES) {
            if (rule.isViolated().test(accountId)) {
                throw new InvalidBookingException(rule.message());
            }
        }
    }
}