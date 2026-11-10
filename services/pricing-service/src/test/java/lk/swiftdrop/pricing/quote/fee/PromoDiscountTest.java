package lk.swiftdrop.pricing.quote.fee;

import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.pricing.promo.entity.PromoCode;
import lk.swiftdrop.pricing.quote.model.RouteType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static lk.swiftdrop.pricing.support.PricingFixtures.NOW;
import static lk.swiftdrop.pricing.support.PricingFixtures.drop100;
import static lk.swiftdrop.pricing.support.PricingFixtures.withNow;
import static lk.swiftdrop.pricing.support.PricingFixtures.withPromo;
import static lk.swiftdrop.pricing.support.PricingFixtures.workedExample;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromoDiscountTest {

    private final PromoDiscount component = new PromoDiscount();

    @Test
    void withoutAPromoCodeThereIsNoDiscount() {
        FeeBreakdown.Builder breakdown = feeOf("490.00");

        component.apply(workedExample(), breakdown);

        assertThat(breakdown.build().discount()).isEqualByComparingTo("0").hasScaleOf(2);
    }

    @Test
    void drop100Takes100Off() {
        FeeBreakdown.Builder breakdown = feeOf("490.00");

        component.apply(withPromo(workedExample(), "DROP100", drop100()), breakdown);

        FeeBreakdown fee = breakdown.build();
        assertThat(fee.discount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(fee.total()).isEqualTo(new BigDecimal("390.00"));
    }

    @Test
    @DisplayName("A promo larger than the fee is capped at the fee, so the total is 0 and never negative")
    void promoLargerThanTheFeeIsCappedAtTheFee() {
        PromoCode big = new PromoCode("BIG1000", new BigDecimal("1000.00"), NOW.minusSeconds(60), null, true);
        FeeBreakdown.Builder breakdown = feeOf("300.00");

        component.apply(withPromo(workedExample(), "BIG1000", big), breakdown);

        FeeBreakdown fee = breakdown.build();
        assertThat(fee.discount()).isEqualTo(new BigDecimal("300.00"));
        assertThat(fee.total()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void rejectsAnUnknownPromoCode() {
        assertRejected(withPromo(workedExample(), "NOPE", null), "Promo code NOPE is not valid.");
    }

    @Test
    @DisplayName("An inactive code gets the same answer as an unknown one")
    void rejectsAnInactivePromoCodeLikeAnUnknownOne() {
        PromoCode inactive = new PromoCode("OLD50", new BigDecimal("50.00"), NOW.minusSeconds(60), null, false);

        assertRejected(withPromo(workedExample(), "OLD50", inactive), "Promo code OLD50 is not valid.");
    }

    @Test
    void rejectsAnExpiredPromoCode() {
        PromoCode expired = new PromoCode("OCT50", new BigDecimal("50.00"),
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z"), true);

        assertRejected(withPromo(workedExample(), "OCT50", expired), "Promo code OCT50 has expired.");
    }

    @Test
    @DisplayName("A code has ended at its valid_to instant and has started at its valid_from instant")
    void validityBoundaries() {
        PromoCode promo = new PromoCode("WEEK", new BigDecimal("50.00"), NOW, NOW.plusSeconds(3600), true);

        FeeBreakdown.Builder atStart = feeOf("490.00");
        component.apply(withPromo(workedExample(), "WEEK", promo), atStart);
        assertThat(atStart.build().discount()).isEqualByComparingTo("50");

        assertRejected(withNow(withPromo(workedExample(), "WEEK", promo), NOW.plusSeconds(3600)),
                "Promo code WEEK has expired.");
    }

    @Test
    void rejectsAPromoCodeThatHasNotStartedYet() {
        PromoCode future = new PromoCode("XMAS", new BigDecimal("50.00"),
                Instant.parse("2026-12-20T00:00:00Z"), null, true);

        assertRejected(withPromo(workedExample(), "XMAS", future), "Promo code XMAS is not valid yet.");
    }

    private void assertRejected(FeeContext context, String detail) {
        assertThatThrownBy(() -> component.apply(context, feeOf("490.00")))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getErrors())
                        .extracting("field").containsExactly("promoCode"))
                .hasMessage(detail);
    }

    /** A breakdown whose subtotal is {@code amount}, as the earlier components would leave it. */
    private static FeeBreakdown.Builder feeOf(String amount) {
        return FeeBreakdown.builder()
                .routeType(RouteType.NEIGHBOURING)
                .baseFee(new BigDecimal(amount));
    }
}
