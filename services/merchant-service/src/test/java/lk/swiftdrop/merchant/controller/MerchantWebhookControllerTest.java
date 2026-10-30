package lk.swiftdrop.merchant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import lk.swiftdrop.merchant.dto.UpdateWebhookRequest;
import lk.swiftdrop.merchant.dto.WebhookCreatedResponse;
import lk.swiftdrop.merchant.dto.WebhookSettingsResponse;
import lk.swiftdrop.merchant.entity.Merchant;
import lk.swiftdrop.merchant.entity.MerchantStatus;
import lk.swiftdrop.merchant.service.MerchantService;
import lk.swiftdrop.merchant.service.WebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "management.health.db.enabled=false"
})
@AutoConfigureMockMvc
class MerchantWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebhookService webhookService;

    @MockitoBean
    private MerchantService merchantService;

    @Test
    @DisplayName("Merchant can view webhook settings without exposing secret")
    void getWebhookSettingsSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        Merchant merchant = new Merchant(merchantId, userId, "Apex", "BR-1", "Nimal",
                "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.APPROVED);

        when(merchantService.findEntityByUserId(userId)).thenReturn(merchant);

        WebhookSettingsResponse response = new WebhookSettingsResponse(
                UUID.randomUUID(), "https://apex.lk/webhook", true, Instant.now(), null
        );
        when(webhookService.getWebhookSettings(merchantId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/merchants/me/webhook")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_MERCHANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.webhookUrl").value("https://apex.lk/webhook"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.secret").doesNotExist());
    }

    @Test
    @DisplayName("Merchant can update webhook URL and receive secret")
    void updateWebhookSettingsSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        Merchant merchant = new Merchant(merchantId, userId, "Apex", "BR-1", "Nimal",
                "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.APPROVED);

        when(merchantService.findEntityByUserId(userId)).thenReturn(merchant);

        WebhookCreatedResponse response = new WebhookCreatedResponse(
                UUID.randomUUID(), "https://apex.lk/new-webhook", "whsec_newsecret123", true, Instant.now()
        );
        when(webhookService.updateWebhookSettings(eq(merchantId), any(UpdateWebhookRequest.class)))
                .thenReturn(response);

        String body = """
                {
                    "webhookUrl": "https://apex.lk/new-webhook",
                    "rotateSecret": false
                }
                """;

        mockMvc.perform(put("/api/v1/merchants/me/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_MERCHANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.webhookUrl").value("https://apex.lk/new-webhook"))
                .andExpect(jsonPath("$.secret").value("whsec_newsecret123"));
    }

    @Test
    @DisplayName("Invalid webhook URL returns 400 Bad Request")
    void invalidWebhookUrlReturns400() throws Exception {
        UUID userId = UUID.randomUUID();

        String body = """
                {
                    "webhookUrl": "not-a-valid-url",
                    "rotateSecret": false
                }
                """;

        mockMvc.perform(put("/api/v1/merchants/me/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_MERCHANT"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
