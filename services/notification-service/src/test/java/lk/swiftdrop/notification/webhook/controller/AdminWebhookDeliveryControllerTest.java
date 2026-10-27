package lk.swiftdrop.notification.webhook.controller;

import lk.swiftdrop.notification.client.MerchantServiceClient;
import lk.swiftdrop.notification.client.dto.MerchantWebhookResponse;
import lk.swiftdrop.notification.webhook.entity.WebhookDelivery;
import lk.swiftdrop.notification.webhook.entity.WebhookDeliveryStatus;
import lk.swiftdrop.notification.webhook.mapper.WebhookDeliveryMapper;
import lk.swiftdrop.notification.webhook.service.WebhookDeliveryService;
import lk.swiftdrop.notification.webhook.service.WebhookSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminWebhookDeliveryController.class)
@Import(WebhookDeliveryMapper.class)
class AdminWebhookDeliveryControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private WebhookDeliveryService deliveryService;
    @MockitoBean private WebhookSender webhookSender;
    @MockitoBean private MerchantServiceClient merchantServiceClient;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListWebhookDeliveries() throws Exception {
        when(deliveryService.listAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildDelivery())));
        mockMvc.perform(get("/api/v1/admin/webhook-deliveries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].status").value("FAILED"));
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    void dispatcherCanListWebhookDeliveries() throws Exception {
        when(deliveryService.listAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/api/v1/admin/webhook-deliveries"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/webhook-deliveries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/webhook-deliveries"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COURIER")
    void courierCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/webhook-deliveries"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void replayReturns404ForNonexistentDelivery() throws Exception {
        when(deliveryService.findById(any(UUID.class))).thenReturn(Optional.empty());
        mockMvc.perform(post("/api/v1/admin/webhook-deliveries/" + UUID.randomUUID() + "/replay"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanReplayDelivery() throws Exception {
        WebhookDelivery original = buildDelivery();
        when(deliveryService.findById(any(UUID.class))).thenReturn(Optional.of(original));
        when(merchantServiceClient.getWebhookConfig(any()))
                .thenReturn(Optional.of(new MerchantWebhookResponse("https://store.example.com/hook", "sec")));
        when(webhookSender.send(any(), any(), any(), any()))
                .thenReturn(new WebhookSender.WebhookSendResult(true, 200, "OK", null));
        WebhookDelivery replayed = buildDelivery();
        replayed.markSuccess(200, "OK");
        when(deliveryService.recordSuccess(any(), any(), any(), any(), any(int.class), any(), any(int.class), any()))
                .thenReturn(replayed);

        mockMvc.perform(post("/api/v1/admin/webhook-deliveries/" + UUID.randomUUID() + "/replay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotReplayWebhook() throws Exception {
        mockMvc.perform(post("/api/v1/admin/webhook-deliveries/" + UUID.randomUUID() + "/replay"))
                .andExpect(status().isForbidden());
    }

    private WebhookDelivery buildDelivery() {
        return new WebhookDelivery(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "https://store.example.com/hook", 1, "{\"test\":true}");
    }
}
