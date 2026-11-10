package lk.swiftdrop.pricing.quote.controller;

import jakarta.validation.Valid;
import lk.swiftdrop.pricing.quote.dto.PricingQuoteResponse;
import lk.swiftdrop.pricing.quote.dto.QuoteRequest;
import lk.swiftdrop.pricing.quote.service.QuoteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The expected fee before booking, for guests, the booking screen and the AI assistant. Public (no token) and
 * rate-limited per client IP by the gateway. The parcel's fee is still the binding quote taken at booking.
 */
@RestController
@RequestMapping("/api/v1/pricing")
public class PricingEstimateController {

    private final QuoteService quoteService;

    public PricingEstimateController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    /** {@code ?pickupZone&dropoffZone&weightKg&size&speed&promoCode}, the same fields as the quote. */
    @GetMapping("/estimate")
    public PricingQuoteResponse estimate(@Valid @ModelAttribute QuoteRequest request) {
        return quoteService.quote(request);
    }
}
