package lk.swiftdrop.pricing.rule.service;

import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.pricing.rule.dto.PricingRuleResponse;
import lk.swiftdrop.pricing.rule.dto.UpdatePricingRuleRequest;
import lk.swiftdrop.pricing.rule.entity.PricingRule;
import lk.swiftdrop.pricing.rule.entity.RuleType;
import lk.swiftdrop.pricing.rule.repository.PricingRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static lk.swiftdrop.pricing.support.PricingFixtures.rule;
import static lk.swiftdrop.pricing.support.PricingFixtures.seedRules;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PricingRuleServiceTest {

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @InjectMocks
    private PricingRuleService service;

    @Test
    void listsEveryRule() {
        given(pricingRuleRepository.findAllByOrderByRuleTypeAscRuleKeyAsc()).willReturn(seedRules());

        List<PricingRuleResponse> rules = service.allRules();

        assertThat(rules).hasSize(12);
        assertThat(rules).filteredOn(r -> r.ruleType() == RuleType.SIZE)
                .allSatisfy(r -> assertThat(r.maxWeightKg()).isNotNull());
    }

    @Test
    void changesTheAmount() {
        PricingRule express = rule(RuleType.SPEED, "EXPRESS", "100.00", null);
        given(pricingRuleRepository.findById(express.getId())).willReturn(Optional.of(express));

        PricingRuleResponse updated = service.update(express.getId(),
                new UpdatePricingRuleRequest(new BigDecimal("150.00"), true));

        assertThat(updated.amount()).isEqualByComparingTo("150.00");
        assertThat(express.getAmount()).isEqualByComparingTo("150.00");
    }

    @ParameterizedTest(name = "{0} {1} can be deactivated")
    @CsvSource({"SIZE, EXTRA_LARGE", "SPEED, NEXT_DAY"})
    void sizeAndSpeedRulesCanBeDeactivated(RuleType type, String key) {
        PricingRule optional = rule(type, key, "0.00", type == RuleType.SIZE ? "40.00" : null);
        given(pricingRuleRepository.findById(optional.getId())).willReturn(Optional.of(optional));

        service.update(optional.getId(), new UpdatePricingRuleRequest(new BigDecimal("0.00"), false));

        assertThat(optional.isActive()).isFalse();
    }

    @ParameterizedTest(name = "{0} {1} cannot be deactivated")
    @CsvSource({"ROUTE_BASE, SAME_ZONE", "EXTRA_KG, PER_KG"})
    void rulesEveryQuoteNeedsCannotBeDeactivated(RuleType type, String key) {
        PricingRule required = rule(type, key, "40.00", null);
        given(pricingRuleRepository.findById(required.getId())).willReturn(Optional.of(required));

        assertThatThrownBy(() -> service.update(required.getId(),
                new UpdatePricingRuleRequest(new BigDecimal("40.00"), false)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("The " + type + " " + key + " rule is needed by every quote and cannot be deactivated.");
        assertThat(required.isActive()).isTrue();
    }

    @Test
    void unknownRuleIsNotFound() {
        UUID id = UUID.randomUUID();
        given(pricingRuleRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new UpdatePricingRuleRequest(BigDecimal.ONE, true)))
                .isInstanceOf(NotFoundException.class);
    }
}
