package lk.swiftdrop.pricing.rule.service;

import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.pricing.rule.dto.PricingRuleResponse;
import lk.swiftdrop.pricing.rule.dto.UpdatePricingRuleRequest;
import lk.swiftdrop.pricing.rule.entity.PricingRule;
import lk.swiftdrop.pricing.rule.entity.RuleType;
import lk.swiftdrop.pricing.rule.repository.PricingRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Admins change a rule's amount and whether it is active. Inactive SIZE and SPEED rules mean the option is not
 * offered; ROUTE_BASE and EXTRA_KG rules are needed by every quote and cannot be deactivated.
 */
@Service
public class PricingRuleService {

    private static final Logger log = LoggerFactory.getLogger(PricingRuleService.class);

    static final Set<RuleType> ALWAYS_ACTIVE = EnumSet.of(RuleType.ROUTE_BASE, RuleType.EXTRA_KG);

    private final PricingRuleRepository pricingRuleRepository;

    public PricingRuleService(PricingRuleRepository pricingRuleRepository) {
        this.pricingRuleRepository = pricingRuleRepository;
    }

    @Transactional(readOnly = true)
    public List<PricingRuleResponse> allRules() {
        return pricingRuleRepository.findAllByOrderByRuleTypeAscRuleKeyAsc().stream()
                .map(PricingRuleResponse::from)
                .toList();
    }

    @Transactional
    public PricingRuleResponse update(UUID id, UpdatePricingRuleRequest request) {
        PricingRule rule = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pricing rule " + id + " was not found."));
        if (!request.active() && ALWAYS_ACTIVE.contains(rule.getRuleType())) {
            throw new ConflictException("The " + rule.getRuleType() + " " + rule.getRuleKey()
                    + " rule is needed by every quote and cannot be deactivated.");
        }
        rule.update(request.amount(), request.active());
        pricingRuleRepository.flush();
        log.info("Pricing rule {} {} set to {} (active={})", rule.getRuleType(), rule.getRuleKey(),
                rule.getAmount(), rule.isActive());
        return PricingRuleResponse.from(rule);
    }
}
