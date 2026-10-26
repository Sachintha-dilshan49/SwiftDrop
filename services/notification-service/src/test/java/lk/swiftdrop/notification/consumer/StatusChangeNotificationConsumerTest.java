package lk.swiftdrop.notification.consumer;

import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.notification.consumer.service.IdempotencyService;
import lk.swiftdrop.notification.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusChangeNotificationConsumerTest {

    @Mock private IdempotencyService idempotencyService;
    @Mock private NotificationService notificationService;
    @InjectMocks private StatusChangeNotificationConsumer consumer;

    @Test
    void processesNewEvent() {
        ParcelStatusChanged event = buildEvent();
        when(idempotencyService.isAlreadyProcessed(event.eventId(), "notification")).thenReturn(false);
        consumer.onStatusChange(event);
        verify(notificationService).processStatusChange(event);
        verify(idempotencyService).markProcessed(event.eventId(), "notification");
    }

    @Test
    void skipsDuplicateEvent() {
        ParcelStatusChanged event = buildEvent();
        when(idempotencyService.isAlreadyProcessed(event.eventId(), "notification")).thenReturn(true);
        consumer.onStatusChange(event);
        verify(notificationService, never()).processStatusChange(event);
    }

    @Test
    void rethrowsExceptionForRequeue() {
        ParcelStatusChanged event = buildEvent();
        when(idempotencyService.isAlreadyProcessed(event.eventId(), "notification")).thenReturn(false);
        doThrow(new RuntimeException("DB error")).when(notificationService).processStatusChange(any());
        assertThatThrownBy(() -> consumer.onStatusChange(event))
                .isInstanceOf(RuntimeException.class);
    }

    private ParcelStatusChanged buildEvent() {
        return new ParcelStatusChanged(
                UUID.randomUUID(), ParcelStatusChanged.TYPE, ParcelStatusChanged.VERSION,
                Instant.now(), UUID.randomUUID(), "SD-TEST-0001", UUID.randomUUID(),
                null, null, ParcelStatus.CREATED, ParcelStatus.PICKED_UP, null, null);
    }
}
