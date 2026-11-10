package lk.swiftdrop.pricing.quote.controller;

import lk.swiftdrop.common.internal.InternalTokenAuthenticationFilter;
import lk.swiftdrop.common.internal.InternalTokenTestContextCustomizerFactory;
import lk.swiftdrop.pricing.quote.dto.PricingQuoteResponse;
import lk.swiftdrop.pricing.quote.dto.QuoteRequest;
import lk.swiftdrop.pricing.quote.service.QuoteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static lk.swiftdrop.pricing.support.QuoteSamples.WORKED_EXAMPLE_JSON;
import static lk.swiftdrop.pricing.support.QuoteSamples.workedExampleQuote;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * The quote response is read by parcel-service as its {@code PricingQuote} record
 * (services/parcel-service/src/main/java/lk/swiftdrop/parcel/parcel/client/PricingQuote.java). That module
 * cannot be a test dependency here, so its field names are listed below: change them only together with
 * parcel-service.
 */
@WebMvcTest(controllers = InternalQuoteController.class)
class QuoteResponseContractTest {

    /** The components of parcel-service's PricingQuote, in declaration order. */
    static final List<String> PARCEL_SERVICE_PRICING_QUOTE_FIELDS = List.of(
            "routeType", "baseFee", "weightFee", "sizeFee", "expressFee", "discount", "total", "currency");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuoteService quoteService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("The serialised quote has exactly the field names of parcel-service's PricingQuote")
    void serialisedQuoteHasExactlyTheFieldsParcelServiceReads() throws Exception {
        given(quoteService.quote(any(QuoteRequest.class))).willReturn(workedExampleQuote());

        String body = mockMvc.perform(post("/internal/pricing/quote")
                        .header(InternalTokenAuthenticationFilter.HEADER, InternalTokenTestContextCustomizerFactory.TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WORKED_EXAMPLE_JSON))
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> json = JsonMapper.builder().build().readValue(body, LinkedHashMap.class);
        assertThat(json.keySet()).containsExactlyInAnyOrderElementsOf(PARCEL_SERVICE_PRICING_QUOTE_FIELDS);
        // Amounts are JSON numbers with two decimals, which parcel-service checks
        assertThat(body).contains("\"total\":390.00", "\"sizeFee\":0.00", "\"currency\":\"LKR\"");
    }

    @Test
    void responseRecordDeclaresTheSameComponentsInTheSameOrder() {
        List<String> components = Arrays.stream(PricingQuoteResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertThat(components).containsExactlyElementsOf(PARCEL_SERVICE_PRICING_QUOTE_FIELDS);
    }
}
