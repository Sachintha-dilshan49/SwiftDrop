package lk.swiftdrop.parcel.parcel.client;

import lk.swiftdrop.parcel.parcel.entity.RouteType;

import java.math.BigDecimal;

/** The binding fee from pricing-service: its breakdown and the total the customer pays. */
public record PricingQuote(
        RouteType routeType,
        BigDecimal baseFee,
        BigDecimal weightFee,
        BigDecimal sizeFee,
        BigDecimal expressFee,
        BigDecimal discount,
        BigDecimal total,
        String currency
) {
}
