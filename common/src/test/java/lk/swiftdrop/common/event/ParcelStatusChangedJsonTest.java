package lk.swiftdrop.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import tools.jackson.databind.json.JsonMapper;

class ParcelStatusChangedJsonTest {

    // The example from docs/architecture.md §6, with full UUIDs
    private static final String CONTRACT = """
            {
              "eventId": "8f2c1e0a-3b5d-4c2e-9a71-0d6f2b9c1e44",
              "type": "ParcelStatusChanged",
              "version": 1,
              "occurredAt": "2026-10-21T09:15:00Z",
              "parcelId": "c41d6a2e-5f0b-4a8e-b1c3-7d9e2f4a6b80",
              "trackingNo": "SD-7K3Q-9P2M",
              "customerId": "a9e0b3c4-1d2e-4f5a-8b6c-7d8e9f0a1b2c",
              "merchantId": "5b17c2d3-4e5f-4a6b-9c7d-8e9f0a1b2c3d",
              "courierId": "e3f8a1b2-c3d4-4e5f-a6b7-c8d9e0f1a2b3",
              "fromStatus": "IN_TRANSIT",
              "toStatus": "OUT_FOR_DELIVERY",
              "note": "Left Kandy hub",
              "failureReason": null
            }
            """;

    private final JsonMapper mapper = JsonMapper.builder().build();

    private final ParcelStatusChanged event = new ParcelStatusChanged(
            UUID.fromString("8f2c1e0a-3b5d-4c2e-9a71-0d6f2b9c1e44"), ParcelStatusChanged.TYPE,
            ParcelStatusChanged.VERSION, Instant.parse("2026-10-21T09:15:00Z"),
            UUID.fromString("c41d6a2e-5f0b-4a8e-b1c3-7d9e2f4a6b80"), "SD-7K3Q-9P2M",
            UUID.fromString("a9e0b3c4-1d2e-4f5a-8b6c-7d8e9f0a1b2c"),
            UUID.fromString("5b17c2d3-4e5f-4a6b-9c7d-8e9f0a1b2c3d"),
            UUID.fromString("e3f8a1b2-c3d4-4e5f-a6b7-c8d9e0f1a2b3"),
            ParcelStatus.IN_TRANSIT, ParcelStatus.OUT_FOR_DELIVERY, "Left Kandy hub", null);

    @Test
    void serializesExactlyToTheDocumentedV1Shape() throws Exception {
        JSONAssert.assertEquals(CONTRACT, mapper.writeValueAsString(event), JSONCompareMode.STRICT);
    }

    @Test
    void deserializesTheDocumentedV1Shape() {
        assertThat(mapper.readValue(CONTRACT, ParcelStatusChanged.class)).isEqualTo(event);
    }

    @Test
    void ignoresFieldsAddedByNewerPublishers() {
        String withExtraField = CONTRACT.replace("\"note\"", "\"addedLater\": 1, \"note\"");

        assertThat(mapper.readValue(withExtraField, ParcelStatusChanged.class)).isEqualTo(event);
    }

    @Test
    void factoryCreatesV1EventWithRandomId() {
        ParcelStatusChanged created = ParcelStatusChanged.of(Instant.parse("2026-10-21T09:15:00Z"), UUID.randomUUID(),
                "SD-7K3Q-9P2M", UUID.randomUUID(), null, null, null, ParcelStatus.CREATED, null, null);

        assertThat(created.type()).isEqualTo("ParcelStatusChanged");
        assertThat(created.version()).isEqualTo(1);
        assertThat(created.eventId()).isNotNull();
    }

    @Test
    void rejectsEventWithoutRequiredFields() {
        assertThatNullPointerException().isThrownBy(() -> ParcelStatusChanged.of(Instant.now(), UUID.randomUUID(),
                "SD-7K3Q-9P2M", UUID.randomUUID(), null, null, null, null, null, null)).withMessage("toStatus");
    }

    @Test
    void buildsLowerCaseRoutingKeys() {
        assertThat(ParcelEvents.routingKey(ParcelStatus.OUT_FOR_DELIVERY)).isEqualTo("parcel.status.out_for_delivery");
        assertThat(ParcelEvents.ALL_STATUS_CHANGES).isEqualTo("parcel.status.#");
    }
}
