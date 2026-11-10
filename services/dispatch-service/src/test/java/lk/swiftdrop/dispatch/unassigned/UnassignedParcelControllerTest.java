package lk.swiftdrop.dispatch.unassigned;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.common.error.ServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(UnassignedParcelController.class)
class UnassignedParcelControllerTest {

    private static final String TOKEN = "dispatcher-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UnassignedParcelService unassignedParcelService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void dispatcherSeesParcelsWaitingForACourier() throws Exception {
        UUID parcelId = UUID.randomUUID();
        given(unassignedParcelService.getUnassignedParcels(TOKEN)).willReturn(List.of(new UnassignedParcelResponse(
                parcelId, "SD-0001", "Colombo 03", "Kandy", "SMALL", new BigDecimal("1.50"), Instant.now())));

        mockMvc.perform(get("/api/v1/dispatch/unassigned").with(user("DISPATCHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].parcelId").value(parcelId.toString()))
                .andExpect(jsonPath("$[0].trackingNo").value("SD-0001"))
                .andExpect(jsonPath("$[0].weightKg").value(1.50));
    }

    @Test
    void adminCanViewUnassignedParcels() throws Exception {
        given(unassignedParcelService.getUnassignedParcels(TOKEN)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/dispatch/unassigned").with(user("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void courierCannotViewUnassignedParcels() throws Exception {
        mockMvc.perform(get("/api/v1/dispatch/unassigned").with(user("COURIER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(unassignedParcelService);
    }

    @Test
    void customerCannotViewUnassignedParcels() throws Exception {
        mockMvc.perform(get("/api/v1/dispatch/unassigned").with(user("CUSTOMER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(unassignedParcelService);
    }

    @Test
    void rejectsRequestWithoutTokenWith401() throws Exception {
        mockMvc.perform(get("/api/v1/dispatch/unassigned"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(unassignedParcelService);
    }

    @Test
    void parcelServiceDownGives503() throws Exception {
        given(unassignedParcelService.getUnassignedParcels(TOKEN))
                .willThrow(new ServiceUnavailableException("parcel-service is not available right now."));

        mockMvc.perform(get("/api/v1/dispatch/unassigned").with(user("DISPATCHER")))
                .andExpect(status().isServiceUnavailable());
    }

    private static RequestPostProcessor user(String role) {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).tokenValue(TOKEN))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
