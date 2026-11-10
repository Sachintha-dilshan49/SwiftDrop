package lk.swiftdrop.merchant.service;

import java.util.Optional;
import java.util.UUID;
import lk.swiftdrop.common.error.ForbiddenException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.merchant.client.CreateParcelInternalRequest;
import lk.swiftdrop.merchant.client.CreateParcelInternalResponse;
import lk.swiftdrop.merchant.client.ParcelServiceClient;
import lk.swiftdrop.merchant.dto.MerchantCreateOrderRequest;
import lk.swiftdrop.merchant.dto.MerchantOrderResponse;
import lk.swiftdrop.merchant.entity.Merchant;
import lk.swiftdrop.merchant.entity.MerchantOrder;
import lk.swiftdrop.merchant.entity.MerchantStatus;
import lk.swiftdrop.merchant.repository.MerchantOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantOrderService {

    private static final Logger log = LoggerFactory.getLogger(MerchantOrderService.class);

    private final MerchantOrderRepository merchantOrderRepository;
    private final ParcelServiceClient parcelServiceClient;

    public MerchantOrderService(MerchantOrderRepository merchantOrderRepository,
                                ParcelServiceClient parcelServiceClient) {
        this.merchantOrderRepository = merchantOrderRepository;
        this.parcelServiceClient = parcelServiceClient;
    }

    @Transactional
    public MerchantOrderResponse createOrder(Merchant merchant, MerchantCreateOrderRequest request) {
        if (merchant.getStatus() != MerchantStatus.APPROVED) {
            log.warn("Merchant {} attempted to create order but is {}", merchant.getId(), merchant.getStatus());
            throw new ForbiddenException("Merchant is not approved. Current status: " + merchant.getStatus());
        }

        // Idempotency check
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Optional<MerchantOrder> existing = merchantOrderRepository.findByMerchantIdAndIdempotencyKey(
                    merchant.getId(), request.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Idempotent request matched for merchantId: {}, idempotencyKey: {}",
                        merchant.getId(), request.idempotencyKey());
                return MerchantOrderResponse.from(existing.get());
            }
        }

        CreateParcelInternalRequest parcelRequest = new CreateParcelInternalRequest(
                merchant.getId(),
                request.pickupAddress(),
                request.dropoffAddress(),
                request.recipientName(),
                request.recipientPhone(),
                request.recipientEmail(),
                request.pickupZone(),
                request.dropoffZone(),
                request.size(),
                request.speed(),
                request.weightKg()
        );

        CreateParcelInternalResponse parcelResponse = parcelServiceClient.createParcel(parcelRequest);

        MerchantOrder order = new MerchantOrder(
                UUID.randomUUID(),
                merchant.getId(),
                parcelResponse.id(),
                parcelResponse.trackingNo(),
                request.recipientName(),
                request.recipientPhone(),
                request.dropoffAddress(),
                parcelResponse.status() != null ? parcelResponse.status() : "CREATED",
                parcelResponse.feeAmount(),
                parcelResponse.feeCurrency() != null ? parcelResponse.feeCurrency() : "LKR",
                request.idempotencyKey()
        );

        MerchantOrder saved = merchantOrderRepository.save(order);
        log.info("Order created successfully for merchantId: {}, orderId: {}, trackingNo: {}",
                merchant.getId(), saved.getId(), saved.getTrackingNo());

        return MerchantOrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<MerchantOrderResponse> listOrdersForMerchant(UUID merchantId, Pageable pageable) {
        return merchantOrderRepository.findAllByMerchantIdOrderByCreatedAtDesc(merchantId, pageable)
                .map(MerchantOrderResponse::from);
    }

    @Transactional(readOnly = true)
    public MerchantOrderResponse getOrderById(UUID merchantId, UUID orderId) {
        MerchantOrder order = merchantOrderRepository.findByIdAndMerchantId(orderId, merchantId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        return MerchantOrderResponse.from(order);
    }
}
