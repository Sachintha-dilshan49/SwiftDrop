package lk.swiftdrop.pricing.quote.controller;

import jakarta.validation.Valid;
import lk.swiftdrop.pricing.quote.dto.PricingQuoteResponse;
import lk.swiftdrop.pricing.quote.dto.QuoteRequest;
import lk.swiftdrop.pricing.quote.service.QuoteService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The binding fee of a new parcel, for parcel-service only. {@code common} requires {@code X-Internal-Token}
 * on {@code /internal/**}; a user JWT does not open it (ADR-010).
 */
@RestController
@RequestMapping("/internal/pricing")
public class InternalQuoteController {

    private final QuoteService quoteService;

    public InternalQuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @PostMapping("/quote")
    public PricingQuoteResponse quote(@Valid @RequestBody QuoteRequest request) {
        return quoteService.quote(request);
    }
}
