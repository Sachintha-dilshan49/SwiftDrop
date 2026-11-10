package lk.swiftdrop.merchant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class MerchantOrderServiceTest {

    @Mock
    private MerchantOrderRepository merchantOrderRepository;

    @Mock
    private ParcelServiceClient parcelServiceClient;

    @InjectMocks
    private MerchantOrderService merchantOrderService;

    @Test
    @DisplayName("Create order fails if merchant is not APPROVED")
    void createOrderFailsForUnapprovedMerchant() {
        Merchant merchant = new Merchant(UUID.randomUUID(), UUID.randomUUID(), "Apex", "BR-1", "Nimal",
                "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.PENDING);
        MerchantCreateOrderRequest request = new MerchantCreateOrderRequest(
                "Kamal Silva", "0712345678", "kamal@example.com", "123 Galle Rd, Colombo", "456 Kandy Rd, Kelaniya",
                "COLOMBO_SOUTH", "COLOMBO_CENTRAL", new BigDecimal("2.50"), "MEDIUM", "STANDARD", null);

        assertThatThrownBy(() -> merchantOrderService.createOrder(merchant, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Merchant is not approved");

        verify(parcelServiceClient, never()).createParcel(any());
    }

    @Test
    @DisplayName("Create order is idempotent when matching idempotencyKey exists")
    void createOrderIdempotent() {
        UUID merchantId = UUID.randomUUID();
        Merchant merchant = new Merchant(merchantId, UUID.randomUUID(), "Apex", "BR-1", "Nimal",
                "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.APPROVED);

        MerchantCreateOrderRequest request = new MerchantCreateOrderRequest(
                "Kamal Silva", "0712345678", "kamal@example.com", "123 Galle Rd, Colombo", "456 Kandy Rd, Kelaniya",
                "COLOMBO_SOUTH", "COLOMBO_CENTRAL", new BigDecimal("2.50"), "MEDIUM", "STANDARD", "idemp-key-123");

        MerchantOrder existingOrder = new MerchantOrder(
                UUID.randomUUID(), merchantId, UUID.randomUUID(), "SD-100200",
                "Kamal Silva", "0712345678", "456 Kandy Rd, Kelaniya",
                "CREATED", new BigDecimal("450.00"), "LKR", "idemp-key-123"
        );
        existingOrder.setCreatedAt(Instant.now());

        when(merchantOrderRepository.findByMerchantIdAndIdempotencyKey(merchantId, "idemp-key-123"))
                .thenReturn(Optional.of(existingOrder));

        MerchantOrderResponse response = merchantOrderService.createOrder(merchant, request);

        assertThat(response).isNotNull();
        assertThat(response.trackingNo()).isEqualTo("SD-100200");
        verify(parcelServiceClient, never()).createParcel(any());
    }

    @Test
    @DisplayName("Create order calls parcel-service and saves new merchant order")
    void createOrderSuccess() {
        UUID merchantId = UUID.randomUUID();
        Merchant merchant = new Merchant(merchantId, UUID.randomUUID(), "Apex", "BR-1", "Nimal",
                "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.APPROVED);

        MerchantCreateOrderRequest request = new MerchantCreateOrderRequest(
                "Kamal Silva", "0712345678", "kamal@example.com", "123 Galle Rd, Colombo", "456 Kandy Rd, Kelaniya",
                "COLOMBO_SOUTH", "COLOMBO_CENTRAL", new BigDecimal("2.50"), "MEDIUM", "STANDARD", "new-idemp-key");

        UUID parcelId = UUID.randomUUID();
        CreateParcelInternalResponse parcelResponse = new CreateParcelInternalResponse(
                parcelId, "SD-789012", "CREATED", new BigDecimal("650.00"), "LKR");

        when(merchantOrderRepository.findByMerchantIdAndIdempotencyKey(merchantId, "new-idemp-key"))
                .thenReturn(Optional.empty());
        when(parcelServiceClient.createParcel(any(CreateParcelInternalRequest.class)))
                .thenReturn(parcelResponse);
        when(merchantOrderRepository.save(any(MerchantOrder.class))).thenAnswer(inv -> {
            MerchantOrder order = inv.getArgument(0);
            order.setCreatedAt(Instant.now());
            return order;
        });

        MerchantOrderResponse response = merchantOrderService.createOrder(merchant, request);

        assertThat(response).isNotNull();
        assertThat(response.parcelId()).isEqualTo(parcelId);
        assertThat(response.trackingNo()).isEqualTo("SD-789012");
        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.feeAmount()).isEqualTo(new BigDecimal("650.00"));
        verify(parcelServiceClient).createParcel(any(CreateParcelInternalRequest.class));
        verify(merchantOrderRepository).save(any(MerchantOrder.class));
    }

    @Test
    @DisplayName("List orders for merchant enforces data isolation")
    void listOrdersForMerchant() {
        UUID merchantId = UUID.randomUUID();
        MerchantOrder order = new MerchantOrder(
                UUID.randomUUID(), merchantId, UUID.randomUUID(), "SD-111222",
                "Kamal", "0712345678", "Kelaniya", "CREATED", new BigDecimal("500.00"), "LKR", null);
        order.setCreatedAt(Instant.now());

        when(merchantOrderRepository.findAllByMerchantIdOrderByCreatedAtDesc(merchantId, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(order)));

        Page<MerchantOrderResponse> result = merchantOrderService.listOrdersForMerchant(merchantId, PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).trackingNo()).isEqualTo("SD-111222");
    }

    @Test
    @DisplayName("Get order by ID throws NotFoundException if order does not belong to merchant")
    void getOrderByIdOwnershipCheck() {
        UUID merchantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(merchantOrderRepository.findByIdAndMerchantId(orderId, merchantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> merchantOrderService.getOrderById(merchantId, orderId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Order not found");
    }
}
