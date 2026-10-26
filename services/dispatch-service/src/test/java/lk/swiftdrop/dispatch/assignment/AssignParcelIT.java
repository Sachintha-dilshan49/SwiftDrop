package lk.swiftdrop.dispatch.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lk.swiftdrop.dispatch.courier.Courier;
import lk.swiftdrop.dispatch.courier.CourierRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AssignParcelIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourierRepository courierRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Test
    void savesAssignmentAndRejectsSecondActiveAssignmentForSameParcel() throws Exception {
        Courier courier = new Courier();
        courier.setUserId(UUID.randomUUID());
        courier = courierRepository.save(courier);
        UUID parcelId = UUID.randomUUID();

        mockMvc.perform(assign(parcelId, courier.getId())).andExpect(status().isCreated());
        mockMvc.perform(assign(parcelId, courier.getId())).andExpect(status().isConflict());

        assertThat(assignmentRepository.existsByParcelIdAndStatusIn(parcelId, AssignmentStatus.ACTIVE)).isTrue();
        assertThat(assignmentRepository.count()).isEqualTo(1);
    }

    private static MockHttpServletRequestBuilder assign(UUID parcelId, UUID courierId) {
        return post("/api/v1/dispatch/assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parcelId\":\"" + parcelId + "\",\"courierId\":\"" + courierId + "\"}")
                .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_DISPATCHER")));
    }
}
