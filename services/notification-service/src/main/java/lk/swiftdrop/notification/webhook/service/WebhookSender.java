package lk.swiftdrop.notification.webhook.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.time.Instant;

@Service
public class WebhookSender {

    private static final Logger log = LoggerFactory.getLogger(WebhookSender.class);
    private final RestClient restClient;
    private final WebhookSigningService signingService;

    public WebhookSender(@Qualifier("webhookClient") RestClient restClient,
                         WebhookSigningService signingService) {
        this.restClient = restClient;
        this.signingService = signingService;
    }

    public WebhookSendResult send(String url, String body, String secret, String eventId) {
        String signature = signingService.sign(body, secret);
        try {
            ResponseEntity<String> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-SwiftDrop-Signature", signature)
                    .header("X-SwiftDrop-Event-Id", eventId)
                    .header("X-SwiftDrop-Timestamp", Instant.now().toString())
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);

            int statusCode = response.getStatusCode().value();
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Webhook delivered to {} — HTTP {}", url, statusCode);
                return new WebhookSendResult(true, statusCode, response.getBody(), null);
            } else {
                log.warn("Webhook to {} returned non-2xx: {}", url, statusCode);
                return new WebhookSendResult(false, statusCode, response.getBody(),
                        "Non-2xx response: " + statusCode);
            }
        } catch (RestClientException e) {
            log.error("Webhook to {} failed: {}", url, e.getMessage());
            return new WebhookSendResult(false, 0, null, e.getMessage());
        }
    }

    public record WebhookSendResult(boolean success, int httpStatus,
                                    String responseBody, String failureReason) {}
}
