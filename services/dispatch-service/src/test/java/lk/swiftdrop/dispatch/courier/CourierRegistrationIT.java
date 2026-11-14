package lk.swiftdrop.dispatch.courier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
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

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CourierRegistrationIT {

    /** The courier V4 seeded for a user no auth-service account has; V5 removes it. */
    private static final UUID ORPHAN_DEMO_COURIER = UUID.fromString("00000000-0000-4000-8000-00000000d001");

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourierRepository courierRepository;

    @Test
    void newCourierAccountCanOpenTodaysDeliveriesAndIsRegisteredOnce() throws Exception {
        UUID userId = UUID.randomUUID();

        for (int visit = 0; visit < 2; visit++) {
            mockMvc.perform(get("/api/v1/courier/deliveries/today").with(courier(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        assertThat(courierRepository.findByUserId(userId))
                .hasValueSatisfying(courier -> assertThat(courier.getStatus()).isEqualTo(CourierStatus.AVAILABLE));
        assertThat(courierRepository.findAll()).filteredOn(courier -> courier.getUserId().equals(userId)).hasSize(1);
    }

    @Test
    void orphanDemoCourierIsRemoved() {
        assertThat(courierRepository.findById(ORPHAN_DEMO_COURIER)).isEmpty();
    }

    private static RequestPostProcessor courier(UUID userId) {
        return jwt().jwt(token -> token.subject(userId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_COURIER"));
    }
}
