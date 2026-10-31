package lk.swiftdrop.merchant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.merchant.dto.MerchantOrderResponse;
import lk.swiftdrop.merchant.entity.Merchant;
import lk.swiftdrop.merchant.entity.MerchantStatus;
import lk.swiftdrop.merchant.service.ApiKeyService;
import lk.swiftdrop.merchant.service.MerchantOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "management.health.db.enabled=false"
})
@AutoConfigureMockMvc
class MerchantOrderApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @MockitoBean
    private MerchantOrderService merchantOrderService;

    @Test
    @DisplayName("Missing X-API-Key header returns 401 Unauthorized")
    void missingApiKeyReturns401() throws Exception {
        String body = """
                {
                    "recipientName": "Kamal Perera",
                    "recipientPhone": "0771234567",
                    "recipientEmail": "kamal@example.com",
                    "pickupAddress": "123 Galle Rd, Colombo",
                    "dropoffAddress": "456 Kandy Rd, Kelaniya",
                    "pickupZone": "COLOMBO_SOUTH",
                    "dropoffZone": "COLOMBO_CENTRAL",
                    "weightKg": 2.5,
                    "size": "MEDIUM",
                    "speed": "STANDARD"
                }
                """;

        mockMvc.perform(post("/merchant-api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Invalid X-API-Key returns 401 Unauthorized")
    void invalidApiKeyReturns401() throws Exception {
        when(apiKeyService.validateApiKey("sd_live_invalidkey")).thenReturn(Optional.empty());

        String body = """
                {
                    "recipientName": "Kamal Perera",
                    "recipientPhone": "0771234567",
                    "recipientEmail": "kamal@example.com",
                    "pickupAddress": "123 Galle Rd, Colombo",
                    "dropoffAddress": "456 Kandy Rd, Kelaniya",
                    "pickupZone": "COLOMBO_SOUTH",
                    "dropoffZone": "COLOMBO_CENTRAL",
                    "weightKg": 2.5,
                    "size": "MEDIUM",
                    "speed": "STANDARD"
                }
                """;

        mockMvc.perform(post("/merchant-api/orders")
                        .header("X-API-Key", "sd_live_invalidkey")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Revoked X-API-Key returns 401 Unauthorized")
    void revokedApiKeyReturns401() throws Exception {
        when(apiKeyService.validateApiKey("sd_live_revokedkey")).thenReturn(Optional.empty());

        String body = """
                {
                    "recipientName": "Kamal Perera",
                    "recipientPhone": "0771234567",
                    "recipientEmail": "kamal@example.com",
                    "pickupAddress": "123 Galle Rd, Colombo",
                    "dropoffAddress": "456 Kandy Rd, Kelaniya",
                    "pickupZone": "COLOMBO_SOUTH",
                    "dropoffZone": "COLOMBO_CENTRAL",
                    "weightKg": 2.5,
                    "size": "MEDIUM",
                    "speed": "STANDARD"
                }
                """;

        mockMvc.perform(post("/merchant-api/orders")
                        .header("X-API-Key", "sd_live_revokedkey")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Valid X-API-Key belonging to approved merchant creates order and returns 201")
    void validKeyCreatesOrderSuccessfully() throws Exception {
        UUID merchantId = UUID.randomUUID();
        Merchant merchant = new Merchant(merchantId, UUID.randomUUID(), "Apex Store", "BR-12345",
                "Nimal Perera", "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.APPROVED);

        when(apiKeyService.validateApiKey("sd_live_validkey")).thenReturn(Optional.of(merchant));

        UUID orderId = UUID.randomUUID();
        UUID parcelId = UUID.randomUUID();
        MerchantOrderResponse orderResponse = new MerchantOrderResponse(
                orderId, parcelId, "SD-123456", "Kamal Perera", "0771234567",
                "456 Kandy Rd, Kelaniya", "CREATED", new BigDecimal("550.00"), "LKR", Instant.now()
        );

        when(merchantOrderService.createOrder(eq(merchant), any())).thenReturn(orderResponse);

        String body = """
                {
                    "recipientName": "Kamal Perera",
                    "recipientPhone": "0771234567",
                    "recipientEmail": "kamal@example.com",
                    "pickupAddress": "123 Galle Rd, Colombo",
                    "dropoffAddress": "456 Kandy Rd, Kelaniya",
                    "pickupZone": "COLOMBO_SOUTH",
                    "dropoffZone": "COLOMBO_CENTRAL",
                    "weightKg": 2.5,
                    "size": "MEDIUM",
                    "speed": "STANDARD",
                    "idempotencyKey": "order-req-001"
                }
                """;

        mockMvc.perform(post("/merchant-api/orders")
                        .header("X-API-Key", "sd_live_validkey")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackingNo").value("SD-123456"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.feeAmount").value(550.00));
    }

    @Test
    @DisplayName("Invalid order payload with negative weight returns 400 Bad Request")
    void invalidOrderPayloadReturns400() throws Exception {
        UUID merchantId = UUID.randomUUID();
        Merchant merchant = new Merchant(merchantId, UUID.randomUUID(), "Apex Store", "BR-12345",
                "Nimal Perera", "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.APPROVED);

        when(apiKeyService.validateApiKey("sd_live_validkey")).thenReturn(Optional.of(merchant));

        String invalidBody = """
                {
                    "recipientName": "Kamal Perera",
                    "recipientPhone": "",
                    "recipientEmail": "not-an-email",
                    "pickupAddress": "123 Galle Rd, Colombo",
                    "dropoffAddress": "456 Kandy Rd, Kelaniya",
                    "pickupZone": "COLOMBO_SOUTH",
                    "dropoffZone": "COLOMBO_CENTRAL",
                    "weightKg": -1.0,
                    "size": "MEDIUM",
                    "speed": "STANDARD"
                }
                """;

        mockMvc.perform(post("/merchant-api/orders")
                        .header("X-API-Key", "sd_live_validkey")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Downstream parcel validation error returns 400 Bad Request with details")
    void downstreamValidationErrorReturns400() throws Exception {
        UUID merchantId = UUID.randomUUID();
        Merchant merchant = new Merchant(merchantId, UUID.randomUUID(), "Apex Store", "BR-12345",
                "Nimal Perera", "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.APPROVED);

        when(apiKeyService.validateApiKey("sd_live_validkey")).thenReturn(Optional.of(merchant));
        when(merchantOrderService.createOrder(eq(merchant), any()))
                .thenThrow(new ValidationException("Weight 12.0 kg does not fit size SMALL (max 5 kg)"));

        String body = """
                {
                    "recipientName": "Kamal Perera",
                    "recipientPhone": "0771234567",
                    "recipientEmail": "kamal@example.com",
                    "pickupAddress": "123 Galle Rd, Colombo",
                    "dropoffAddress": "456 Kandy Rd, Kelaniya",
                    "pickupZone": "COLOMBO_SOUTH",
                    "dropoffZone": "COLOMBO_CENTRAL",
                    "weightKg": 12.0,
                    "size": "SMALL",
                    "speed": "STANDARD"
                }
                """;

        mockMvc.perform(post("/merchant-api/orders")
                        .header("X-API-Key", "sd_live_validkey")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Weight 12.0 kg does not fit size SMALL (max 5 kg)"));
    }
}
