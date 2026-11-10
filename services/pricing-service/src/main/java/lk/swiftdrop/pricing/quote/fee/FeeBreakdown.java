package lk.swiftdrop.pricing.quote.fee;

import lk.swiftdrop.pricing.quote.model.RouteType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * How a fee is made up. Every amount is LKR with scale 2, and
 * {@code total = baseFee + weightFee + sizeFee + expressFee - discount}, never below 0.
 */
public record FeeBreakdown(
        RouteType routeType,
        BigDecimal baseFee,
        BigDecimal weightFee,
        BigDecimal sizeFee,
        BigDecimal expressFee,
        BigDecimal discount,
        BigDecimal total,
        String currency
) {

    public static final String CURRENCY = "LKR";

    public static Builder builder() {
        return new Builder();
    }

    /** Filled in by the fee components in order; parts no component set are 0. */
    public static final class Builder {

        private RouteType routeType;
        private BigDecimal baseFee = money(BigDecimal.ZERO);
        private BigDecimal weightFee = money(BigDecimal.ZERO);
        private BigDecimal sizeFee = money(BigDecimal.ZERO);
        private BigDecimal expressFee = money(BigDecimal.ZERO);
        private BigDecimal discount = money(BigDecimal.ZERO);

        private Builder() {
        }

        public Builder routeType(RouteType routeType) {
            this.routeType = routeType;
            return this;
        }

        public Builder baseFee(BigDecimal amount) {
            this.baseFee = money(amount);
            return this;
        }

        public Builder weightFee(BigDecimal amount) {
            this.weightFee = money(amount);
            return this;
        }

        public Builder sizeFee(BigDecimal amount) {
            this.sizeFee = money(amount);
            return this;
        }

        public Builder expressFee(BigDecimal amount) {
            this.expressFee = money(amount);
            return this;
        }

        public Builder discount(BigDecimal amount) {
            this.discount = money(amount);
            return this;
        }

        /** The fee before the promo discount. */
        public BigDecimal subtotal() {
            return baseFee.add(weightFee).add(sizeFee).add(expressFee);
        }

        public FeeBreakdown build() {
            Objects.requireNonNull(routeType, "routeType");
            BigDecimal total = subtotal().subtract(discount);
            if (total.signum() < 0) {
                throw new IllegalStateException("The discount is larger than the fee");
            }
            return new FeeBreakdown(routeType, baseFee, weightFee, sizeFee, expressFee, discount, total, CURRENCY);
        }

        private static BigDecimal money(BigDecimal amount) {
            return Objects.requireNonNull(amount, "amount").setScale(2, RoundingMode.HALF_UP);
        }
    }
}
