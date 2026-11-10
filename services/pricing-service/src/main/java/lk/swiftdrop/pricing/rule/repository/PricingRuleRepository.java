package lk.swiftdrop.pricing.rule.repository;

import lk.swiftdrop.pricing.rule.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PricingRuleRepository extends JpaRepository<PricingRule, UUID> {

    List<PricingRule> findByActiveTrue();

    List<PricingRule> findAllByOrderByRuleTypeAscRuleKeyAsc();
}
