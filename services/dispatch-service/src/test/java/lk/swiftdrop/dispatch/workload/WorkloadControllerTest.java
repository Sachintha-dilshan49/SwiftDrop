package lk.swiftdrop.dispatch.workload;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import lk.swiftdrop.dispatch.courier.CourierStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(WorkloadController.class)
class WorkloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkloadService workloadService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void dispatcherSeesActiveDeliveriesPerCourier() throws Exception {
        UUID courierId = UUID.randomUUID();
        given(workloadService.getWorkload()).willReturn(List.of(
                new CourierWorkloadResponse(courierId, UUID.randomUUID(), "Kasun", CourierStatus.AVAILABLE, 3)));

        mockMvc.perform(get("/api/v1/dispatch/workload").with(user("DISPATCHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courierId").value(courierId.toString()))
                .andExpect(jsonPath("$[0].firstName").value("Kasun"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].activeDeliveries").value(3));
    }

    @Test
    void adminCanViewWorkload() throws Exception {
        given(workloadService.getWorkload()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/dispatch/workload").with(user("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void courierCannotViewOtherCouriersWorkload() throws Exception {
        mockMvc.perform(get("/api/v1/dispatch/workload").with(user("COURIER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(workloadService);
    }

    @Test
    void rejectsRequestWithoutTokenWith401() throws Exception {
        mockMvc.perform(get("/api/v1/dispatch/workload"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(workloadService);
    }

    private static RequestPostProcessor user(String role) {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
