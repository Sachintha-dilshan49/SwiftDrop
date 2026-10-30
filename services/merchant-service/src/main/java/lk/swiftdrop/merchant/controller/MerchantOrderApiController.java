package lk.swiftdrop.merchant.controller;

import jakarta.validation.Valid;
import java.net.URI;
import lk.swiftdrop.merchant.dto.MerchantCreateOrderRequest;
import lk.swiftdrop.merchant.dto.MerchantOrderResponse;
import lk.swiftdrop.merchant.entity.Merchant;
import lk.swiftdrop.merchant.service.MerchantOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/merchant-api/orders")
public class MerchantOrderApiController {

    private final MerchantOrderService merchantOrderService;

    public MerchantOrderApiController(MerchantOrderService merchantOrderService) {
        this.merchantOrderService = merchantOrderService;
    }

    @PostMapping
    public ResponseEntity<MerchantOrderResponse> createOrder(
            @AuthenticationPrincipal Merchant merchant,
            @Valid @RequestBody MerchantCreateOrderRequest request) {
        MerchantOrderResponse response = merchantOrderService.createOrder(merchant, request);
        return ResponseEntity.created(URI.create("/merchant-api/orders/" + response.id()))
                .body(response);
    }
}
