package lk.swiftdrop.pricing.quote.dto;

import lk.swiftdrop.pricing.quote.fee.FeeBreakdown;
import lk.swiftdrop.pricing.quote.model.RouteType;

import java.math.BigDecimal;

/**
 * The fee and how it is made up. The field names are a contract: parcel-service reads this as its
 * {@code PricingQuote} and stores every part with the parcel (docs/architecture.md §8).
 */
public record PricingQuoteResponse(
        RouteType routeType,
        BigDecimal baseFee,
        BigDecimal weightFee,
        BigDecimal sizeFee,
        BigDecimal expressFee,
        BigDecimal discount,
        BigDecimal total,
        String currency
) {

    public static PricingQuoteResponse from(FeeBreakdown fee) {
        return new PricingQuoteResponse(fee.routeType(), fee.baseFee(), fee.weightFee(), fee.sizeFee(),
                fee.expressFee(), fee.discount(), fee.total(), fee.currency());
    }
}
