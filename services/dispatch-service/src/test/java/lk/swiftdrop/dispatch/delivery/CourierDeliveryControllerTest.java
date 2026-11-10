package lk.swiftdrop.dispatch.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.common.error.ForbiddenException;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.dispatch.assignment.AssignmentStatus;
import lk.swiftdrop.dispatch.proof.ProofPhoto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(CourierDeliveryController.class)
class CourierDeliveryControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourierDeliveryService courierDeliveryService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void courierGetsOwnListForToday() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        given(courierDeliveryService.getTodaysDeliveries(USER_ID)).willReturn(List.of(new CourierDeliveryResponse(
                assignmentId, UUID.randomUUID(), AssignmentStatus.ASSIGNED, Instant.now())));

        mockMvc.perform(get("/api/v1/courier/deliveries/today").with(user("COURIER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assignmentId").value(assignmentId.toString()))
                .andExpect(jsonPath("$[0].status").value("ASSIGNED"));
    }

    @Test
    void courierGetsOwnDelivery() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        given(courierDeliveryService.getDelivery(USER_ID, assignmentId)).willReturn(new CourierDeliveryResponse(
                assignmentId, UUID.randomUUID(), AssignmentStatus.IN_PROGRESS, Instant.now()));

        mockMvc.perform(get("/api/v1/courier/deliveries/{id}", assignmentId).with(user("COURIER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void courierCannotViewAnotherCouriersDelivery() throws Exception {
        UUID othersAssignment = UUID.randomUUID();
        given(courierDeliveryService.getDelivery(USER_ID, othersAssignment)).willThrow(new ForbiddenException(
                "not-your-delivery", "Not your delivery", "Delivery is assigned to another courier."));

        mockMvc.perform(get("/api/v1/courier/deliveries/{id}", othersAssignment).with(user("COURIER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/not-your-delivery"));
    }

    @Test
    void dispatcherCannotUseCourierEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/courier/deliveries/today").with(user("DISPATCHER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(courierDeliveryService);
    }

    @Test
    void rejectsRequestWithoutTokenWith401() throws Exception {
        mockMvc.perform(get("/api/v1/courier/deliveries/today"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(courierDeliveryService);
    }

    @Test
    void rejectsDeliveryIdThatIsNotAUuidWith400() throws Exception {
        mockMvc.perform(get("/api/v1/courier/deliveries/{id}", "abc").with(user("COURIER")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(courierDeliveryService);
    }

    @Test
    void courierUpdatesOwnDeliveryStatus() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        UpdateDeliveryStatusRequest request =
                new UpdateDeliveryStatusRequest(ParcelStatus.DELIVERED, "At the gate", "Nimali", null);
        given(courierDeliveryService.updateStatus(USER_ID, assignmentId, request, null)).willReturn(
                new CourierDeliveryResponse(assignmentId, UUID.randomUUID(), AssignmentStatus.COMPLETED, Instant.now()));

        mockMvc.perform(multipart("/api/v1/courier/deliveries/{id}/status", assignmentId)
                        .param("status", "DELIVERED").param("note", "At the gate").param("recipientName", "Nimali")
                        .with(user("COURIER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void photoIsPassedToTheServiceAsProof() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01};
        given(courierDeliveryService.updateStatus(eq(USER_ID), eq(assignmentId), any(), any()))
                .willReturn(new CourierDeliveryResponse(
                        assignmentId, UUID.randomUUID(), AssignmentStatus.COMPLETED, Instant.now()));

        mockMvc.perform(multipart("/api/v1/courier/deliveries/{id}/status", assignmentId)
                        .file(new MockMultipartFile("photo", "door.jpg", "image/jpeg", jpeg))
                        .param("status", "DELIVERED").with(user("COURIER")))
                .andExpect(status().isOk());

        ArgumentCaptor<ProofPhoto> photo = ArgumentCaptor.forClass(ProofPhoto.class);
        verify(courierDeliveryService).updateStatus(eq(USER_ID), eq(assignmentId), any(), photo.capture());
        assertThat(photo.getValue().content()).isEqualTo(jpeg);
    }

    @Test
    void rejectsStatusUpdateWithoutStatusWith400() throws Exception {
        mockMvc.perform(multipart("/api/v1/courier/deliveries/{id}/status", UUID.randomUUID())
                        .param("note", "no status").with(user("COURIER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
        verifyNoInteractions(courierDeliveryService);
    }

    @Test
    void rejectsUnknownStatusValueWith400() throws Exception {
        mockMvc.perform(multipart("/api/v1/courier/deliveries/{id}/status", UUID.randomUUID())
                        .param("status", "TELEPORTED").with(user("COURIER")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(courierDeliveryService);
    }

    @Test
    void dispatcherCannotUpdateCourierDeliveryStatus() throws Exception {
        mockMvc.perform(multipart("/api/v1/courier/deliveries/{id}/status", UUID.randomUUID())
                        .param("status", "PICKED_UP").with(user("DISPATCHER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(courierDeliveryService);
    }

    private static RequestPostProcessor user(String role) {
        return jwt().jwt(token -> token.subject(USER_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
