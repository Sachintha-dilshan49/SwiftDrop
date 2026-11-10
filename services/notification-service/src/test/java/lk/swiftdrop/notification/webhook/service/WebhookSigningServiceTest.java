package lk.swiftdrop.notification.webhook.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class WebhookSigningServiceTest {

    private final WebhookSigningService service = new WebhookSigningService();

    @Test
    void producesConsistentHmacSignature() {
        assertThat(service.sign("hello", "secret")).isEqualTo(service.sign("hello", "secret"));
    }

    @Test
    void signatureStartsWithSha256Prefix() {
        assertThat(service.sign("payload", "key")).startsWith("sha256=");
    }

    @Test
    void differentPayloadsProduceDifferentSignatures() {
        assertThat(service.sign("payload1", "secret")).isNotEqualTo(service.sign("payload2", "secret"));
    }

    @Test
    void differentSecretsProduceDifferentSignatures() {
        assertThat(service.sign("payload", "secret1")).isNotEqualTo(service.sign("payload", "secret2"));
    }

    @Test
    void signatureHasExpectedLength() {
        // sha256= prefix (7) + 64 hex chars = 71
        assertThat(service.sign("test", "key")).hasSize(71);
    }
}
