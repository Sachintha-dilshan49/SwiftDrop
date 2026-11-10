package lk.swiftdrop.dispatch.unassigned;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.UUID;
import lk.swiftdrop.dispatch.assignment.Assignment;
import lk.swiftdrop.dispatch.assignment.AssignmentRepository;
import lk.swiftdrop.dispatch.assignment.AssignmentStatus;
import lk.swiftdrop.dispatch.courier.Courier;
import lk.swiftdrop.dispatch.courier.CourierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Unassigned list against real PostgreSQL and a fake parcel-service. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UnassignedParcelsIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @RegisterExtension
    static WireMockExtension parcelService = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("swiftdrop.services.parcel-url", parcelService::baseUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourierRepository courierRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Test
    void onlyParcelsWithoutAnActiveAssignmentAreListed() throws Exception {
        UUID waiting = UUID.randomUUID();
        UUID assigned = UUID.randomUUID();
        UUID failedEarlier = UUID.randomUUID();
        Courier courier = new Courier();
        courier.setUserId(UUID.randomUUID());
        courier = courierRepository.save(courier);
        assign(courier, assigned, AssignmentStatus.ASSIGNED);
        assign(courier, failedEarlier, AssignmentStatus.FAILED);
        parcelService.stubFor(get(urlPathEqualTo("/api/v1/parcels")).willReturn(okJson("""
                {"content":[%s,%s,%s],"page":0,"size":100,"totalElements":3}
                """.formatted(parcel(waiting, "SD-1"), parcel(assigned, "SD-2"), parcel(failedEarlier, "SD-3")))));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/dispatch/unassigned")
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_DISPATCHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].trackingNo").value("SD-1"))
                .andExpect(jsonPath("$[1].trackingNo").value("SD-3"));
    }

    private void assign(Courier courier, UUID parcelId, AssignmentStatus status) {
        Assignment assignment = new Assignment();
        assignment.setParcelId(parcelId);
        assignment.setCourierId(courier.getId());
        assignment.setAssignedByUserId(UUID.randomUUID());
        assignment.setStatus(status);
        assignmentRepository.save(assignment);
    }

    private static String parcel(UUID id, String trackingNo) {
        return """
                {"id":"%s","trackingNo":"%s","status":"CREATED","pickupAddress":"Colombo 03",
                 "dropoffAddress":"Kandy","size":"SMALL","weightKg":1.50,"createdAt":"2026-11-10T04:30:00Z"}
                """.formatted(id, trackingNo);
    }
}
