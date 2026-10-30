package lk.swiftdrop.merchant.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import lk.swiftdrop.merchant.exception.DownstreamServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class ParcelServiceClientTest {

    private WireMockServer wireMockServer;
    private ParcelServiceClient parcelServiceClient;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();

        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(Duration.ofSeconds(2));

        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + wireMockServer.port())
                .requestFactory(factory)
                .build();

        parcelServiceClient = new ParcelServiceClient(restClient);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    @DisplayName("createParcel sends correct payload to /internal/parcels and returns parcel response")
    void createParcelSuccess() {
        UUID merchantId = UUID.randomUUID();
        UUID parcelId = UUID.randomUUID();

        wireMockServer.stubFor(post(urlEqualTo("/internal/parcels"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
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
                "Kasun Silva", "0771234567", "SMALL", new BigDecimal("1.50")
        );

        CreateParcelInternalResponse response = parcelServiceClient.createParcel(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(parcelId);
        assertThat(response.trackingNo()).isEqualTo("SD-998877");
        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.feeAmount()).isEqualByComparingTo(new BigDecimal("450.00"));

        wireMockServer.verify(postRequestedFor(urlEqualTo("/internal/parcels")));
    }

    @Test
    @DisplayName("createParcel throws DownstreamServiceException when parcel-service returns 500 error")
    void createParcelDownstreamError() {
        wireMockServer.stubFor(post(urlEqualTo("/internal/parcels"))
                .willReturn(aResponse().withStatus(500)));

        CreateParcelInternalRequest request = new CreateParcelInternalRequest(
                UUID.randomUUID(), "123 Main St", "456 High St",
                "Kasun Silva", "0771234567", "SMALL", new BigDecimal("1.50")
        );

        assertThatThrownBy(() -> parcelServiceClient.createParcel(request))
                .isInstanceOf(DownstreamServiceException.class)
                .hasMessageContaining("Failed to create parcel in parcel service");
    }

    @Test
    @DisplayName("createParcel throws DownstreamServiceException when parcel-service times out")
    void createParcelTimeout() {
        wireMockServer.stubFor(post(urlEqualTo("/internal/parcels"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(3000)));

        CreateParcelInternalRequest request = new CreateParcelInternalRequest(
                UUID.randomUUID(), "123 Main St", "456 High St",
                "Kasun Silva", "0771234567", "SMALL", new BigDecimal("1.50")
        );

        assertThatThrownBy(() -> parcelServiceClient.createParcel(request))
                .isInstanceOf(DownstreamServiceException.class)
                .hasMessageContaining("unavailable or timed out");
    }
}
