package lk.swiftdrop.pricing;

import lk.swiftdrop.pricing.support.AbstractPostgresIT;
import lk.swiftdrop.pricing.support.TestJwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static lk.swiftdrop.pricing.support.QuoteSamples.WORKED_EXAMPLE_QUERY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Admin changes go to PostgreSQL and the next quote uses them: amounts are never hard-coded. Every test puts
 * the seed data back afterwards.
 */
@AutoConfigureMockMvc
class AdminChangesIT extends AbstractPostgresIT {

    private static final String ADMIN = TestJwts.bearer("ADMIN");
    private static final String ESTIMATE = "/api/v1/pricing/estimate?" + WORKED_EXAMPLE_QUERY;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUpTokens() {
        TestJwts.acceptRoleTokens(jwtDecoder);
    }

    @AfterEach
    void restoreSeedData() {
        jdbc.update("UPDATE pricing_rules SET amount = 100.00, active = true "
                + "WHERE rule_type = 'SPEED' AND rule_key = 'EXPRESS'");
        jdbc.update("UPDATE zones SET active = true WHERE code = 'COLOMBO_CENTRAL'");
        jdbc.update("DELETE FROM promo_codes WHERE code <> 'DROP100'");
        jdbc.update("DELETE FROM zones WHERE code = 'MATARA'");
    }

    @Test
    @DisplayName("A new EXPRESS amount changes the next estimate")
    void changedRuleAmountIsUsedByTheNextQuote() throws Exception {
        UUID express = jdbc.queryForObject(
                "SELECT id FROM pricing_rules WHERE rule_type = 'SPEED' AND rule_key = 'EXPRESS'", UUID.class);

        mockMvc.perform(put("/api/v1/admin/pricing-rules/" + express).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 175.00, "active": true }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get(ESTIMATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expressFee").value(175.00))
                .andExpect(jsonPath("$.total").value(465.00));
    }

    @Test
    void deactivatedSpeedIsNotOffered() throws Exception {
        UUID express = jdbc.queryForObject(
                "SELECT id FROM pricing_rules WHERE rule_type = 'SPEED' AND rule_key = 'EXPRESS'", UUID.class);

        mockMvc.perform(put("/api/v1/admin/pricing-rules/" + express).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 100.00, "active": false }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get(ESTIMATE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The EXPRESS delivery speed is not available right now."));
    }

    @Test
    void deletedZoneIsNoLongerServedOrListed() throws Exception {
        UUID central = jdbc.queryForObject("SELECT id FROM zones WHERE code = 'COLOMBO_CENTRAL'", UUID.class);

        mockMvc.perform(delete("/api/v1/admin/zones/" + central).header("Authorization", ADMIN))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ESTIMATE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Drop-off zone COLOMBO_CENTRAL is not served."));
        mockMvc.perform(get("/api/v1/pricing/zones"))
                .andExpect(jsonPath("$.length()").value(8));
        mockMvc.perform(get("/api/v1/admin/zones/" + central).header("Authorization", ADMIN))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void newZoneAndPromoCodeCanBeQuotedRightAway() throws Exception {
        mockMvc.perform(post("/api/v1/admin/zones").header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "MATARA", "name": "Matara", "group": "INTER_CITY",
                                  "districts": ["Matara", "Weligama"] }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.districts.length()").value(2));
        mockMvc.perform(post("/api/v1/admin/promo-codes").header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "south25", "discountAmount": 25.00, "validFrom": "2026-01-01T00:00:00Z" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SOUTH25"));

        // 650 inter-city + 40 + 0 + 0 standard - 25
        mockMvc.perform(get("/api/v1/pricing/estimate?pickupZone=COLOMBO_SOUTH&dropoffZone=MATARA&weightKg=2"
                        + "&size=SMALL&speed=STANDARD&promoCode=SOUTH25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeType").value("INTER_CITY"))
                .andExpect(jsonPath("$.total").value(665.00));
    }
}
