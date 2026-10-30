package lk.swiftdrop.merchant.controller;

import java.util.UUID;
import lk.swiftdrop.merchant.dto.MerchantOrderResponse;
import lk.swiftdrop.merchant.entity.Merchant;
import lk.swiftdrop.merchant.security.SecurityUtils;
import lk.swiftdrop.merchant.service.MerchantOrderService;
import lk.swiftdrop.merchant.service.MerchantService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
public class MerchantDashboardController {

    private final MerchantOrderService merchantOrderService;
    private final MerchantService merchantService;

    public MerchantDashboardController(MerchantOrderService merchantOrderService,
                                       MerchantService merchantService) {
        this.merchantOrderService = merchantOrderService;
        this.merchantService = merchantService;
    }

    @GetMapping({"/api/v1/merchants/me/orders", "/merchants/me/orders"})
    public ResponseEntity<Page<MerchantOrderResponse>> listMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        Merchant merchant = merchantService.findEntityByUserId(userId);
        Pageable pageable = PageRequest.of(page, size);
        Page<MerchantOrderResponse> orders = merchantOrderService.listOrdersForMerchant(merchant.getId(), pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping({"/api/v1/merchants/me/orders/{id}", "/merchants/me/orders/{id}"})
    public ResponseEntity<MerchantOrderResponse> getOrderById(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        Merchant merchant = merchantService.findEntityByUserId(userId);
        MerchantOrderResponse order = merchantOrderService.getOrderById(merchant.getId(), id);
        return ResponseEntity.ok(order);
    }
}
