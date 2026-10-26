package lk.swiftdrop.dispatch.delivery;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.common.error.ForbiddenException;
import lk.swiftdrop.dispatch.assignment.AssignmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

    private static RequestPostProcessor user(String role) {
        return jwt().jwt(token -> token.subject(USER_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
