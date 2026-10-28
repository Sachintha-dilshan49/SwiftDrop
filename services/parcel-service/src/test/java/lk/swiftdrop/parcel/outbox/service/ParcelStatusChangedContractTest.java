package lk.swiftdrop.parcel.outbox.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.parcel.config.JacksonConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the JSON shape of {@code ParcelStatusChanged} v1 as published by parcel-service
 * (docs/architecture.md §6), including compatibility with notification-service's converter.
 */
class ParcelStatusChangedContractTest {

    private static final Set<String> CONTRACT_FIELDS = Set.of(
            "eventId", "type", "version", "occurredAt", "parcelId", "trackingNo", "customerId",
            "merchantId", "courierId", "fromStatus", "toStatus", "note", "failureReason");

    private final ObjectMapper mapper = JacksonConfig.eventObjectMapper();

    private final ParcelStatusChanged event = new ParcelStatusChanged(
            UUID.fromString("8f2c1e0a-3b5d-4c2e-9a71-0d6f2b9c1e44"),
            ParcelStatusChanged.TYPE,
            ParcelStatusChanged.VERSION,
            Instant.parse("2026-10-21T09:15:00Z"),
            UUID.randomUUID(),
            "SD-7K3Q-9P2M",
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            ParcelStatus.IN_TRANSIT,
            ParcelStatus.OUT_FOR_DELIVERY,
            "Left Kandy hub",
            null);

    @Test
    @DisplayName("Event JSON has exactly the v1 contract fields")
    void serializesExactlyTheContractFields() throws Exception {
        JsonNode json = mapper.readTree(mapper.writeValueAsString(event));

        Set<String> fields = new HashSet<>();
        json.fieldNames().forEachRemaining(fields::add);

        assertThat(fields).isEqualTo(CONTRACT_FIELDS);
        assertThat(json.get("type").asText()).isEqualTo("ParcelStatusChanged");
        assertThat(json.get("version").asInt()).isEqualTo(1);
        assertThat(json.get("toStatus").asText()).isEqualTo("OUT_FOR_DELIVERY");
        assertThat(json.get("merchantId").isNull()).isTrue();
        assertThat(json.get("failureReason").isNull()).isTrue();
    }

    @Test
    @DisplayName("occurredAt is an ISO-8601 UTC string, not an epoch number")
    void serializesOccurredAtAsIsoString() throws Exception {
        JsonNode json = mapper.readTree(mapper.writeValueAsString(event));

        assertThat(json.get("occurredAt").isTextual()).isTrue();
        assertThat(json.get("occurredAt").asText()).isEqualTo("2026-10-21T09:15:00Z");
    }

    @Test
    @DisplayName("A message from parcel-service's converter is read back by a converter configured like notification-service's")
    void roundTripsThroughNotificationStyleConverter() {
        Jackson2JsonMessageConverter publisher = new Jackson2JsonMessageConverter(mapper, "*");
        Jackson2JsonMessageConverter consumer = new Jackson2JsonMessageConverter("*");

        Message message = publisher.toMessage(event, new MessageProperties());

        assertThat(new String(message.getBody())).contains("\"occurredAt\":\"2026-10-21T09:15:00Z\"");
        assertThat(consumer.fromMessage(message)).isEqualTo(event);
    }

    @Test
    @DisplayName("Outbox rows written before the fix (numeric occurredAt) are still readable")
    void readsLegacyNumericOccurredAt() throws Exception {
        ObjectMapper legacyMapper = JacksonConfig.eventObjectMapper().copy()
                .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String legacyPayload = legacyMapper.writeValueAsString(event);
        assertThat(mapper.readTree(legacyPayload).get("occurredAt").isNumber()).isTrue();

        ParcelStatusChanged read = mapper.readValue(legacyPayload, ParcelStatusChanged.class);

        assertThat(read).isEqualTo(event);
    }
}
