package lk.swiftdrop.pricing.support;

import lk.swiftdrop.pricing.quote.dto.PricingQuoteResponse;
import lk.swiftdrop.pricing.quote.dto.QuoteRequest;
import lk.swiftdrop.pricing.quote.model.DeliverySpeed;
import lk.swiftdrop.pricing.quote.model.ParcelSize;
import lk.swiftdrop.pricing.quote.model.RouteType;

import java.math.BigDecimal;

/** The worked example of docs/architecture.md §8 as a request and its response. */
public final class QuoteSamples {

    /** The body parcel-service sends, as JSON. */
    public static final String WORKED_EXAMPLE_JSON = """
            { "pickupZone": "COLOMBO_SOUTH", "dropoffZone": "COLOMBO_CENTRAL", "weightKg": 1.2,
              "size": "SMALL", "speed": "EXPRESS", "promoCode": "DROP100" }
            """;

    public static final String WORKED_EXAMPLE_QUERY =
            "pickupZone=COLOMBO_SOUTH&dropoffZone=COLOMBO_CENTRAL&weightKg=1.2&size=SMALL&speed=EXPRESS&promoCode=DROP100";

    private QuoteSamples() {
    }

    public static QuoteRequest workedExampleRequest() {
        return new QuoteRequest("COLOMBO_SOUTH", "COLOMBO_CENTRAL", new BigDecimal("1.2"), ParcelSize.SMALL,
                DeliverySpeed.EXPRESS, "DROP100");
    }

    public static PricingQuoteResponse workedExampleQuote() {
        return new PricingQuoteResponse(RouteType.NEIGHBOURING, new BigDecimal("350.00"), new BigDecimal("40.00"),
                new BigDecimal("0.00"), new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("390.00"),
                "LKR");
    }
}
