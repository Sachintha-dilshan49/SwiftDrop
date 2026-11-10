package lk.swiftdrop.merchant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.merchant.dto.ApiKeyCreatedResponse;
import lk.swiftdrop.merchant.dto.ApiKeyResponse;
import lk.swiftdrop.merchant.dto.CreateApiKeyRequest;
import lk.swiftdrop.merchant.entity.Merchant;
import lk.swiftdrop.merchant.entity.MerchantStatus;
import lk.swiftdrop.merchant.service.ApiKeyService;
import lk.swiftdrop.merchant.service.MerchantService;
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
class MerchantApiKeyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @MockitoBean
    private MerchantService merchantService;

    @Test
    @DisplayName("Merchant can create API key and receive raw key once")
    void createApiKeySuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        Merchant merchant = new Merchant(merchantId, userId, "Apex", "BR-1", "Nimal",
                "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.APPROVED);

        when(merchantService.findEntityByUserId(userId)).thenReturn(merchant);

        UUID keyId = UUID.randomUUID();
        ApiKeyCreatedResponse createdResponse = new ApiKeyCreatedResponse(
                keyId, "Live Store Key", "sd_live_12345678",
                "sd_live_1234567890abcdef1234567890abcdef", null, Instant.now()
        );

        when(apiKeyService.createApiKey(eq(merchantId), any(CreateApiKeyRequest.class)))
                .thenReturn(createdResponse);

        String body = """
                {
                    "name": "Live Store Key"
                }
                """;

        mockMvc.perform(post("/api/v1/merchants/me/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_MERCHANT"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(keyId.toString()))
                .andExpect(jsonPath("$.name").value("Live Store Key"))
                .andExpect(jsonPath("$.rawApiKey").value("sd_live_1234567890abcdef1234567890abcdef"))
                .andExpect(jsonPath("$.keyPrefix").value("sd_live_12345678"));
    }

    @Test
    @DisplayName("Merchant can list own API keys")
    void listApiKeysSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        Merchant merchant = new Merchant(merchantId, userId, "Apex", "BR-1", "Nimal",
                "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.APPROVED);

        when(merchantService.findEntityByUserId(userId)).thenReturn(merchant);

        ApiKeyResponse keyResponse = new ApiKeyResponse(
                UUID.randomUUID(), "Live Key", "sd_live_12345678", false, null, null, Instant.now()
        );
        when(apiKeyService.listApiKeys(merchantId)).thenReturn(List.of(keyResponse));

        mockMvc.perform(get("/api/v1/merchants/me/api-keys")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_MERCHANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Live Key"))
                .andExpect(jsonPath("$[0].keyPrefix").value("sd_live_12345678"))
                .andExpect(jsonPath("$[0].revoked").value(false));
    }

    @Test
    @DisplayName("Merchant can revoke own API key")
    void revokeApiKeySuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();
        Merchant merchant = new Merchant(merchantId, userId, "Apex", "BR-1", "Nimal",
                "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.APPROVED);

        when(merchantService.findEntityByUserId(userId)).thenReturn(merchant);

        mockMvc.perform(delete("/api/v1/merchants/me/api-keys/" + keyId)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_MERCHANT"))))
                .andExpect(status().isNoContent());

        verify(apiKeyService).revokeApiKey(merchantId, keyId);
    }
}
