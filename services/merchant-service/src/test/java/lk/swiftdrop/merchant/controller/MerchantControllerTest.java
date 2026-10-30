package lk.swiftdrop.merchant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import lk.swiftdrop.merchant.dto.MerchantResponse;
import lk.swiftdrop.merchant.dto.RegisterMerchantRequest;
import lk.swiftdrop.merchant.entity.MerchantStatus;
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
class MerchantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MerchantService merchantService;

    @Test
    @DisplayName("Public merchant registration succeeds with 201 Created")
    void registerMerchantSuccess() throws Exception {
        UUID merchantId = UUID.randomUUID();
        MerchantResponse response = new MerchantResponse(
                merchantId, null, "Apex Electronics", "BR-12345",
                "Nimal Perera", "nimal@apex.lk", "+94771234567",
                "123 Galle Road, Colombo 03", MerchantStatus.PENDING,
                null, Instant.now(), null
        );

        when(merchantService.register(any(RegisterMerchantRequest.class), any())).thenReturn(response);

        String body = """
                {
                    "businessName": "Apex Electronics",
                    "businessRegNo": "BR-12345",
                    "contactName": "Nimal Perera",
                    "email": "nimal@apex.lk",
                    "phone": "+94771234567",
                    "address": "123 Galle Road, Colombo 03"
                }
                """;

        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(merchantId.toString()))
                .andExpect(jsonPath("$.businessName").value("Apex Electronics"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("Registration rejects missing required fields with 400 Bad Request")
    void registerMissingFields() throws Exception {
        String invalidBody = """
                {
                    "businessName": "",
                    "businessRegNo": "BR-12345",
                    "contactName": "",
                    "email": "invalid-email-format",
                    "phone": "",
                    "address": ""
                }
                """;

        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Unauthenticated request to /api/v1/merchants/me returns 401 Unauthorized")
    void getMyProfileUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authenticated merchant can view own profile")
    void getMyProfileAuthenticated() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        MerchantResponse response = new MerchantResponse(
                merchantId, userId, "Apex Electronics", "BR-12345",
                "Nimal Perera", "nimal@apex.lk", "+94771234567",
                "123 Galle Road, Colombo 03", MerchantStatus.APPROVED,
                null, Instant.now(), null
        );

        when(merchantService.getMerchantByUserId(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/merchants/me")
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_MERCHANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Apex Electronics"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}
