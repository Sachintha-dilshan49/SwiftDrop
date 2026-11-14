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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** The demo courier from V4 exists, so the demo COURIER account can open the courier screens. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DemoCourierSeedIT {

    /** Must match the demo COURIER user seeded in auth-service. */
    private static final UUID DEMO_COURIER_USER_ID = UUID.fromString("00000000-0000-4000-8000-00000000c001");

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourierRepository courierRepository;

    @Test
    void demoCourierIsSeededAvailable() {
        assertThat(courierRepository.findByUserId(DEMO_COURIER_USER_ID))
                .hasValueSatisfying(courier -> assertThat(courier.getStatus()).isEqualTo(CourierStatus.AVAILABLE));
    }

    @Test
    void demoCourierCanOpenTodaysDeliveries() throws Exception {
        mockMvc.perform(get("/api/v1/courier/deliveries/today")
                        .with(jwt().jwt(token -> token.subject(DEMO_COURIER_USER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_COURIER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
