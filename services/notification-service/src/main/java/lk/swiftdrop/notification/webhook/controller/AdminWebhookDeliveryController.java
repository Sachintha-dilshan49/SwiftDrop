package lk.swiftdrop.notification.webhook.controller;

import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.notification.client.MerchantServiceClient;
import lk.swiftdrop.notification.client.dto.MerchantWebhookResponse;
import lk.swiftdrop.notification.webhook.dto.WebhookDeliveryResponse;
import lk.swiftdrop.notification.webhook.entity.WebhookDelivery;
import lk.swiftdrop.notification.webhook.entity.WebhookDeliveryStatus;
import lk.swiftdrop.notification.webhook.mapper.WebhookDeliveryMapper;
import lk.swiftdrop.notification.webhook.service.WebhookDeliveryService;
import lk.swiftdrop.notification.webhook.service.WebhookSender;
import lk.swiftdrop.notification.webhook.service.WebhookSender.WebhookSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/webhook-deliveries")
public class AdminWebhookDeliveryController {

    private static final Logger log = LoggerFactory.getLogger(AdminWebhookDeliveryController.class);

    private final WebhookDeliveryService deliveryService;
    private final WebhookDeliveryMapper mapper;
    private final WebhookSender webhookSender;
    private final MerchantServiceClient merchantServiceClient;

    public AdminWebhookDeliveryController(WebhookDeliveryService deliveryService,
                                          WebhookDeliveryMapper mapper,
                                          WebhookSender webhookSender,
                                          MerchantServiceClient merchantServiceClient) {
        this.deliveryService = deliveryService;
        this.mapper = mapper;
        this.webhookSender = webhookSender;
        this.merchantServiceClient = merchantServiceClient;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public Page<WebhookDeliveryResponse> list(
            @RequestParam(required = false) WebhookDeliveryStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<WebhookDelivery> page = (status != null)
                ? deliveryService.listByStatus(status, pageable)
                : deliveryService.listAll(pageable);
        return page.map(mapper::toResponse);
    }

    @PostMapping("/{id}/replay")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<WebhookDeliveryResponse> replay(@PathVariable UUID id) {
        WebhookDelivery original = deliveryService.findById(id)
                .orElseThrow(() -> new NotFoundException("Webhook delivery not found: " + id));

        MerchantWebhookResponse config = merchantServiceClient
                .getWebhookConfig(original.getMerchantId())
                .orElseThrow(() -> new NotFoundException(
                        "Merchant webhook config not found for merchant: " + original.getMerchantId()));

        WebhookSendResult result = webhookSender.send(
                config.webhookUrl(), original.getRequestBody(),
                config.secret(), original.getEventId().toString());

        WebhookDelivery replay;
        if (result.success()) {
            replay = deliveryService.recordSuccess(original.getEventId(), original.getParcelId(),
                    original.getMerchantId(), config.webhookUrl(), original.getAttemptNumber() + 1,
                    original.getRequestBody(), result.httpStatus(), result.responseBody());
            log.info("Webhook {} replayed successfully", id);
        } else {
            replay = deliveryService.recordFailure(original.getEventId(), original.getParcelId(),
                    original.getMerchantId(), config.webhookUrl(), original.getAttemptNumber() + 1,
                    original.getRequestBody(), result.httpStatus(), result.responseBody(), result.failureReason());
            log.warn("Webhook {} replay failed: {}", id, result.failureReason());
        }
        return ResponseEntity.ok(mapper.toResponse(replay));
    }
}
