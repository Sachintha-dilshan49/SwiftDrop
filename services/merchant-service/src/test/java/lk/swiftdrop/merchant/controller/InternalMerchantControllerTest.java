package lk.swiftdrop.merchant.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lk.swiftdrop.common.internal.InternalTokenAuthenticationFilter;
import lk.swiftdrop.common.internal.InternalTokenTestContextCustomizerFactory;
import lk.swiftdrop.merchant.dto.InternalMerchantWebhookResponse;
import lk.swiftdrop.merchant.service.WebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "management.health.db.enabled=false"
})
@AutoConfigureMockMvc
class InternalMerchantControllerTest {

    private static final String INTERNAL_TOKEN = InternalTokenTestContextCustomizerFactory.TEST_TOKEN;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebhookService webhookService;

    @Test
    @DisplayName("Internal service call without internal token returns 401 Unauthorized")
    void getMerchantWebhookWithoutTokenReturns401() throws Exception {
        UUID merchantId = UUID.randomUUID();
        mockMvc.perform(get("/internal/merchants/" + merchantId + "/webhook"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Internal service call with valid internal token retrieves webhook endpoint and secret for Role 6")
    void getMerchantWebhookInternal() throws Exception {
        UUID merchantId = UUID.randomUUID();
        InternalMerchantWebhookResponse response = new InternalMerchantWebhookResponse(
                "https://merchant.example.com/webhooks",
                "whsec_test_secret_abc123"
        );

        when(webhookService.getInternalWebhook(merchantId)).thenReturn(response);

        mockMvc.perform(get("/internal/merchants/" + merchantId + "/webhook")
                        .header(InternalTokenAuthenticationFilter.HEADER, INTERNAL_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.webhookUrl").value("https://merchant.example.com/webhooks"))
                .andExpect(jsonPath("$.secret").value("whsec_test_secret_abc123"));
    }
}
