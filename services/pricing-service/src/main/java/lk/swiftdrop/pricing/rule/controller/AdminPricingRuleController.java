package lk.swiftdrop.pricing.rule.controller;

import jakarta.validation.Valid;
import lk.swiftdrop.pricing.rule.dto.PricingRuleResponse;
import lk.swiftdrop.pricing.rule.dto.UpdatePricingRuleRequest;
import lk.swiftdrop.pricing.rule.service.PricingRuleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Pricing rules, admins only. Rules are created by migrations; admins edit the amount and the active flag. */
@RestController
@RequestMapping("/api/v1/admin/pricing-rules")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPricingRuleController {

    private final PricingRuleService pricingRuleService;

    public AdminPricingRuleController(PricingRuleService pricingRuleService) {
        this.pricingRuleService = pricingRuleService;
    }

    @GetMapping
    public List<PricingRuleResponse> list() {
        return pricingRuleService.allRules();
    }

    @PutMapping("/{id}")
    public PricingRuleResponse update(@PathVariable UUID id, @Valid @RequestBody UpdatePricingRuleRequest request) {
        return pricingRuleService.update(id, request);
    }
}
