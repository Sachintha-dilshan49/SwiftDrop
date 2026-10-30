package lk.swiftdrop.merchant.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lk.swiftdrop.merchant.dto.MerchantResponse;
import lk.swiftdrop.merchant.dto.RegisterMerchantRequest;
import lk.swiftdrop.merchant.security.SecurityUtils;
import lk.swiftdrop.merchant.service.MerchantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping({"/api/v1/merchants/register", "/merchants/register"})
    public ResponseEntity<MerchantResponse> register(@Valid @RequestBody RegisterMerchantRequest request) {
        UUID authenticatedUserId = SecurityUtils.getOptionalAuthenticatedUserId();
        MerchantResponse response = merchantService.register(request, authenticatedUserId);
        return ResponseEntity.created(URI.create("/api/v1/merchants/me"))
                .body(response);
    }

    @GetMapping({"/api/v1/merchants/me", "/merchants/me"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MerchantResponse> getMyMerchantProfile() {
        UUID authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        MerchantResponse response = merchantService.getMerchantByUserId(authenticatedUserId);
        return ResponseEntity.ok(response);
    }
}
