package lk.swiftdrop.pricing.rule.controller;

import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.pricing.rule.dto.PricingRuleResponse;
import lk.swiftdrop.pricing.rule.dto.UpdatePricingRuleRequest;
import lk.swiftdrop.pricing.rule.entity.RuleType;
import lk.swiftdrop.pricing.rule.service.PricingRuleService;
import lk.swiftdrop.pricing.support.TestJwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminPricingRuleController.class)
class AdminPricingRuleControllerTest {

    private static final String RULES = "/api/v1/admin/pricing-rules";
    private static final String ADMIN = TestJwts.bearer("ADMIN");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PricingRuleService pricingRuleService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUpTokens() {
        TestJwts.acceptRoleTokens(jwtDecoder);
    }

    @Test
    void adminListsTheRulesWithSizeLimits() throws Exception {
        given(pricingRuleService.allRules()).willReturn(List.of(
                rule(RuleType.SIZE, "MEDIUM", "100.00", "10.00", true),
                rule(RuleType.SPEED, "EXPRESS", "100.00", null, true)));

        mockMvc.perform(get(RULES).header("Authorization", ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ruleType").value("SIZE"))
                .andExpect(jsonPath("$[0].ruleKey").value("MEDIUM"))
                .andExpect(jsonPath("$[0].maxWeightKg").value(10.00))
                .andExpect(jsonPath("$[1].maxWeightKg").doesNotExist());
    }

    @Test
    void adminChangesAnAmount() throws Exception {
        given(pricingRuleService.update(eq(id), any(UpdatePricingRuleRequest.class)))
                .willReturn(rule(RuleType.SPEED, "EXPRESS", "150.00", null, true));

        mockMvc.perform(put(RULES + "/" + id).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 150.00, "active": true }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(150.00));

        verify(pricingRuleService).update(id, new UpdatePricingRuleRequest(new BigDecimal("150.00"), true));
    }

    @Test
    void negativeOrMissingValuesAreBadRequest() throws Exception {
        mockMvc.perform(put(RULES + "/" + id).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": -5 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItems("amount", "active")));

        verifyNoInteractions(pricingRuleService);
    }

    @Test
    void deactivatingARequiredRuleIsAConflict() throws Exception {
        given(pricingRuleService.update(eq(id), any(UpdatePricingRuleRequest.class))).willThrow(new ConflictException(
                "The ROUTE_BASE SAME_ZONE rule is needed by every quote and cannot be deactivated."));

        mockMvc.perform(put(RULES + "/" + id).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 300.00, "active": false }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("The ROUTE_BASE SAME_ZONE rule is needed by every quote and cannot be deactivated."));
    }

    @Test
    void unknownRuleIsNotFound() throws Exception {
        given(pricingRuleService.update(eq(id), any(UpdatePricingRuleRequest.class)))
                .willThrow(new NotFoundException("Pricing rule " + id + " was not found."));

        mockMvc.perform(put(RULES + "/" + id).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 1.00, "active": true }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void rulesCannotBeCreated() throws Exception {
        mockMvc.perform(post(RULES).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void withoutATokenIsUnauthorized() throws Exception {
        mockMvc.perform(get(RULES)).andExpect(status().isUnauthorized());

        verifyNoInteractions(pricingRuleService);
    }

    @ParameterizedTest(name = "{0} is forbidden")
    @ValueSource(strings = {"CUSTOMER", "MERCHANT", "COURIER", "DISPATCHER"})
    void nonAdminIsForbidden(String role) throws Exception {
        mockMvc.perform(get(RULES).header("Authorization", TestJwts.bearer(role)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(RULES + "/" + id).header("Authorization", TestJwts.bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 0.00, "active": true }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(pricingRuleService);
    }

    private PricingRuleResponse rule(RuleType type, String key, String amount, String maxWeightKg, boolean active) {
        return new PricingRuleResponse(id, type, key, new BigDecimal(amount), "LKR",
                maxWeightKg == null ? null : new BigDecimal(maxWeightKg), active, null);
    }
}
