package lk.swiftdrop.pricing.rule.dto;

import lk.swiftdrop.pricing.rule.entity.PricingRule;
import lk.swiftdrop.pricing.rule.entity.RuleType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** @param maxWeightKg only for SIZE rules, otherwise null */
public record PricingRuleResponse(
        UUID id,
        RuleType ruleType,
        String ruleKey,
        BigDecimal amount,
        String currency,
        BigDecimal maxWeightKg,
        boolean active,
        Instant updatedAt
) {

    public static PricingRuleResponse from(PricingRule rule) {
        return new PricingRuleResponse(rule.getId(), rule.getRuleType(), rule.getRuleKey(), rule.getAmount(),
                rule.getCurrency(), rule.getMaxWeightKg(), rule.isActive(), rule.getUpdatedAt());
    }
}
