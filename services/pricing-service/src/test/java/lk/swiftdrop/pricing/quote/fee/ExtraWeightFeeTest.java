package lk.swiftdrop.pricing.quote.fee;

import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.pricing.quote.model.DeliverySpeed;
import lk.swiftdrop.pricing.quote.model.ParcelSize;
import lk.swiftdrop.pricing.rule.entity.RuleType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static lk.swiftdrop.pricing.support.PricingFixtures.COLOMBO_CENTRAL;
import static lk.swiftdrop.pricing.support.PricingFixtures.COLOMBO_SOUTH;
import static lk.swiftdrop.pricing.support.PricingFixtures.context;
import static lk.swiftdrop.pricing.support.PricingFixtures.rule;
import static lk.swiftdrop.pricing.support.PricingFixtures.ruleBookWithout;
import static lk.swiftdrop.pricing.support.PricingFixtures.withRules;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtraWeightFeeTest {

    private final ExtraWeightFee component = new ExtraWeightFee();

    @ParameterizedTest(name = "{0} kg costs {1}")
    @CsvSource({
            "0.1, 0.00",
            "1.0, 0.00",
            "1.01, 40.00",
            "1.2, 40.00",
            "2.0, 40.00",
            "2.01, 80.00",
            "5, 160.00",
            "40, 1560.00"
    })
    void firstKgIsFreeAndEveryStartedKgAfterItCosts40(String weightKg, String weightFee) {
        FeeBreakdown.Builder breakdown = FeeBreakdown.builder();

        component.apply(parcelOf(weightKg), breakdown);

        assertThat(breakdown.subtotal()).isEqualByComparingTo(weightFee).hasScaleOf(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"40.01", "41", "100"})
    void rejectsParcelsOver40Kg(String weightKg) {
        assertThatThrownBy(() -> component.apply(parcelOf(weightKg), FeeBreakdown.builder()))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getErrors())
                        .extracting("field").containsExactly("weightKg"))
                .hasMessage("Parcels heavier than 40 kg cannot be delivered.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void rejectsAWeightThatIsNotPositive(String weightKg) {
        assertThatThrownBy(() -> component.apply(parcelOf(weightKg), FeeBreakdown.builder()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("The weight must be greater than 0 kg.");
    }

    @Test
    void usesThePerKgAmountFromTheRules() {
        PricingRules rules = PricingRules.of(List.of(rule(RuleType.EXTRA_KG, "PER_KG", "55.50", null)));
        FeeBreakdown.Builder breakdown = FeeBreakdown.builder();

        component.apply(withRules(parcelOf("3.5"), rules), breakdown);

        assertThat(breakdown.subtotal()).isEqualTo(new BigDecimal("166.50"));
    }

    @Test
    void missingPerKgRuleIsAConfigurationError() {
        FeeContext context = withRules(parcelOf("3"), ruleBookWithout(rule -> rule.getRuleType() == RuleType.EXTRA_KG));

        assertThatThrownBy(() -> component.apply(context, FeeBreakdown.builder()))
                .isInstanceOf(IllegalStateException.class);
    }

    private static FeeContext parcelOf(String weightKg) {
        return context(COLOMBO_SOUTH, COLOMBO_CENTRAL, weightKg, ParcelSize.EXTRA_LARGE, DeliverySpeed.STANDARD);
    }
}
