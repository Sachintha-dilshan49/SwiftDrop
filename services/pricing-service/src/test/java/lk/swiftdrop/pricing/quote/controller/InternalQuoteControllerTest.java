package lk.swiftdrop.pricing.quote.controller;

import lk.swiftdrop.common.error.FieldErrorDetail;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.common.internal.InternalTokenAuthenticationFilter;
import lk.swiftdrop.common.internal.InternalTokenTestContextCustomizerFactory;
import lk.swiftdrop.pricing.quote.dto.QuoteRequest;
import lk.swiftdrop.pricing.quote.service.QuoteService;
import lk.swiftdrop.pricing.support.TestJwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static lk.swiftdrop.pricing.support.QuoteSamples.WORKED_EXAMPLE_JSON;
import static lk.swiftdrop.pricing.support.QuoteSamples.workedExampleQuote;
import static lk.swiftdrop.pricing.support.QuoteSamples.workedExampleRequest;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Only parcel-service, with the shared internal token, gets a binding quote; a user JWT alone gets 401 (ADR-010). */
@WebMvcTest(controllers = InternalQuoteController.class)
class InternalQuoteControllerTest {

    private static final String QUOTE = "/internal/pricing/quote";
    private static final String TOKEN_HEADER = InternalTokenAuthenticationFilter.HEADER;
    private static final String TOKEN = InternalTokenTestContextCustomizerFactory.TEST_TOKEN;

    static final List<String> ROLES = TestJwts.ROLES;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuoteService quoteService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUpTokens() {
        TestJwts.acceptRoleTokens(jwtDecoder);
    }

    @Test
    @DisplayName("With the internal token, the worked example is quoted at 390.00 with its breakdown")
    void quoteWithInternalTokenReturnsTheBreakdown() throws Exception {
        given(quoteService.quote(any(QuoteRequest.class))).willReturn(workedExampleQuote());

        mockMvc.perform(post(QUOTE)
                        .header(TOKEN_HEADER, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WORKED_EXAMPLE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeType").value("NEIGHBOURING"))
                .andExpect(jsonPath("$.baseFee").value(350.00))
                .andExpect(jsonPath("$.weightFee").value(40.00))
                .andExpect(jsonPath("$.sizeFee").value(0.00))
                .andExpect(jsonPath("$.expressFee").value(100.00))
                .andExpect(jsonPath("$.discount").value(100.00))
                .andExpect(jsonPath("$.total").value(390.00))
                .andExpect(jsonPath("$.currency").value("LKR"));

        verify(quoteService).quote(workedExampleRequest());
    }

    @Test
    @DisplayName("Missing and malformed fields return 400 with field errors, before pricing runs")
    void invalidRequestReturnsBadRequestWithFieldErrors() throws Exception {
        mockMvc.perform(post(QUOTE)
                        .header(TOKEN_HEADER, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "pickupZone": "colombo south", "weightKg": -1, "size": "SMALL",
                                  "promoCode": "DROP 100" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/validation-error"))
                .andExpect(jsonPath("$.errors[*].field",
                        hasItems("pickupZone", "dropoffZone", "weightKg", "speed", "promoCode")));

        verifyNoInteractions(quoteService);
    }

    @Test
    @DisplayName("A request pricing rejects returns 400 with pricing's client-safe detail, which parcel-service passes on")
    void rejectedQuoteReturnsBadRequestWithTheReason() throws Exception {
        given(quoteService.quote(any(QuoteRequest.class))).willThrow(new ValidationException(
                "Promo code DROP100 has expired.", List.of(new FieldErrorDetail("promoCode", "is not valid"))));

        mockMvc.perform(post(QUOTE)
                        .header(TOKEN_HEADER, TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WORKED_EXAMPLE_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Promo code DROP100 has expired."))
                .andExpect(jsonPath("$.errors[0].field").value("promoCode"));
    }

    @Test
    void quoteWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post(QUOTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WORKED_EXAMPLE_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/unauthorized"));

        verifyNoInteractions(quoteService);
    }

    @Test
    void quoteWithAWrongInternalTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post(QUOTE)
                        .header(TOKEN_HEADER, "not-the-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WORKED_EXAMPLE_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(quoteService);
    }

    @ParameterizedTest(name = "a {0} JWT alone does not open the quote")
    @FieldSource("ROLES")
    void userJwtDoesNotOpenTheQuote(String role) throws Exception {
        mockMvc.perform(post(QUOTE)
                        .header("Authorization", TestJwts.bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WORKED_EXAMPLE_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(quoteService);
    }
}
