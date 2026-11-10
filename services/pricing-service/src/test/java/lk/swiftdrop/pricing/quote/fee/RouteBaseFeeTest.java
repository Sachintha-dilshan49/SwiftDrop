package lk.swiftdrop.pricing.quote.fee;

import lk.swiftdrop.pricing.quote.model.DeliverySpeed;
import lk.swiftdrop.pricing.quote.model.ParcelSize;
import lk.swiftdrop.pricing.quote.model.RouteType;
import lk.swiftdrop.pricing.rule.entity.RuleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static lk.swiftdrop.pricing.support.PricingFixtures.COLOMBO_CENTRAL;
import static lk.swiftdrop.pricing.support.PricingFixtures.COLOMBO_SOUTH;
import static lk.swiftdrop.pricing.support.PricingFixtures.GALLE;
import static lk.swiftdrop.pricing.support.PricingFixtures.KANDY;
import static lk.swiftdrop.pricing.support.PricingFixtures.KOTTE_RAJAGIRIYA;
import static lk.swiftdrop.pricing.support.PricingFixtures.NUGEGODA_MAHARAGAMA;
import static lk.swiftdrop.pricing.support.PricingFixtures.context;
import static lk.swiftdrop.pricing.support.PricingFixtures.ruleBookWithout;
import static lk.swiftdrop.pricing.support.PricingFixtures.withRules;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteBaseFeeTest {

    private final RouteBaseFee component = new RouteBaseFee();

    static Stream<Arguments> routes() {
        return Stream.of(
                // INTER_CITY is checked first, so it wins even over the same zone
                Arguments.of(KANDY, KANDY, RouteType.INTER_CITY, "650.00"),
                Arguments.of(COLOMBO_SOUTH, GALLE, RouteType.INTER_CITY, "650.00"),
                Arguments.of(KANDY, KOTTE_RAJAGIRIYA, RouteType.INTER_CITY, "650.00"),
                Arguments.of(GALLE, KANDY, RouteType.INTER_CITY, "650.00"),
                Arguments.of(COLOMBO_SOUTH, COLOMBO_SOUTH, RouteType.SAME_ZONE, "300.00"),
                Arguments.of(KOTTE_RAJAGIRIYA, KOTTE_RAJAGIRIYA, RouteType.SAME_ZONE, "300.00"),
                Arguments.of(COLOMBO_SOUTH, COLOMBO_CENTRAL, RouteType.NEIGHBOURING, "350.00"),
                Arguments.of(KOTTE_RAJAGIRIYA, NUGEGODA_MAHARAGAMA, RouteType.NEIGHBOURING, "350.00"),
                Arguments.of(COLOMBO_CENTRAL, NUGEGODA_MAHARAGAMA, RouteType.COLOMBO_SUBURBS, "400.00"),
                Arguments.of(KOTTE_RAJAGIRIYA, COLOMBO_SOUTH, RouteType.COLOMBO_SUBURBS, "400.00"));
    }

    @ParameterizedTest(name = "{0} -> {1} is {2} at {3}")
    @MethodSource("routes")
    void chargesTheBaseFeeOfTheRouteType(ZoneRef pickup, ZoneRef dropoff, RouteType routeType, String baseFee) {
        FeeBreakdown.Builder breakdown = FeeBreakdown.builder();

        component.apply(context(pickup, dropoff, "1", ParcelSize.SMALL, DeliverySpeed.STANDARD), breakdown);

        FeeBreakdown fee = breakdown.build();
        assertThat(fee.routeType()).isEqualTo(routeType);
        assertThat(fee.baseFee()).isEqualByComparingTo(baseFee).hasScaleOf(2);
    }

    @Test
    @DisplayName("A missing route rule is a configuration error (500), not a customer error")
    void missingRouteRuleIsAConfigurationError() {
        var context = withRules(context(COLOMBO_SOUTH, COLOMBO_CENTRAL, "1", ParcelSize.SMALL, DeliverySpeed.STANDARD),
                ruleBookWithout(rule -> rule.getRuleType() == RuleType.ROUTE_BASE));

        assertThatThrownBy(() -> component.apply(context, FeeBreakdown.builder()))
                .isInstanceOf(IllegalStateException.class);
    }
}
