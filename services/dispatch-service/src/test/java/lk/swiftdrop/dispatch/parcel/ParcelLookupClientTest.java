package lk.swiftdrop.dispatch.parcel;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.error.ServiceUnavailableException;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.dispatch.config.RestClientConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Runs the client against a fake parcel-service over real HTTP. */
class ParcelLookupClientTest {

    @RegisterExtension
    static WireMockExtension parcelService = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final String PATH = "/api/v1/parcels";
    private static final UUID PARCEL_ID = UUID.randomUUID();

    @Test
    void asksForTheOldestCreatedParcelsWithTheDispatchersOwnToken() {
        parcelService.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson("""
                {"content":[{"id":"%s","trackingNo":"SD-0001","status":"CREATED",
                  "pickupAddress":"Colombo 03","dropoffAddress":"Kandy",
                  "recipientName":"Nimali","recipientPhone":"0771234567",
                  "size":"SMALL","weightKg":1.50,"feeAmount":450.00,"feeCurrency":"LKR",
                  "createdAt":"2026-11-10T04:30:00Z","updatedAt":"2026-11-10T04:30:00Z"}],
                 "page":0,"size":100,"totalElements":1}
                """.formatted(PARCEL_ID))));

        List<WaitingParcel> parcels = clientFor(parcelService.baseUrl()).oldestCreatedParcels("dispatcher-token");

        assertThat(parcels).singleElement().satisfies(parcel -> {
            assertThat(parcel.id()).isEqualTo(PARCEL_ID);
            assertThat(parcel.trackingNo()).isEqualTo("SD-0001");
            assertThat(parcel.dropoffAddress()).isEqualTo("Kandy");
            assertThat(parcel.weightKg()).isEqualByComparingTo(new BigDecimal("1.50"));
        });
        parcelService.verify(getRequestedFor(urlPathEqualTo(PATH))
                .withQueryParam("status", equalTo("CREATED"))
                .withQueryParam("sort", equalTo("createdAt,asc"))
                .withQueryParam("size", equalTo("100"))
                .withHeader("Authorization", equalTo("Bearer dispatcher-token"))
                .withHeader("X-Internal-Token", absent()));
    }

    @Test
    void emptyPageGivesEmptyList() {
        parcelService.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson("""
                {"content":[],"page":0,"size":100,"totalElements":0}
                """)));

        assertThat(clientFor(parcelService.baseUrl()).oldestCreatedParcels("t")).isEmpty();
    }

    @Test
    void serverErrorBecomesServiceUnavailable() {
        parcelService.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> clientFor(parcelService.baseUrl()).oldestCreatedParcels("t"))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void unreachableParcelServiceBecomesServiceUnavailable() {
        assertThatThrownBy(() -> clientFor("http://localhost:1").oldestCreatedParcels("t"))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void readsOneParcelWithTheDispatchersOwnToken() {
        parcelService.stubFor(get(urlPathEqualTo(PATH + "/" + PARCEL_ID)).willReturn(okJson("""
                {"id":"%s","trackingNo":"SD-0001","status":"CREATED","pickupAddress":"Liberty Plaza",
                 "dropoffAddress":"Rajagiriya Flyover","recipientName":"Nimal","recipientPhone":"0711234567",
                 "size":"SMALL","weightKg":1.20,"history":[]}
                """.formatted(PARCEL_ID))));

        AssignableParcel parcel = clientFor(parcelService.baseUrl()).getParcel(PARCEL_ID, "dispatcher-token");

        assertThat(parcel.status()).isEqualTo(ParcelStatus.CREATED);
        assertThat(parcel.recipientName()).isEqualTo("Nimal");
        assertThat(parcel.toString()).doesNotContain("Nimal", "Liberty Plaza");
        parcelService.verify(getRequestedFor(urlPathEqualTo(PATH + "/" + PARCEL_ID))
                .withHeader("Authorization", equalTo("Bearer dispatcher-token")));
    }

    @Test
    void unknownParcelBecomesNotFound() {
        parcelService.stubFor(get(urlPathEqualTo(PATH + "/" + PARCEL_ID)).willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> clientFor(parcelService.baseUrl()).getParcel(PARCEL_ID, "t"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void parcelDetailsServerErrorBecomesServiceUnavailable() {
        parcelService.stubFor(get(urlPathEqualTo(PATH + "/" + PARCEL_ID)).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> clientFor(parcelService.baseUrl()).getParcel(PARCEL_ID, "t"))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    private static ParcelLookupClient clientFor(String baseUrl) {
        return new ParcelLookupClient(new RestClientConfig().parcelServiceUserRestClient(baseUrl));
    }
}
