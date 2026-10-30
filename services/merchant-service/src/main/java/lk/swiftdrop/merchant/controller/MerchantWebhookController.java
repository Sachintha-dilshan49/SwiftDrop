package lk.swiftdrop.merchant.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lk.swiftdrop.merchant.dto.UpdateWebhookRequest;
import lk.swiftdrop.merchant.dto.WebhookCreatedResponse;
import lk.swiftdrop.merchant.dto.WebhookSettingsResponse;
import lk.swiftdrop.merchant.entity.Merchant;
import lk.swiftdrop.merchant.security.SecurityUtils;
import lk.swiftdrop.merchant.service.MerchantService;
import lk.swiftdrop.merchant.service.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
public class MerchantWebhookController {

    private final WebhookService webhookService;
    private final MerchantService merchantService;

    public MerchantWebhookController(WebhookService webhookService, MerchantService merchantService) {
        this.webhookService = webhookService;
        this.merchantService = merchantService;
    }

    @GetMapping({"/api/v1/merchants/me/webhook", "/merchants/me/webhook"})
    public ResponseEntity<WebhookSettingsResponse> getWebhookSettings() {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        Merchant merchant = merchantService.findEntityByUserId(userId);
        WebhookSettingsResponse response = webhookService.getWebhookSettings(merchant.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping({"/api/v1/merchants/me/webhook", "/merchants/me/webhook"})
    public ResponseEntity<WebhookCreatedResponse> updateWebhookSettings(
            @Valid @RequestBody UpdateWebhookRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        Merchant merchant = merchantService.findEntityByUserId(userId);
        WebhookCreatedResponse response = webhookService.updateWebhookSettings(merchant.getId(), request);
        return ResponseEntity.ok(response);
    }
}
