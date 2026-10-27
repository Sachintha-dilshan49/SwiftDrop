package lk.swiftdrop.notification.client;

import lk.swiftdrop.notification.client.dto.MerchantWebhookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.util.Optional;
import java.util.UUID;

@Component
public class MerchantServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MerchantServiceClient.class);
    private final RestClient restClient;

    public MerchantServiceClient(@Qualifier("merchantServiceClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Optional<MerchantWebhookResponse> getWebhookConfig(UUID merchantId) {
        try {
            MerchantWebhookResponse response = restClient.get()
                    .uri("/internal/merchants/{id}/webhook", merchantId)
                    .retrieve()
                    .body(MerchantWebhookResponse.class);
            return Optional.ofNullable(response);
        } catch (RestClientException e) {
            log.error("Failed to fetch webhook config for merchant {}: {}", merchantId, e.getMessage());
            return Optional.empty();
        }
    }
}
