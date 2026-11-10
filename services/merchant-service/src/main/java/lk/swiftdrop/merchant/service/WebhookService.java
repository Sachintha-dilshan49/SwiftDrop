package lk.swiftdrop.merchant.service;

import java.util.UUID;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.merchant.dto.InternalMerchantWebhookResponse;
import lk.swiftdrop.merchant.dto.UpdateWebhookRequest;
import lk.swiftdrop.merchant.dto.WebhookCreatedResponse;
import lk.swiftdrop.merchant.dto.WebhookSettingsResponse;
import lk.swiftdrop.merchant.entity.MerchantWebhookEndpoint;
import lk.swiftdrop.merchant.repository.MerchantRepository;
import lk.swiftdrop.merchant.repository.MerchantWebhookEndpointRepository;
import lk.swiftdrop.merchant.security.ApiKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final MerchantWebhookEndpointRepository webhookRepository;
    private final MerchantRepository merchantRepository;
    private final ApiKeyGenerator apiKeyGenerator;

    public WebhookService(MerchantWebhookEndpointRepository webhookRepository,
                          MerchantRepository merchantRepository,
                          ApiKeyGenerator apiKeyGenerator) {
        this.webhookRepository = webhookRepository;
        this.merchantRepository = merchantRepository;
        this.apiKeyGenerator = apiKeyGenerator;
    }

    @Transactional(readOnly = true)
    public WebhookSettingsResponse getWebhookSettings(UUID merchantId) {
        MerchantWebhookEndpoint endpoint = webhookRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new NotFoundException("Webhook settings not found for merchant: " + merchantId));
        return WebhookSettingsResponse.from(endpoint);
    }

    @Transactional
    public WebhookCreatedResponse updateWebhookSettings(UUID merchantId, UpdateWebhookRequest request) {
        if (!merchantRepository.existsById(merchantId)) {
            throw new NotFoundException("Merchant not found: " + merchantId);
        }

        MerchantWebhookEndpoint endpoint = webhookRepository.findByMerchantId(merchantId)
                .orElseGet(() -> new MerchantWebhookEndpoint(
                        UUID.randomUUID(),
                        merchantId,
                        request.webhookUrl(),
                        apiKeyGenerator.generateWebhookSecret()
                ));

        endpoint.setWebhookUrl(request.webhookUrl());
        if (request.rotateSecret()) {
            endpoint.setSecret(apiKeyGenerator.generateWebhookSecret());
            log.info("Rotated webhook secret for merchantId: {}", merchantId);
        }

        MerchantWebhookEndpoint saved = webhookRepository.save(endpoint);
        log.info("Updated webhook settings for merchantId: {}", merchantId);

        return new WebhookCreatedResponse(
                saved.getId(),
                saved.getWebhookUrl(),
                saved.getSecret(),
                saved.isActive(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public InternalMerchantWebhookResponse getInternalWebhook(UUID merchantId) {
        MerchantWebhookEndpoint endpoint = webhookRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new NotFoundException("Webhook endpoint not found for merchant: " + merchantId));

        if (!endpoint.isActive()) {
            throw new NotFoundException("Webhook endpoint is disabled for merchant: " + merchantId);
        }

        return new InternalMerchantWebhookResponse(
                endpoint.getWebhookUrl(),
                endpoint.getSecret()
        );
    }
}
