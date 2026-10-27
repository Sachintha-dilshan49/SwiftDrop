package lk.swiftdrop.dispatch.parcel;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.UUID;
import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.event.ParcelStatus;
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

    private static final UUID PARCEL_ID = UUID.randomUUID();
    private static final UUID COURIER_ID = UUID.randomUUID();
    private static final String PATH = "/internal/parcels/" + PARCEL_ID + "/status";
    private static final ParcelStatusUpdate UPDATE =
            new ParcelStatusUpdate(ParcelStatus.FAILED, "Gate locked", null, "Nobody at home", COURIER_ID, null);

    private ParcelServiceClient client;

    @BeforeEach
    void setUp() {
        client = new ParcelServiceClient(new RestClientConfig().parcelServiceRestClient(parcelService.baseUrl()));
    }

    @Test
    void sendsStatusChangeWithCallersTokenAsJson() {
        parcelService.stubFor(patch(urlEqualTo(PATH)).willReturn(aResponse().withStatus(200)));

        client.updateStatus(PARCEL_ID, UPDATE, "courier-token");

        parcelService.verify(patchRequestedFor(urlEqualTo(PATH))
                .withHeader("Authorization", equalTo("Bearer courier-token"))
                .withRequestBody(equalToJson("""
                        {"status":"FAILED","note":"Gate locked","recipientName":null,
                         "failureReason":"Nobody at home","courierId":"%s","proofObjectKey":null}
                        """.formatted(COURIER_ID))));
    }

    @Test
    void rejectedTransitionBecomesConflict() {
        parcelService.stubFor(patch(urlEqualTo(PATH)).willReturn(aResponse().withStatus(409)));

        assertThatThrownBy(() -> client.updateStatus(PARCEL_ID, UPDATE, "t"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cannot move to FAILED");
    }

    @Test
    void unknownParcelBecomesNotFound() {
        parcelService.stubFor(patch(urlEqualTo(PATH)).willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> client.updateStatus(PARCEL_ID, UPDATE, "t"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void serverErrorBecomesBadGateway() {
        parcelService.stubFor(patch(urlEqualTo(PATH)).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.updateStatus(PARCEL_ID, UPDATE, "t"))
                .isInstanceOf(ParcelServiceException.class);
    }

    @Test
    void unreachableParcelServiceBecomesBadGateway() {
        ParcelServiceClient offline = new ParcelServiceClient(
                new RestClientConfig().parcelServiceRestClient("http://localhost:1"));

        assertThatThrownBy(() -> offline.updateStatus(PARCEL_ID, UPDATE, "t"))
                .isInstanceOf(ParcelServiceException.class);
    }
}
