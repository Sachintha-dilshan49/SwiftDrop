package lk.swiftdrop.merchant.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lk.swiftdrop.merchant.dto.MerchantResponse;
import lk.swiftdrop.merchant.dto.RejectMerchantRequest;
import lk.swiftdrop.merchant.entity.MerchantStatus;
import lk.swiftdrop.merchant.service.MerchantService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@PreAuthorize("hasRole('ADMIN')")
public class AdminMerchantController {

    private final MerchantService merchantService;

    public AdminMerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @GetMapping({"/api/v1/admin/merchants", "/admin/merchants"})
    public ResponseEntity<Page<MerchantResponse>> listMerchants(
            @RequestParam(required = false, defaultValue = "PENDING") MerchantStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MerchantResponse> merchants = merchantService.listMerchantsByStatus(status, pageable);
        return ResponseEntity.ok(merchants);
    }

    @PatchMapping({"/api/v1/admin/merchants/{id}/approve", "/admin/merchants/{id}/approve"})
    public ResponseEntity<MerchantResponse> approveMerchant(@PathVariable UUID id) {
        MerchantResponse response = merchantService.approveMerchant(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping({"/api/v1/admin/merchants/{id}/reject", "/admin/merchants/{id}/reject"})
    public ResponseEntity<MerchantResponse> rejectMerchant(
            @PathVariable UUID id,
            @Valid @RequestBody RejectMerchantRequest request) {
        MerchantResponse response = merchantService.rejectMerchant(id, request.reason());
        return ResponseEntity.ok(response);
    }
}
