package lk.swiftdrop.dispatch.parcel;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.UUID;
import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.error.ServiceUnavailableException;
import lk.swiftdrop.common.error.SwiftDropException;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.common.internal.InternalRestClientFactory;
import lk.swiftdrop.dispatch.config.RestClientConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Runs the client against a fake parcel-service over real HTTP. */
class ParcelServiceClientTest {

    @RegisterExtension
    static WireMockExtension parcelService = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final String INTERNAL_TOKEN = "test-internal-token";
    private static final UUID PARCEL_ID = UUID.randomUUID();
    private static final UUID COURIER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String PATH = "/internal/parcels/" + PARCEL_ID + "/status";
    private static final ParcelStatusUpdate UPDATE = new ParcelStatusUpdate(
            ParcelStatus.FAILED, COURIER_ID, "Gate locked", null, "Nobody at home", USER_ID, "COURIER");

    private ParcelServiceClient client;

    @BeforeEach
    void setUp() {
        client = clientFor(parcelService.baseUrl());
    }

    @Test
    void sendsStatusChangeWithTheInternalTokenAsJson() {
        parcelService.stubFor(patch(urlEqualTo(PATH)).willReturn(aResponse().withStatus(200)));

        client.updateStatus(PARCEL_ID, UPDATE);

        parcelService.verify(patchRequestedFor(urlEqualTo(PATH))
                .withHeader("X-Internal-Token", equalTo(INTERNAL_TOKEN))
                .withHeader("Authorization", absent())
                .withRequestBody(equalToJson("""
                        {"status":"FAILED","courierId":"%s","note":"Gate locked","proofReference":null,
                         "failureReason":"Nobody at home","changedBy":"%s","changedByRole":"COURIER"}
                        """.formatted(COURIER_ID, USER_ID))));
    }

    @Test
    void rejectedTransitionKeepsParcelServicesProblem() {
        parcelService.stubFor(patch(urlEqualTo(PATH)).willReturn(aResponse().withStatus(409)
                .withHeader("Content-Type", "application/problem+json")
                .withBody("""
                        {"type":"https://swiftdrop.lk/problems/invalid-status-transition",
                         "title":"Invalid status transition","status":409,
                         "detail":"Cannot change status from CREATED to DELIVERED."}
                        """)));

        assertThatThrownBy(() -> client.updateStatus(PARCEL_ID, UPDATE))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CREATED to DELIVERED")
                .satisfies(e -> assertThat(((SwiftDropException) e).getProblemType())
                        .isEqualTo("invalid-status-transition"));
    }

    @Test
    void unknownParcelBecomesNotFound() {
        parcelService.stubFor(patch(urlEqualTo(PATH)).willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> client.updateStatus(PARCEL_ID, UPDATE))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void serverErrorBecomesServiceUnavailable() {
        parcelService.stubFor(patch(urlEqualTo(PATH)).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.updateStatus(PARCEL_ID, UPDATE))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void unreachableParcelServiceBecomesServiceUnavailable() {
        assertThatThrownBy(() -> clientFor("http://localhost:1").updateStatus(PARCEL_ID, UPDATE))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    private static ParcelServiceClient clientFor(String baseUrl) {
        return new ParcelServiceClient(new RestClientConfig()
                .parcelServiceRestClient(new InternalRestClientFactory(INTERNAL_TOKEN), baseUrl));
    }
}
