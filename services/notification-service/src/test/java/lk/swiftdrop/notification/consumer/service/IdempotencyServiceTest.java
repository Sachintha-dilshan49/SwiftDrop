package lk.swiftdrop.notification.consumer.service;

import lk.swiftdrop.notification.consumer.entity.ProcessedEvent;
import lk.swiftdrop.notification.consumer.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock private ProcessedEventRepository repository;
    @InjectMocks private IdempotencyService idempotencyService;

    @Test
    void returnsTrueWhenEventAlreadyProcessed() {
        UUID eventId = UUID.randomUUID();
        when(repository.existsByEventIdAndConsumerName(eventId, "notification")).thenReturn(true);
        assertThat(idempotencyService.isAlreadyProcessed(eventId, "notification")).isTrue();
    }

    @Test
    void returnsFalseWhenEventNotProcessed() {
        UUID eventId = UUID.randomUUID();
        when(repository.existsByEventIdAndConsumerName(eventId, "notification")).thenReturn(false);
        assertThat(idempotencyService.isAlreadyProcessed(eventId, "notification")).isFalse();
    }

    @Test
    void markProcessedSavesNewRecord() {
        UUID eventId = UUID.randomUUID();
        when(repository.existsByEventIdAndConsumerName(eventId, "webhook")).thenReturn(false);
        idempotencyService.markProcessed(eventId, "webhook");
        verify(repository).save(any(ProcessedEvent.class));
    }

    @Test
    void markProcessedSkipsDuplicates() {
        UUID eventId = UUID.randomUUID();
        when(repository.existsByEventIdAndConsumerName(eventId, "webhook")).thenReturn(true);
        idempotencyService.markProcessed(eventId, "webhook");
        verify(repository, never()).save(any());
    }
}
