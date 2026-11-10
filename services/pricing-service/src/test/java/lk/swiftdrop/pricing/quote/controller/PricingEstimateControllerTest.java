package lk.swiftdrop.pricing.quote.controller;

import lk.swiftdrop.common.error.FieldErrorDetail;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.pricing.quote.dto.QuoteRequest;
import lk.swiftdrop.pricing.quote.model.DeliverySpeed;
import lk.swiftdrop.pricing.quote.model.ParcelSize;
import lk.swiftdrop.pricing.quote.service.QuoteService;
import lk.swiftdrop.pricing.support.TestJwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static lk.swiftdrop.pricing.support.QuoteSamples.WORKED_EXAMPLE_QUERY;
import static lk.swiftdrop.pricing.support.QuoteSamples.workedExampleQuote;
import static lk.swiftdrop.pricing.support.QuoteSamples.workedExampleRequest;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The public estimate: no token needed, the same calculation and response shape as the internal quote. */
@WebMvcTest(controllers = PricingEstimateController.class)
class PricingEstimateControllerTest {

    private static final String ESTIMATE = "/api/v1/pricing/estimate";

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
    @DisplayName("A guest without a token gets the worked example estimated at 390.00")
    void guestGetsTheEstimateWithoutAToken() throws Exception {
        given(quoteService.quote(any(QuoteRequest.class))).willReturn(workedExampleQuote());

        mockMvc.perform(get(ESTIMATE + "?" + WORKED_EXAMPLE_QUERY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeType").value("NEIGHBOURING"))
                .andExpect(jsonPath("$.total").value(390.00))
                .andExpect(jsonPath("$.discount").value(100.00))
                .andExpect(jsonPath("$.currency").value("LKR"));

        verify(quoteService).quote(workedExampleRequest());
    }

    @Test
    @DisplayName("An expired or broken token on the public estimate is ignored instead of causing 401")
    void staleTokenIsIgnored() throws Exception {
        given(quoteService.quote(any(QuoteRequest.class))).willReturn(workedExampleQuote());

        mockMvc.perform(get(ESTIMATE + "?" + WORKED_EXAMPLE_QUERY)
                        .header("Authorization", "Bearer expired-or-broken"))
                .andExpect(status().isOk());
    }

    @Test
    void promoCodeIsOptional() throws Exception {
        given(quoteService.quote(any(QuoteRequest.class))).willReturn(workedExampleQuote());

        mockMvc.perform(get(ESTIMATE)
                        .param("pickupZone", "KANDY")
                        .param("dropoffZone", "GALLE")
                        .param("weightKg", "3")
                        .param("size", "MEDIUM")
                        .param("speed", "STANDARD"))
                .andExpect(status().isOk());

        verify(quoteService).quote(new QuoteRequest("KANDY", "GALLE", new BigDecimal("3"), ParcelSize.MEDIUM,
                DeliverySpeed.STANDARD, null));
    }

    @Test
    @DisplayName("Missing parameters return 400 with a field error each")
    void missingParametersReturnBadRequest() throws Exception {
        mockMvc.perform(get(ESTIMATE).param("pickupZone", "COLOMBO_SOUTH"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/validation-error"))
                .andExpect(jsonPath("$.errors[*].field", hasItems("dropoffZone", "weightKg", "size", "speed")));

        verifyNoInteractions(quoteService);
    }

    @Test
    @DisplayName("The old fromZone / toZone parameters are not accepted")
    void oldZoneParameterNamesAreRejected() throws Exception {
        mockMvc.perform(get(ESTIMATE + "?fromZone=COLOMBO_SOUTH&toZone=COLOMBO_CENTRAL&weightKg=1&size=SMALL&speed=STANDARD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItems("pickupZone", "dropoffZone")));
    }

    @Test
    @DisplayName("An unknown size is a 400 field error that does not expose Java type names")
    void unknownSizeIsBadRequest() throws Exception {
        mockMvc.perform(get(ESTIMATE + "?" + WORKED_EXAMPLE_QUERY.replace("size=SMALL", "size=HUGE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"))
                .andExpect(jsonPath("$.errors[0].message").value("must be one of SMALL, MEDIUM, LARGE, EXTRA_LARGE"))
                .andExpect(content().string(not(containsString("lk.swiftdrop"))))
                .andExpect(content().string(not(containsString("java."))));

        verifyNoInteractions(quoteService);
    }

    @Test
    void weightThatIsNotANumberIsBadRequest() throws Exception {
        mockMvc.perform(get(ESTIMATE + "?" + WORKED_EXAMPLE_QUERY.replace("weightKg=1.2", "weightKg=heavy")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("weightKg"))
                .andExpect(jsonPath("$.errors[0].message").value("must be a number"));

        verifyNoInteractions(quoteService);
    }

    @Test
    void rejectionDetailIsReturnedToTheGuest() throws Exception {
        given(quoteService.quote(any(QuoteRequest.class))).willThrow(new ValidationException(
                "A SMALL parcel can weigh at most 5 kg. Choose a larger size.",
                List.of(new FieldErrorDetail("weightKg", "must be at most 5 kg for size SMALL"))));

        mockMvc.perform(get(ESTIMATE + "?" + WORKED_EXAMPLE_QUERY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A SMALL parcel can weigh at most 5 kg. Choose a larger size."));
    }
}
