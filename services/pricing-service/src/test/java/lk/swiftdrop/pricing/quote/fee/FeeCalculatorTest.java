package lk.swiftdrop.pricing.quote.fee;

import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.pricing.promo.entity.PromoCode;
import lk.swiftdrop.pricing.quote.model.DeliverySpeed;
import lk.swiftdrop.pricing.quote.model.ParcelSize;
import lk.swiftdrop.pricing.quote.model.RouteType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static lk.swiftdrop.pricing.support.PricingFixtures.COLOMBO_CENTRAL;
import static lk.swiftdrop.pricing.support.PricingFixtures.COLOMBO_SOUTH;
import static lk.swiftdrop.pricing.support.PricingFixtures.GALLE;
import static lk.swiftdrop.pricing.support.PricingFixtures.KANDY;
import static lk.swiftdrop.pricing.support.PricingFixtures.KOTTE_RAJAGIRIYA;
import static lk.swiftdrop.pricing.support.PricingFixtures.NOW;
import static lk.swiftdrop.pricing.support.PricingFixtures.context;
import static lk.swiftdrop.pricing.support.PricingFixtures.drop100;
import static lk.swiftdrop.pricing.support.PricingFixtures.withPromo;
import static lk.swiftdrop.pricing.support.PricingFixtures.workedExample;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeeCalculatorTest {

    /** In formula order, as Spring injects them (checked separately below). */
    private final FeeCalculator calculator = new FeeCalculator(List.of(
            new RouteBaseFee(), new ExtraWeightFee(), new SizeSurcharge(), new SpeedSurcharge(), new PromoDiscount()));

    @Test
    @DisplayName("Worked example of architecture §8: COLOMBO_SOUTH -> COLOMBO_CENTRAL, 1.2 kg, SMALL, EXPRESS = 490.00")
    void workedExampleCosts490() {
        FeeBreakdown fee = calculator.calculate(workedExample());

        assertThat(fee).isEqualTo(new FeeBreakdown(RouteType.NEIGHBOURING, new BigDecimal("350.00"),
                new BigDecimal("40.00"), new BigDecimal("0.00"), new BigDecimal("100.00"), new BigDecimal("0.00"),
                new BigDecimal("490.00"), "LKR"));
    }

    @Test
    @DisplayName("Worked example with DROP100 = 390.00")
    void workedExampleWithDrop100Costs390() {
        FeeBreakdown fee = calculator.calculate(withPromo(workedExample(), "DROP100", drop100()));

        assertThat(fee).isEqualTo(new FeeBreakdown(RouteType.NEIGHBOURING, new BigDecimal("350.00"),
                new BigDecimal("40.00"), new BigDecimal("0.00"), new BigDecimal("100.00"), new BigDecimal("100.00"),
                new BigDecimal("390.00"), "LKR"));
    }

    static Stream<Arguments> everyRouteType() {
        return Stream.of(
                Arguments.of(COLOMBO_SOUTH, COLOMBO_SOUTH, RouteType.SAME_ZONE, "300.00"),
                Arguments.of(COLOMBO_SOUTH, COLOMBO_CENTRAL, RouteType.NEIGHBOURING, "350.00"),
                Arguments.of(COLOMBO_CENTRAL, KOTTE_RAJAGIRIYA, RouteType.COLOMBO_SUBURBS, "400.00"),
                Arguments.of(KANDY, GALLE, RouteType.INTER_CITY, "650.00"));
    }

    @ParameterizedTest(name = "{2}: 1 kg SMALL STANDARD costs {3}")
    @MethodSource("everyRouteType")
    void aOneKgStandardSmallParcelCostsTheBaseFee(ZoneRef pickup, ZoneRef dropoff, RouteType routeType, String total) {
        FeeBreakdown fee = calculator.calculate(context(pickup, dropoff, "1", ParcelSize.SMALL, DeliverySpeed.STANDARD));

        assertThat(fee.routeType()).isEqualTo(routeType);
        assertThat(fee.total()).isEqualTo(new BigDecimal(total));
    }

    @Test
    void addsEveryPartForAHeavyExpressInterCityParcel() {
        FeeBreakdown fee = calculator.calculate(context(KANDY, COLOMBO_CENTRAL, "18.3", ParcelSize.LARGE,
                DeliverySpeed.EXPRESS));

        // 650 + 18 extra kg * 40 + 250 + 100
        assertThat(fee.total()).isEqualTo(new BigDecimal("1720.00"));
        assertThat(fee.weightFee()).isEqualTo(new BigDecimal("720.00"));
    }

    @Test
    @DisplayName("The promo is capped at everything before it, including the surcharges")
    void promoLargerThanTheFeeGivesAZeroTotal() {
        PromoCode big = new PromoCode("BIG1000", new BigDecimal("1000.00"), NOW.minusSeconds(60), null, true);

        FeeBreakdown fee = calculator.calculate(withPromo(workedExample(), "BIG1000", big));

        assertThat(fee.discount()).isEqualTo(new BigDecimal("490.00"));
        assertThat(fee.total()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void expiredPromoRejectsTheWholeQuote() {
        PromoCode expired = new PromoCode("OCT50", new BigDecimal("50.00"),
                NOW.minusSeconds(7200), NOW.minusSeconds(3600), true);

        assertThatThrownBy(() -> calculator.calculate(withPromo(workedExample(), "OCT50", expired)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Promo code OCT50 has expired.");
    }

    @Test
    @DisplayName("Over 40 kg is reported before the size limit")
    void overFortyKgIsReportedFirst() {
        assertThatThrownBy(() -> calculator.calculate(
                context(COLOMBO_SOUTH, COLOMBO_CENTRAL, "45", ParcelSize.SMALL, DeliverySpeed.STANDARD)))
                .hasMessage("Parcels heavier than 40 kg cannot be delivered.");
    }

    @Test
    @DisplayName("Spring injects the components in formula order, with the promo last")
    void springOrdersTheComponentsAsTheFormula() {
        try (var context = new AnnotationConfigApplicationContext(PromoDiscount.class, SpeedSurcharge.class,
                SizeSurcharge.class, ExtraWeightFee.class, RouteBaseFee.class, FeeCalculator.class)) {
            FeeCalculator fromSpring = context.getBean(FeeCalculator.class);

            FeeBreakdown fee = fromSpring.calculate(withPromo(workedExample(), "DROP100", drop100()));

            assertThat(fee.total()).isEqualTo(new BigDecimal("390.00"));
            assertThat(context.getBeanProvider(FeeComponent.class).orderedStream().map(Object::getClass).toList())
                    .containsExactly(RouteBaseFee.class, ExtraWeightFee.class, SizeSurcharge.class,
                            SpeedSurcharge.class, PromoDiscount.class);
        }
    }
}
