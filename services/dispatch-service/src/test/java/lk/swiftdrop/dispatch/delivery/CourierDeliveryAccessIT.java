package lk.swiftdrop.dispatch.delivery;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lk.swiftdrop.dispatch.assignment.Assignment;
import lk.swiftdrop.dispatch.assignment.AssignmentRepository;
import lk.swiftdrop.dispatch.courier.Courier;
import lk.swiftdrop.dispatch.courier.CourierRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** The authorization test the brief asks for: a courier cannot access another courier's deliveries. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CourierDeliveryAccessIT {

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
    void courierCannotAccessAnotherCouriersDeliveries() throws Exception {
        Courier kamal = courier();
        Courier nimal = courier();
        Assignment kamalsDelivery = assign(kamal);

        mockMvc.perform(get("/api/v1/courier/deliveries/{id}", kamalsDelivery.getId()).with(signedInAs(nimal)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/courier/deliveries/today").with(signedInAs(nimal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/api/v1/courier/deliveries/{id}", kamalsDelivery.getId()).with(signedInAs(kamal)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/courier/deliveries/today").with(signedInAs(kamal)))
                .andExpect(jsonPath("$[0].assignmentId").value(kamalsDelivery.getId().toString()));
    }

    private Courier courier() {
        Courier courier = new Courier();
        courier.setUserId(UUID.randomUUID());
        return courierRepository.save(courier);
    }

    private Assignment assign(Courier courier) {
        Assignment assignment = new Assignment();
        assignment.setParcelId(UUID.randomUUID());
        assignment.setCourierId(courier.getId());
        assignment.setAssignedByUserId(UUID.randomUUID());
        return assignmentRepository.save(assignment);
    }

    private static RequestPostProcessor signedInAs(Courier courier) {
        return jwt().jwt(token -> token.subject(courier.getUserId().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_COURIER"));
    }
}
