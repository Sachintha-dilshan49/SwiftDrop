package lk.swiftdrop.pricing.quote.fee;

import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.pricing.quote.model.DeliverySpeed;
import lk.swiftdrop.pricing.quote.model.ParcelSize;
import lk.swiftdrop.pricing.rule.entity.RuleType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static lk.swiftdrop.pricing.support.PricingFixtures.COLOMBO_CENTRAL;
import static lk.swiftdrop.pricing.support.PricingFixtures.COLOMBO_SOUTH;
import static lk.swiftdrop.pricing.support.PricingFixtures.context;
import static lk.swiftdrop.pricing.support.PricingFixtures.ruleBookWithout;
import static lk.swiftdrop.pricing.support.PricingFixtures.withRules;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpeedSurchargeTest {

    private final SpeedSurcharge component = new SpeedSurcharge();

    @ParameterizedTest(name = "{0} costs {1}")
    @CsvSource({"EXPRESS, 100.00", "STANDARD, 0.00", "NEXT_DAY, 0.00"})
    void onlyExpressCostsExtra(DeliverySpeed speed, String surcharge) {
        FeeBreakdown.Builder breakdown = FeeBreakdown.builder();

        component.apply(context(COLOMBO_SOUTH, COLOMBO_CENTRAL, "1", ParcelSize.SMALL, speed), breakdown);

        assertThat(breakdown.subtotal()).isEqualByComparingTo(surcharge).hasScaleOf(2);
    }

    @Test
    void rejectsASpeedWhoseRuleIsInactive() {
        FeeContext context = withRules(context(COLOMBO_SOUTH, COLOMBO_CENTRAL, "1", ParcelSize.SMALL, DeliverySpeed.NEXT_DAY),
                ruleBookWithout(rule -> rule.getRuleType() == RuleType.SPEED && rule.getRuleKey().equals("NEXT_DAY")));

        assertThatThrownBy(() -> component.apply(context, FeeBreakdown.builder()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("The NEXT_DAY delivery speed is not available right now.");
    }
}
