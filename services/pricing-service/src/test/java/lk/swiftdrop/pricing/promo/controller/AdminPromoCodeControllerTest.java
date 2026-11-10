package lk.swiftdrop.pricing.promo.controller;

import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.FieldErrorDetail;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.pricing.promo.dto.CreatePromoCodeRequest;
import lk.swiftdrop.pricing.promo.dto.PromoCodeResponse;
import lk.swiftdrop.pricing.promo.dto.UpdatePromoCodeRequest;
import lk.swiftdrop.pricing.promo.service.PromoCodeService;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminPromoCodeController.class)
class AdminPromoCodeControllerTest {

    private static final String PROMOS = "/api/v1/admin/promo-codes";
    private static final String ADMIN = TestJwts.bearer("ADMIN");
    private static final Instant FROM = Instant.parse("2026-12-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2027-01-01T00:00:00Z");
    private static final String NEW_PROMO = """
            { "code": "xmas50", "discountAmount": 50.00,
              "validFrom": "2026-12-01T00:00:00Z", "validTo": "2027-01-01T00:00:00Z" }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PromoCodeService promoCodeService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUpTokens() {
        TestJwts.acceptRoleTokens(jwtDecoder);
    }

    @Test
    void adminListsPromoCodes() throws Exception {
        given(promoCodeService.allPromoCodes()).willReturn(List.of(promo("DROP100", null)));

        mockMvc.perform(get(PROMOS).header("Authorization", ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("DROP100"))
                .andExpect(jsonPath("$[0].validTo").doesNotExist());
    }

    @Test
    void adminGetsOnePromoCode() throws Exception {
        given(promoCodeService.promoCode(id)).willReturn(promo("XMAS50", TO));

        mockMvc.perform(get(PROMOS + "/" + id).header("Authorization", ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validTo").value("2027-01-01T00:00:00Z"));
    }

    @Test
    void adminCreatesAPromoCodeWith201AndLocation() throws Exception {
        given(promoCodeService.create(any(CreatePromoCodeRequest.class))).willReturn(promo("XMAS50", TO));

        mockMvc.perform(post(PROMOS).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_PROMO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", PROMOS + "/" + id))
                .andExpect(jsonPath("$.code").value("XMAS50"))
                .andExpect(jsonPath("$.discountAmount").value(50.00));

        verify(promoCodeService).create(new CreatePromoCodeRequest("xmas50", new BigDecimal("50.00"), FROM, TO, null));
    }

    @Test
    void createWithInvalidFieldsReturnsBadRequest() throws Exception {
        mockMvc.perform(post(PROMOS).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "XMAS 50", "discountAmount": 0 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItems("code", "discountAmount", "validFrom")));

        verifyNoInteractions(promoCodeService);
    }

    @Test
    void periodEndingBeforeItStartsIsBadRequest() throws Exception {
        given(promoCodeService.create(any(CreatePromoCodeRequest.class))).willThrow(new ValidationException(
                "The end of the validity period must be after its start.",
                List.of(new FieldErrorDetail("validTo", "must be after validFrom"))));

        mockMvc.perform(post(PROMOS).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_PROMO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("validTo"));
    }

    @Test
    void duplicateCodeIsAConflict() throws Exception {
        given(promoCodeService.create(any(CreatePromoCodeRequest.class)))
                .willThrow(new ConflictException("Promo code XMAS50 already exists."));

        mockMvc.perform(post(PROMOS).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_PROMO))
                .andExpect(status().isConflict());
    }

    @Test
    void adminUpdatesAPromoCode() throws Exception {
        given(promoCodeService.update(eq(id), any(UpdatePromoCodeRequest.class))).willReturn(promo("XMAS50", null));

        mockMvc.perform(put(PROMOS + "/" + id).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "discountAmount": 75.00, "validFrom": "2026-12-01T00:00:00Z", "active": false }
                                """))
                .andExpect(status().isOk());

        verify(promoCodeService).update(id, new UpdatePromoCodeRequest(new BigDecimal("75.00"), FROM, null, false));
    }

    @Test
    void adminDeletesAPromoCodeWith204() throws Exception {
        mockMvc.perform(delete(PROMOS + "/" + id).header("Authorization", ADMIN))
                .andExpect(status().isNoContent());

        verify(promoCodeService).delete(id);
    }

    @Test
    void withoutATokenIsUnauthorized() throws Exception {
        mockMvc.perform(get(PROMOS)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(PROMOS + "/" + id)).andExpect(status().isUnauthorized());

        verifyNoInteractions(promoCodeService);
    }

    @ParameterizedTest(name = "{0} is forbidden")
    @ValueSource(strings = {"CUSTOMER", "MERCHANT", "COURIER", "DISPATCHER"})
    void nonAdminIsForbidden(String role) throws Exception {
        mockMvc.perform(get(PROMOS).header("Authorization", TestJwts.bearer(role)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(PROMOS).header("Authorization", TestJwts.bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_PROMO))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(PROMOS + "/" + id).header("Authorization", TestJwts.bearer(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(promoCodeService);
    }

    private PromoCodeResponse promo(String code, Instant validTo) {
        return new PromoCodeResponse(id, code, new BigDecimal("50.00"), "LKR", FROM, validTo, true,
                Instant.parse("2026-11-01T00:00:00Z"), null);
    }
}
