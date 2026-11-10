package lk.swiftdrop.merchant.controller;

import java.util.UUID;
import lk.swiftdrop.merchant.dto.InternalMerchantWebhookResponse;
import lk.swiftdrop.merchant.service.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/merchants")
public class InternalMerchantController {

    private final WebhookService webhookService;

    public InternalMerchantController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping("/{merchantId}/webhook")
    public ResponseEntity<InternalMerchantWebhookResponse> getMerchantWebhook(
            @PathVariable UUID merchantId) {
        InternalMerchantWebhookResponse response = webhookService.getInternalWebhook(merchantId);
        return ResponseEntity.ok(response);
    }
}
