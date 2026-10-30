package lk.swiftdrop.merchant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private MerchantWebhookEndpointRepository webhookRepository;

    @Mock
    private MerchantRepository merchantRepository;

    private ApiKeyGenerator apiKeyGenerator;
    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        apiKeyGenerator = new ApiKeyGenerator();
        webhookService = new WebhookService(webhookRepository, merchantRepository, apiKeyGenerator);
    }

    @Test
    @DisplayName("Get webhook settings returns settings without secret")
    void getWebhookSettingsSuccess() {
        UUID merchantId = UUID.randomUUID();
        MerchantWebhookEndpoint endpoint = new MerchantWebhookEndpoint(
                UUID.randomUUID(), merchantId, "https://apex.lk/webhook", "whsec_secret123");

        when(webhookRepository.findByMerchantId(merchantId)).thenReturn(Optional.of(endpoint));

        WebhookSettingsResponse response = webhookService.getWebhookSettings(merchantId);
        assertThat(response.webhookUrl()).isEqualTo("https://apex.lk/webhook");
        assertThat(response.active()).isTrue();
    }

    @Test
    @DisplayName("Get webhook settings throws NotFoundException if not configured")
    void getWebhookSettingsNotFound() {
        UUID merchantId = UUID.randomUUID();
        when(webhookRepository.findByMerchantId(merchantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> webhookService.getWebhookSettings(merchantId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Webhook settings not found");
    }

    @Test
    @DisplayName("Update webhook creates new endpoint with generated secret")
    void createNewWebhookEndpoint() {
        UUID merchantId = UUID.randomUUID();
        UpdateWebhookRequest request = new UpdateWebhookRequest("https://apex.lk/webhook", false);

        when(merchantRepository.existsById(merchantId)).thenReturn(true);
        when(webhookRepository.findByMerchantId(merchantId)).thenReturn(Optional.empty());
        when(webhookRepository.save(any(MerchantWebhookEndpoint.class))).thenAnswer(inv -> inv.getArgument(0));

        WebhookCreatedResponse response = webhookService.updateWebhookSettings(merchantId, request);
        assertThat(response.webhookUrl()).isEqualTo("https://apex.lk/webhook");
        assertThat(response.secret()).startsWith("whsec_");
        verify(webhookRepository).save(any(MerchantWebhookEndpoint.class));
    }

    @Test
    @DisplayName("Update webhook rotates secret when requested")
    void updateWebhookRotateSecret() {
        UUID merchantId = UUID.randomUUID();
        MerchantWebhookEndpoint existing = new MerchantWebhookEndpoint(
                UUID.randomUUID(), merchantId, "https://apex.lk/old-webhook", "whsec_oldsecret");
        UpdateWebhookRequest request = new UpdateWebhookRequest("https://apex.lk/new-webhook", true);

        when(merchantRepository.existsById(merchantId)).thenReturn(true);
        when(webhookRepository.findByMerchantId(merchantId)).thenReturn(Optional.of(existing));
        when(webhookRepository.save(any(MerchantWebhookEndpoint.class))).thenAnswer(inv -> inv.getArgument(0));

        WebhookCreatedResponse response = webhookService.updateWebhookSettings(merchantId, request);
        assertThat(response.webhookUrl()).isEqualTo("https://apex.lk/new-webhook");
        assertThat(response.secret()).startsWith("whsec_");
        assertThat(response.secret()).isNotEqualTo("whsec_oldsecret");
    }

    @Test
    @DisplayName("Internal webhook retrieval for Role 6 returns webhookUrl and secret")
    void getInternalWebhookSuccess() {
        UUID merchantId = UUID.randomUUID();
        MerchantWebhookEndpoint endpoint = new MerchantWebhookEndpoint(
                UUID.randomUUID(), merchantId, "https://apex.lk/webhook", "whsec_supersecret");

        when(webhookRepository.findByMerchantId(merchantId)).thenReturn(Optional.of(endpoint));

        InternalMerchantWebhookResponse response = webhookService.getInternalWebhook(merchantId);
        assertThat(response.webhookUrl()).isEqualTo("https://apex.lk/webhook");
        assertThat(response.secret()).isEqualTo("whsec_supersecret");
    }

    @Test
    @DisplayName("Internal webhook retrieval throws NotFoundException if disabled")
    void getInternalWebhookInactive() {
        UUID merchantId = UUID.randomUUID();
        MerchantWebhookEndpoint endpoint = new MerchantWebhookEndpoint(
                UUID.randomUUID(), merchantId, "https://apex.lk/webhook", "whsec_supersecret");
        endpoint.setActive(false);

        when(webhookRepository.findByMerchantId(merchantId)).thenReturn(Optional.of(endpoint));

        assertThatThrownBy(() -> webhookService.getInternalWebhook(merchantId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Webhook endpoint is disabled");
    }
}
