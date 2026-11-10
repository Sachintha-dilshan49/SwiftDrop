package lk.swiftdrop.merchant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.merchant.dto.MerchantOrderResponse;
import lk.swiftdrop.merchant.entity.Merchant;
import lk.swiftdrop.merchant.entity.MerchantStatus;
import lk.swiftdrop.merchant.service.MerchantOrderService;
import lk.swiftdrop.merchant.service.MerchantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "management.health.db.enabled=false"
})
@AutoConfigureMockMvc
class MerchantDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MerchantOrderService merchantOrderService;

    @MockitoBean
    private MerchantService merchantService;

    @Test
    @DisplayName("Merchant can list own orders through dashboard")
    void listMyOrdersSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        Merchant merchant = new Merchant(merchantId, userId, "Apex", "BR-1", "Nimal",
                "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.APPROVED);

        when(merchantService.findEntityByUserId(userId)).thenReturn(merchant);

        MerchantOrderResponse order = new MerchantOrderResponse(
                UUID.randomUUID(), UUID.randomUUID(), "SD-111222", "Kamal", "0712345678",
                "Kelaniya", "CREATED", new BigDecimal("450.00"), "LKR", Instant.now()
        );

        when(merchantOrderService.listOrdersForMerchant(eq(merchantId), any()))
                .thenReturn(new PageImpl<>(List.of(order)));

        mockMvc.perform(get("/api/v1/merchants/me/orders")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_MERCHANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].trackingNo").value("SD-111222"))
                .andExpect(jsonPath("$.content[0].status").value("CREATED"));
    }

    @Test
    @DisplayName("Merchant can retrieve specific order by ID")
    void getOrderByIdSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Merchant merchant = new Merchant(merchantId, userId, "Apex", "BR-1", "Nimal",
                "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.APPROVED);

        when(merchantService.findEntityByUserId(userId)).thenReturn(merchant);

        MerchantOrderResponse order = new MerchantOrderResponse(
                orderId, UUID.randomUUID(), "SD-111222", "Kamal", "0712345678",
                "Kelaniya", "CREATED", new BigDecimal("450.00"), "LKR", Instant.now()
        );

        when(merchantOrderService.getOrderById(merchantId, orderId)).thenReturn(order);

        mockMvc.perform(get("/api/v1/merchants/me/orders/" + orderId)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_MERCHANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.trackingNo").value("SD-111222"));
    }
}
