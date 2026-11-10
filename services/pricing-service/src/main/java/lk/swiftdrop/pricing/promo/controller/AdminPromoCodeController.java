package lk.swiftdrop.pricing.promo.controller;

import jakarta.validation.Valid;
import lk.swiftdrop.pricing.promo.dto.CreatePromoCodeRequest;
import lk.swiftdrop.pricing.promo.dto.PromoCodeResponse;
import lk.swiftdrop.pricing.promo.dto.UpdatePromoCodeRequest;
import lk.swiftdrop.pricing.promo.service.PromoCodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Promo code management, admins only. */
@RestController
@RequestMapping("/api/v1/admin/promo-codes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromoCodeController {

    private final PromoCodeService promoCodeService;

    public AdminPromoCodeController(PromoCodeService promoCodeService) {
        this.promoCodeService = promoCodeService;
    }

    @GetMapping
    public List<PromoCodeResponse> list() {
        return promoCodeService.allPromoCodes();
    }

    @GetMapping("/{id}")
    public PromoCodeResponse get(@PathVariable UUID id) {
        return promoCodeService.promoCode(id);
    }

    @PostMapping
    public ResponseEntity<PromoCodeResponse> create(@Valid @RequestBody CreatePromoCodeRequest request) {
        PromoCodeResponse created = promoCodeService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/promo-codes/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public PromoCodeResponse update(@PathVariable UUID id, @Valid @RequestBody UpdatePromoCodeRequest request) {
        return promoCodeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        promoCodeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
