package lk.swiftdrop.parcel.tracking.consumer;

import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.parcel.tracking.entity.ProcessedEvent;
import lk.swiftdrop.parcel.tracking.repository.ProcessedEventRepository;
import lk.swiftdrop.parcel.tracking.service.TrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingEventConsumerTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;
    @Mock
    private TrackingService trackingService;

    private TrackingEventConsumer consumer;

    @BeforeEach
    void createConsumer() {
        consumer = new TrackingEventConsumer(processedEventRepository, trackingService, "tracking.q");
    }

    private final ParcelStatusChanged event = ParcelStatusChanged.of(Instant.now(), UUID.randomUUID(),
            "SD-7K3Q-9P2M", UUID.randomUUID(), null, UUID.randomUUID(), ParcelStatus.IN_TRANSIT,
            ParcelStatus.OUT_FOR_DELIVERY, "Out for delivery", null);

    @Test
    @DisplayName("A new event is recorded as processed for tracking.q and then pushed to the streams")
    void newEventIsRecordedThenPushed() {
        when(processedEventRepository.existsByEventIdAndConsumerName(event.eventId(), "tracking.q"))
                .thenReturn(false);

        consumer.onParcelStatusChanged(event);

        ArgumentCaptor<ProcessedEvent> saved = ArgumentCaptor.forClass(ProcessedEvent.class);
        InOrder order = inOrder(processedEventRepository, trackingService);
        order.verify(processedEventRepository).saveAndFlush(saved.capture());
        order.verify(trackingService).sendUpdate(event);
        assertThat(saved.getValue().getEventId()).isEqualTo(event.eventId());
        assertThat(saved.getValue().getConsumerName()).isEqualTo("tracking.q");
        assertThat(saved.getValue().getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("An event already processed by tracking.q is skipped and not pushed again")
    void duplicateEventIsSkipped() {
        when(processedEventRepository.existsByEventIdAndConsumerName(event.eventId(), "tracking.q"))
                .thenReturn(true);

        consumer.onParcelStatusChanged(event);

        verify(processedEventRepository, never()).saveAndFlush(any());
        verifyNoInteractions(trackingService);
    }

    @Test
    @DisplayName("If a parallel delivery stored the event first, nothing is pushed and the delivery fails for a retry")
    void racingDuplicateIsNotPushed() {
        when(processedEventRepository.existsByEventIdAndConsumerName(event.eventId(), "tracking.q"))
                .thenReturn(false);
        when(processedEventRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("uq_tracking_processed_event"));

        assertThatThrownBy(() -> consumer.onParcelStatusChanged(event))
                .isInstanceOf(DataIntegrityViolationException.class);
        verifyNoInteractions(trackingService);
    }
}
