package lk.swiftdrop.merchant.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.merchant.dto.ApiKeyCreatedResponse;
import lk.swiftdrop.merchant.dto.ApiKeyResponse;
import lk.swiftdrop.merchant.dto.CreateApiKeyRequest;
import lk.swiftdrop.merchant.entity.Merchant;
import lk.swiftdrop.merchant.security.SecurityUtils;
import lk.swiftdrop.merchant.service.ApiKeyService;
import lk.swiftdrop.merchant.service.MerchantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
public class MerchantApiKeyController {

    private final ApiKeyService apiKeyService;
    private final MerchantService merchantService;

    public MerchantApiKeyController(ApiKeyService apiKeyService, MerchantService merchantService) {
        this.apiKeyService = apiKeyService;
        this.merchantService = merchantService;
    }

    @PostMapping({"/api/v1/merchants/me/api-keys", "/merchants/me/api-keys"})
    public ResponseEntity<ApiKeyCreatedResponse> createApiKey(@Valid @RequestBody CreateApiKeyRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        Merchant merchant = merchantService.findEntityByUserId(userId);
        ApiKeyCreatedResponse response = apiKeyService.createApiKey(merchant.getId(), request);
        return ResponseEntity.created(URI.create("/api/v1/merchants/me/api-keys/" + response.id()))
                .body(response);
    }

    @GetMapping({"/api/v1/merchants/me/api-keys", "/merchants/me/api-keys"})
    public ResponseEntity<List<ApiKeyResponse>> listApiKeys() {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        Merchant merchant = merchantService.findEntityByUserId(userId);
        List<ApiKeyResponse> response = apiKeyService.listApiKeys(merchant.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping({"/api/v1/merchants/me/api-keys/{id}", "/merchants/me/api-keys/{id}"})
    public ResponseEntity<Void> revokeApiKey(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        Merchant merchant = merchantService.findEntityByUserId(userId);
        apiKeyService.revokeApiKey(merchant.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
