package lk.swiftdrop.merchant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.merchant.dto.MerchantResponse;
import lk.swiftdrop.merchant.entity.MerchantStatus;
import lk.swiftdrop.merchant.service.MerchantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
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
class AdminMerchantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MerchantService merchantService;

    @Test
    @DisplayName("Non-admin user cannot access admin merchant listing")
    void nonAdminCannotListMerchants() throws Exception {
        mockMvc.perform(get("/api/v1/admin/merchants")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MERCHANT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated user cannot access admin merchant endpoints")
    void unauthenticatedCannotAccessAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/merchants"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin can list pending merchant registrations")
    void adminCanListMerchants() throws Exception {
        UUID merchantId = UUID.randomUUID();
        MerchantResponse merchant = new MerchantResponse(
                merchantId, UUID.randomUUID(), "Apex Electronics", "BR-1",
                "Nimal", "nimal@apex.lk", "+94771234567", "Colombo",
                MerchantStatus.PENDING, null, Instant.now(), null
        );

        when(merchantService.listMerchantsByStatus(eq(MerchantStatus.PENDING), any()))
                .thenReturn(new PageImpl<>(List.of(merchant)));

        mockMvc.perform(get("/api/v1/admin/merchants")
                        .param("status", "PENDING")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].businessName").value("Apex Electronics"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("Admin can approve merchant registration")
    void adminCanApproveMerchant() throws Exception {
        UUID merchantId = UUID.randomUUID();
        MerchantResponse response = new MerchantResponse(
                merchantId, UUID.randomUUID(), "Apex Electronics", "BR-1",
                "Nimal", "nimal@apex.lk", "+94771234567", "Colombo",
                MerchantStatus.APPROVED, null, Instant.now(), null
        );

        when(merchantService.approveMerchant(merchantId)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/merchants/" + merchantId + "/approve")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("Admin can reject merchant registration with reason")
    void adminCanRejectMerchant() throws Exception {
        UUID merchantId = UUID.randomUUID();
        MerchantResponse response = new MerchantResponse(
                merchantId, UUID.randomUUID(), "Apex Electronics", "BR-1",
                "Nimal", "nimal@apex.lk", "+94771234567", "Colombo",
                MerchantStatus.REJECTED, "Invalid business proof", Instant.now(), null
        );

        when(merchantService.rejectMerchant(merchantId, "Invalid business proof")).thenReturn(response);

        String body = """
                {
                    "reason": "Invalid business proof"
                }
                """;

        mockMvc.perform(patch("/api/v1/admin/merchants/" + merchantId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Invalid business proof"));
    }
}
