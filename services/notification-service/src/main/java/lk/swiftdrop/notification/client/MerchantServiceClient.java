package lk.swiftdrop.notification.client;

import lk.swiftdrop.common.error.SwiftDropException;
import lk.swiftdrop.notification.client.dto.MerchantWebhookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Optional;
import java.util.UUID;

@Component
public class MerchantServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MerchantServiceClient.class);
    private final RestClient restClient;

    public MerchantServiceClient(@Qualifier("merchantServiceClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Fetches the webhook configuration (URL + secret) for a merchant from merchant-service.
     * Returns empty if the merchant has no webhook configured (404) or if merchant-service is unavailable.
     * The InternalRestClientFactory maps 404 → NotFoundException and I/O failures → ServiceUnavailableException.
     */
    public Optional<MerchantWebhookResponse> getWebhookConfig(UUID merchantId) {
        try {
            MerchantWebhookResponse response = restClient.get()
                    .uri("/internal/merchants/{id}/webhook", merchantId)
                    .retrieve()
                    .body(MerchantWebhookResponse.class);
            return Optional.ofNullable(response);
        } catch (SwiftDropException e) {
            log.error("Failed to fetch webhook config for merchant {}: {}", merchantId, e.getMessage());
            return Optional.empty();
        }
    }
}
