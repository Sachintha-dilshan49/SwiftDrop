package lk.swiftdrop.parcel.parcel.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import lk.swiftdrop.common.error.ServiceUnavailableException;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.common.internal.InternalRestClientFactory;
import lk.swiftdrop.parcel.parcel.entity.DeliverySpeed;
import lk.swiftdrop.parcel.parcel.entity.ParcelSize;
import lk.swiftdrop.parcel.parcel.entity.RouteType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.math.BigDecimal;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.notContaining;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The client against a stubbed pricing-service, following the quote contract in docs/architecture.md §8. */
class PricingServiceClientTest {

    private static final String TOKEN = "test-internal-token";
    private static final String QUOTE_PATH = "/internal/pricing/quote";

    /** The worked example from docs/architecture.md §8. */
    private static final String VALID_QUOTE = """
            {"routeType": "NEIGHBOURING", "baseFee": 350.00, "weightFee": 40.00, "sizeFee": 0.00,
             "expressFee": 100.00, "discount": 100.00, "total": 390.00, "currency": "LKR"}
            """;

    private static final QuoteRequest REQUEST = new QuoteRequest("COLOMBO_SOUTH", "COLOMBO_CENTRAL",
            new BigDecimal("1.20"), ParcelSize.SMALL, DeliverySpeed.EXPRESS, "DROP100");

    private WireMockServer pricing;
    private PricingServiceClient client;

    @BeforeEach
    void setUp() {
        pricing = new WireMockServer(options().dynamicPort());
        pricing.start();
        client = clientFor(pricing.port());
    }

    @AfterEach
    void tearDown() {
        pricing.stop();
    }

    @Test
    @DisplayName("Returns the quote from pricing-service, sent with the internal token")
    void returnsQuoteFromPricingService() {
        pricing.stubFor(post(urlPathEqualTo(QUOTE_PATH))
                .withHeader("X-Internal-Token", equalTo(TOKEN))
                .withRequestBody(equalToJson("""
                        {"pickupZone": "COLOMBO_SOUTH", "dropoffZone": "COLOMBO_CENTRAL", "weightKg": 1.20,
                         "size": "SMALL", "speed": "EXPRESS", "promoCode": "DROP100"}
                        """))
                .willReturn(okJson(VALID_QUOTE)));

        PricingQuote quote = client.getQuote(REQUEST);

        assertThat(quote.routeType()).isEqualTo(RouteType.NEIGHBOURING);
        assertThat(quote.baseFee()).isEqualByComparingTo("350.00");
        assertThat(quote.weightFee()).isEqualByComparingTo("40.00");
        assertThat(quote.expressFee()).isEqualByComparingTo("100.00");
        assertThat(quote.discount()).isEqualByComparingTo("100.00");
        assertThat(quote.total()).isEqualByComparingTo("390.00");
        assertThat(quote.currency()).isEqualTo("LKR");
    }

    @Test
    @DisplayName("Leaves promoCode out of the request when there is none")
    void omitsMissingPromoCode() {
        pricing.stubFor(post(urlPathEqualTo(QUOTE_PATH)).willReturn(okJson(VALID_QUOTE)));

        client.getQuote(new QuoteRequest("COLOMBO_SOUTH", "COLOMBO_CENTRAL", new BigDecimal("1.20"),
                ParcelSize.SMALL, DeliverySpeed.STANDARD, null));

        pricing.verify(postRequestedFor(urlPathEqualTo(QUOTE_PATH)).withRequestBody(notContaining("promoCode")));
    }

    @Test
    @DisplayName("A total of 0 after a promo code is a valid quote")
    void acceptsZeroTotalAfterPromo() {
        pricing.stubFor(post(urlPathEqualTo(QUOTE_PATH)).willReturn(okJson("""
                {"routeType": "SAME_ZONE", "baseFee": 300.00, "weightFee": 0.00, "sizeFee": 0.00,
                 "expressFee": 0.00, "discount": 300.00, "total": 0.00, "currency": "LKR"}
                """)));

        assertThat(client.getQuote(REQUEST).total()).isZero();
    }

    @ParameterizedTest(name = "HTTP {0} from pricing becomes a 400 validation error")
    @ValueSource(ints = {400, 422})
    void rejectedInputBecomesValidationError(int status) {
        pricing.stubFor(post(urlPathEqualTo(QUOTE_PATH)).willReturn(aResponse().withStatus(status)));

        assertThatThrownBy(() -> client.getQuote(REQUEST)).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("A 400 keeps pricing's reason, so the customer knows what to change")
    void rejectionKeepsPricingsReason() {
        pricing.stubFor(post(urlPathEqualTo(QUOTE_PATH)).willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/problem+json")
                .withBody("{\"title\": \"Validation error\", \"status\": 400, "
                        + "\"detail\": \"Promo code DROP100 has expired\"}")));

        assertThatThrownBy(() -> client.getQuote(REQUEST))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Promo code DROP100 has expired");
    }

    @ParameterizedTest(name = "HTTP {0} from pricing becomes 503")
    @ValueSource(ints = {401, 403, 404, 409, 429, 500, 502, 503})
    void otherErrorsBecomeServiceUnavailable(int status) {
        pricing.stubFor(post(urlPathEqualTo(QUOTE_PATH)).willReturn(aResponse().withStatus(status)));

        assertThatThrownBy(() -> client.getQuote(REQUEST)).isInstanceOf(ServiceUnavailableException.class);
    }

    @ParameterizedTest(name = "Invalid quote {0} becomes 503")
    @ValueSource(strings = {
            "{}",
            // no route type, or one pricing does not use
            "{\"baseFee\": 350.00, \"weightFee\": 0, \"sizeFee\": 0, \"expressFee\": 0, \"discount\": 0, \"total\": 350.00, \"currency\": \"LKR\"}",
            "{\"routeType\": \"OVERSEAS\", \"baseFee\": 350.00, \"weightFee\": 0, \"sizeFee\": 0, \"expressFee\": 0, \"discount\": 0, \"total\": 350.00, \"currency\": \"LKR\"}",
            // a missing, negative or over-precise amount
            "{\"routeType\": \"SAME_ZONE\", \"baseFee\": 300.00, \"weightFee\": 0, \"sizeFee\": 0, \"expressFee\": 0, \"discount\": 0, \"currency\": \"LKR\"}",
            "{\"routeType\": \"SAME_ZONE\", \"baseFee\": 300.00, \"weightFee\": 0, \"sizeFee\": 0, \"expressFee\": 0, \"discount\": -10.00, \"total\": 310.00, \"currency\": \"LKR\"}",
            "{\"routeType\": \"SAME_ZONE\", \"baseFee\": 300.005, \"weightFee\": 0, \"sizeFee\": 0, \"expressFee\": 0, \"discount\": 0, \"total\": 300.005, \"currency\": \"LKR\"}",
            // another currency, or parts that do not add up to the total
            "{\"routeType\": \"SAME_ZONE\", \"baseFee\": 300.00, \"weightFee\": 0, \"sizeFee\": 0, \"expressFee\": 0, \"discount\": 0, \"total\": 300.00, \"currency\": \"USD\"}",
            "{\"routeType\": \"SAME_ZONE\", \"baseFee\": 300.00, \"weightFee\": 40.00, \"sizeFee\": 0, \"expressFee\": 0, \"discount\": 0, \"total\": 300.00, \"currency\": \"LKR\"}"
    })
    void invalidQuoteBecomesServiceUnavailable(String body) {
        pricing.stubFor(post(urlPathEqualTo(QUOTE_PATH)).willReturn(okJson(body)));

        assertThatThrownBy(() -> client.getQuote(REQUEST)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    @DisplayName("A body that is not JSON becomes 503")
    void malformedBodyBecomesServiceUnavailable() {
        pricing.stubFor(post(urlPathEqualTo(QUOTE_PATH))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("<html>oops")));

        assertThatThrownBy(() -> client.getQuote(REQUEST)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    @DisplayName("A response slower than the read timeout becomes 503")
    void slowResponseBecomesServiceUnavailable() {
        pricing.stubFor(post(urlPathEqualTo(QUOTE_PATH)).willReturn(okJson(VALID_QUOTE).withFixedDelay(1_000)));

        assertThatThrownBy(() -> client.getQuote(REQUEST)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    @DisplayName("A dropped connection becomes 503")
    void droppedConnectionBecomesServiceUnavailable() {
        pricing.stubFor(post(urlPathEqualTo(QUOTE_PATH))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> client.getQuote(REQUEST)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    @DisplayName("pricing-service being down becomes 503")
    void unreachableServiceBecomesServiceUnavailable() {
        int port = pricing.port();
        pricing.stop();

        assertThatThrownBy(() -> clientFor(port).getQuote(REQUEST)).isInstanceOf(ServiceUnavailableException.class);
    }

    /** The real internal client (token, error mapping) with short timeouts so the tests stay fast. */
    private static PricingServiceClient clientFor(int port) {
        SimpleClientHttpRequestFactory shortTimeouts = new SimpleClientHttpRequestFactory();
        shortTimeouts.setConnectTimeout(Duration.ofMillis(500));
        shortTimeouts.setReadTimeout(Duration.ofMillis(300));
        return new PricingServiceClient(new InternalRestClientFactory(TOKEN)
                .builder("pricing-service", "http://localhost:" + port)
                .requestFactory(shortTimeouts)
                .build());
    }
}
