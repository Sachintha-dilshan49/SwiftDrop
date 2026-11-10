package lk.swiftdrop.merchant.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.math.BigDecimal;
import java.util.UUID;
import lk.swiftdrop.common.error.ServiceUnavailableException;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.common.internal.InternalRestClientFactory;
import lk.swiftdrop.merchant.exception.DownstreamServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class ParcelServiceClientTest {

    private WireMockServer wireMockServer;
    private ParcelServiceClient parcelServiceClient;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();

        InternalRestClientFactory clients = new InternalRestClientFactory("test-internal-token");
        RestClient restClient = clients.create("parcel-service", "http://localhost:" + wireMockServer.port());

        parcelServiceClient = new ParcelServiceClient(restClient);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    @DisplayName("createParcel sends correct payload and X-Internal-Token to /internal/parcels and returns 201 Created")
    void createParcelSuccess() {
        UUID merchantId = UUID.randomUUID();
        UUID parcelId = UUID.randomUUID();

        wireMockServer.stubFor(post(urlEqualTo("/internal/parcels"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("X-Internal-Token", equalTo("test-internal-token"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format("""
                                {
                                    "id": "%s",
                                    "trackingNo": "SD-998877",
                                    "status": "CREATED",
                                    "feeAmount": 450.00,
                                    "feeCurrency": "LKR"
                                }
                                """, parcelId))));

        CreateParcelInternalRequest request = new CreateParcelInternalRequest(
                merchantId, "123 Main St, Colombo", "456 High St, Kandy",
                "Kasun Silva", "0771234567", "kasun@example.com",
                "COLOMBO_SOUTH", "COLOMBO_CENTRAL", "SMALL", "STANDARD",
                new BigDecimal("1.50")
        );

        CreateParcelInternalResponse response = parcelServiceClient.createParcel(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(parcelId);
        assertThat(response.trackingNo()).isEqualTo("SD-998877");
        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.feeAmount()).isEqualByComparingTo(new BigDecimal("450.00"));

        wireMockServer.verify(postRequestedFor(urlEqualTo("/internal/parcels"))
                .withHeader("X-Internal-Token", equalTo("test-internal-token")));
    }

    @Test
    @DisplayName("createParcel maps downstream 400 Bad Request to ValidationException with detail")
    void createParcelValidationFailure() {
        wireMockServer.stubFor(post(urlEqualTo("/internal/parcels"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/problem+json")
                        .withBody("""
                                {
                                    "type": "https://api.swiftdrop.lk/errors/validation-error",
                                    "title": "Validation failed",
                                    "status": 400,
                                    "detail": "Weight 12.0 kg does not fit size SMALL (max 5 kg)"
                                }
                                """)));

        CreateParcelInternalRequest request = new CreateParcelInternalRequest(
                UUID.randomUUID(), "123 Main St", "456 High St",
                "Kasun Silva", "0771234567", "kasun@example.com",
                "COLOMBO_SOUTH", "COLOMBO_CENTRAL", "SMALL", "STANDARD",
                new BigDecimal("12.00")
        );

        assertThatThrownBy(() -> parcelServiceClient.createParcel(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Weight 12.0 kg does not fit size SMALL");
    }

    @Test
    @DisplayName("createParcel throws DownstreamServiceException or ServiceUnavailableException when parcel-service returns 503 or 500")
    void createParcelDownstreamError() {
        wireMockServer.stubFor(post(urlEqualTo("/internal/parcels"))
                .willReturn(aResponse().withStatus(503)));

        CreateParcelInternalRequest request = new CreateParcelInternalRequest(
                UUID.randomUUID(), "123 Main St", "456 High St",
                "Kasun Silva", "0771234567", "kasun@example.com",
                "COLOMBO_SOUTH", "COLOMBO_CENTRAL", "SMALL", "STANDARD",
                new BigDecimal("1.50")
        );

        assertThatThrownBy(() -> parcelServiceClient.createParcel(request))
                .satisfies(e -> assertThat(e instanceof DownstreamServiceException || e instanceof ServiceUnavailableException).isTrue());
    }

    @Test
    @DisplayName("createParcel handles timeout gracefully")
    void createParcelTimeout() {
        wireMockServer.stubFor(post(urlEqualTo("/internal/parcels"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withFixedDelay(7000)));

        CreateParcelInternalRequest request = new CreateParcelInternalRequest(
                UUID.randomUUID(), "123 Main St", "456 High St",
                "Kasun Silva", "0771234567", "kasun@example.com",
                "COLOMBO_SOUTH", "COLOMBO_CENTRAL", "SMALL", "STANDARD",
                new BigDecimal("1.50")
        );

        assertThatThrownBy(() -> parcelServiceClient.createParcel(request))
                .satisfies(e -> assertThat(e instanceof DownstreamServiceException || e instanceof ServiceUnavailableException).isTrue());
    }
}
