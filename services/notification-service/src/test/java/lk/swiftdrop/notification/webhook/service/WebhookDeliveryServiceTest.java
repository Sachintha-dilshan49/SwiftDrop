package lk.swiftdrop.notification.webhook.service;

import lk.swiftdrop.notification.webhook.entity.WebhookDelivery;
import lk.swiftdrop.notification.webhook.entity.WebhookDeliveryStatus;
import lk.swiftdrop.notification.webhook.repository.WebhookDeliveryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookDeliveryServiceTest {

    @Mock private WebhookDeliveryRepository repository;
    @InjectMocks private WebhookDeliveryService service;

    @Test
    void recordSuccessSetsCorrectStatus() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        WebhookDelivery result = service.recordSuccess(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "https://store.example.com/hook", 1, "{}", 200, "OK");
        assertThat(result.getStatus()).isEqualTo(WebhookDeliveryStatus.SUCCESS);
        assertThat(result.getHttpStatus()).isEqualTo(200);
    }

    @Test
    void recordFailureSetsCorrectStatus() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        WebhookDelivery result = service.recordFailure(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "https://store.example.com/hook", 2, "{}", 500, "Error", "Server error");
        assertThat(result.getStatus()).isEqualTo(WebhookDeliveryStatus.FAILED);
        assertThat(result.getAttemptNumber()).isEqualTo(2);
    }

    @Test
    void recordParkedSetsCorrectStatus() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        WebhookDelivery result = service.recordParked(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "https://store.example.com/hook", 4, "{}", "Max retries exceeded");
        assertThat(result.getStatus()).isEqualTo(WebhookDeliveryStatus.PARKED);
    }
}
