package lk.swiftdrop.parcel.parcel.client;

import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.error.ServiceUnavailableException;
import lk.swiftdrop.common.error.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Gets the binding fee of a new parcel from pricing-service ({@code POST /internal/pricing/quote},
 * docs/architecture.md §8). The fee is never made up here: without a valid quote, no parcel is created.
 *
 * <p>The {@link RestClient} comes from {@code InternalRestClientFactory}, which adds the internal token
 * and turns 5xx answers, timeouts and connection failures into {@link ServiceUnavailableException}.
 */
@Component
public class PricingServiceClient {

    private static final Logger log = LoggerFactory.getLogger(PricingServiceClient.class);

    static final String QUOTE_PATH = "/internal/pricing/quote";
    static final String CURRENCY = "LKR";

    /** Pricing says the input cannot be priced, e.g. an expired promo code or a weight too high for the size. */
    private static final Set<Integer> REJECTED_INPUT_STATUSES = Set.of(400, 422);

    private static final String REJECTED_DETAIL =
            "The delivery fee cannot be calculated for this route, weight, size or promo code.";
    private static final String UNAVAILABLE_DETAIL =
            "The delivery fee could not be calculated right now. Please try again later.";

    private final RestClient restClient;

    public PricingServiceClient(@Qualifier("pricingServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * @throws ValidationException         400 when pricing rejects the input, with pricing's reason
     * @throws ServiceUnavailableException 503 when pricing is down, slow, refuses the call or returns an invalid quote
     */
    public PricingQuote getQuote(QuoteRequest request) {
        PricingQuote quote;
        try {
            quote = restClient.post()
                    .uri(QUOTE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(PricingQuote.class);
        } catch (HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            if (REJECTED_INPUT_STATUSES.contains(status)) {
                log.info("Pricing service rejected the quote request with HTTP {}", status);
                throw new ValidationException(rejectionDetail(e));
            }
            log.error("Pricing service refused the quote request with HTTP {}", status);
            throw new ServiceUnavailableException(UNAVAILABLE_DETAIL, e);
        } catch (NotFoundException | ConflictException e) {
            // 404 and 409 never mean the customer's input is wrong, so the customer gets a 503
            log.error("Pricing service answered the quote request with HTTP {}", e.getStatus().value());
            throw new ServiceUnavailableException(UNAVAILABLE_DETAIL, e);
        } catch (RestClientException e) {
            log.warn("Pricing service returned an unreadable quote: {}", e.getClass().getSimpleName());
            throw new ServiceUnavailableException(UNAVAILABLE_DETAIL, e);
        }

        if (!isValid(quote)) {
            log.error("Pricing service returned an invalid quote");
            throw new ServiceUnavailableException(UNAVAILABLE_DETAIL);
        }
        return quote;
    }

    /** Pricing's own reason, e.g. "Promo code DROP100 has expired"; a ProblemDetail's detail is client-safe. */
    private static String rejectionDetail(HttpClientErrorException e) {
        try {
            ProblemDetail problem = e.getResponseBodyAs(ProblemDetail.class);
            if (problem != null && problem.getDetail() != null && !problem.getDetail().isBlank()) {
                return problem.getDetail();
            }
        } catch (RuntimeException notAProblem) {
            // fall back to the generic reason
        }
        return REJECTED_DETAIL;
    }

    /** Every part is a non-negative LKR amount with at most 2 decimals, and the parts add up to the total. */
    private static boolean isValid(PricingQuote quote) {
        if (quote == null || quote.routeType() == null || !CURRENCY.equals(quote.currency())) {
            return false;
        }
        boolean amountsValid = Stream.of(quote.baseFee(), quote.weightFee(), quote.sizeFee(), quote.expressFee(),
                        quote.discount(), quote.total())
                .allMatch(amount -> amount != null && amount.signum() >= 0
                        && amount.stripTrailingZeros().scale() <= 2);
        if (!amountsValid) {
            return false;
        }
        BigDecimal sum = quote.baseFee().add(quote.weightFee()).add(quote.sizeFee()).add(quote.expressFee())
                .subtract(quote.discount());
        return sum.compareTo(quote.total()) == 0;
    }
}
