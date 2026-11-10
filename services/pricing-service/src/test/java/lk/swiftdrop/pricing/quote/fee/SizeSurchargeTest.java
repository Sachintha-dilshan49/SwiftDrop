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

class SizeSurchargeTest {

    private final SizeSurcharge component = new SizeSurcharge();

    @ParameterizedTest(name = "{0} at its limit of {1} kg costs {2}")
    @CsvSource({
            "SMALL, 5, 0.00",
            "MEDIUM, 10, 100.00",
            "LARGE, 20, 250.00",
            "EXTRA_LARGE, 40, 500.00"
    })
    void chargesTheSurchargeOfEachSizeUpToItsLimit(ParcelSize size, String maxWeightKg, String surcharge) {
        FeeBreakdown.Builder breakdown = FeeBreakdown.builder();

        component.apply(parcel(maxWeightKg, size), breakdown);

        assertThat(breakdown.subtotal()).isEqualByComparingTo(surcharge).hasScaleOf(2);
    }

    @ParameterizedTest(name = "{0} rejects {1} kg")
    @CsvSource({
            "SMALL, 5.01, 5",
            "MEDIUM, 10.01, 10",
            "LARGE, 20.01, 20",
            "EXTRA_LARGE, 40.01, 40"
    })
    void rejectsAWeightOverTheSizeLimit(ParcelSize size, String weightKg, String max) {
        assertThatThrownBy(() -> component.apply(parcel(weightKg, size), FeeBreakdown.builder()))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getErrors())
                        .extracting("field").containsExactly("weightKg"))
                .hasMessage("A " + size + " parcel can weigh at most " + max + " kg. Choose a larger size.");
    }

    @Test
    void rejectsASizeWhoseRuleIsInactive() {
        FeeContext context = withRules(parcel("1", ParcelSize.LARGE),
                ruleBookWithout(rule -> rule.getRuleType() == RuleType.SIZE && rule.getRuleKey().equals("LARGE")));

        assertThatThrownBy(() -> component.apply(context, FeeBreakdown.builder()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("The LARGE parcel size is not available right now.");
    }

    private static FeeContext parcel(String weightKg, ParcelSize size) {
        return context(COLOMBO_SOUTH, COLOMBO_CENTRAL, weightKg, size, DeliverySpeed.STANDARD);
    }
}
