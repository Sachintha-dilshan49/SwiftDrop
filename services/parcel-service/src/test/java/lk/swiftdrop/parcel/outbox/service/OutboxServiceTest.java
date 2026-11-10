package lk.swiftdrop.parcel.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.swiftdrop.common.event.ParcelEvents;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.parcel.config.JacksonConfig;
import lk.swiftdrop.parcel.outbox.entity.OutboxEvent;
import lk.swiftdrop.parcel.outbox.entity.OutboxStatus;
import lk.swiftdrop.parcel.outbox.repository.OutboxEventRepository;
import lk.swiftdrop.parcel.parcel.entity.Parcel;
import lk.swiftdrop.parcel.parcel.entity.ParcelSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OutboxService outboxService;
    private final ObjectMapper objectMapper = JacksonConfig.eventObjectMapper();

    @BeforeEach
    void setUp() {
        outboxService = new OutboxService(outboxEventRepository, objectMapper);
    }

    @Test
    @DisplayName("enqueueStatusChangedEvent writes pending event into outbox with correct routing key and payload")
    void enqueueStatusChangedEventSuccess() {
        Parcel parcel = new Parcel(
                UUID.randomUUID(),
                "SD-7K3Q-9P2M",
                UUID.randomUUID(),
                null,
                "Pickup Street",
                "Dropoff Street",
                "Sunil",
                "+94770000000",
                ParcelSize.SMALL,
                BigDecimal.ONE,
                new BigDecimal("400.00"),
                "LKR"
        );
        parcel.setStatus(ParcelStatus.OUT_FOR_DELIVERY);

        outboxService.enqueueStatusChangedEvent(parcel, ParcelStatus.IN_TRANSIT, "Courier out for delivery");

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getRoutingKey()).isEqualTo(ParcelEvents.routingKey(ParcelStatus.OUT_FOR_DELIVERY));
        assertThat(saved.getPayload()).contains("SD-7K3Q-9P2M");
        assertThat(saved.getPayload()).contains("OUT_FOR_DELIVERY");
        assertThat(saved.getAggregateId()).isEqualTo(parcel.getId());
        assertThat(saved.getPayload()).containsPattern("\"occurredAt\":\"\\d{4}-\\d{2}-\\d{2}T[^\"]+Z\"");
    }
}
