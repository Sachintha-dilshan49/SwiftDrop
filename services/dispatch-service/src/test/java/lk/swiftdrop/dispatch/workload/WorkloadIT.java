package lk.swiftdrop.dispatch.workload;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.dispatch.assignment.Assignment;
import lk.swiftdrop.dispatch.assignment.AssignmentRepository;
import lk.swiftdrop.dispatch.assignment.AssignmentStatus;
import lk.swiftdrop.dispatch.courier.Courier;
import lk.swiftdrop.dispatch.courier.CourierRepository;
import lk.swiftdrop.dispatch.courier.CourierStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Workload against real PostgreSQL and a fake auth-service. */
@SpringBootTest
@Testcontainers
class WorkloadIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @RegisterExtension
    static WireMockExtension authService = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("swiftdrop.services.auth-url", authService::baseUrl);
    }

    @Autowired
    private WorkloadService workloadService;

    @Autowired
    private CourierRepository courierRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Test
    void countsActiveDeliveriesPerCourierLeastBusyFirstWithNames() {
        Courier busy = courier(CourierStatus.BUSY, "Kasun");
        Courier idle = courier(CourierStatus.AVAILABLE, "Nadeesha");
        assign(busy, AssignmentStatus.ASSIGNED);
        assign(busy, AssignmentStatus.IN_PROGRESS);
        assign(busy, AssignmentStatus.COMPLETED);
        assign(idle, AssignmentStatus.FAILED);

        // only this test's couriers: the table also holds the demo courier seeded by V4
        List<CourierWorkloadResponse> workload = workloadService.getWorkload().stream()
                .filter(row -> row.courierId().equals(busy.getId()) || row.courierId().equals(idle.getId()))
                .toList();

        assertThat(workload).extracting(CourierWorkloadResponse::courierId).containsExactly(idle.getId(), busy.getId());
        assertThat(workload).extracting(CourierWorkloadResponse::activeDeliveries).containsExactly(0L, 2L);
        assertThat(workload).extracting(CourierWorkloadResponse::firstName).containsExactly("Nadeesha", "Kasun");
        assertThat(workload.get(1).status()).isEqualTo(CourierStatus.BUSY);
    }

    private Courier courier(CourierStatus status, String firstName) {
        Courier courier = new Courier();
        courier.setUserId(UUID.randomUUID());
        courier.setStatus(status);
        authService.stubFor(get(urlPathEqualTo("/internal/users/" + courier.getUserId() + "/contact"))
                .willReturn(okJson("{\"firstName\":\"" + firstName + "\"}")));
        return courierRepository.save(courier);
    }

    private void assign(Courier courier, AssignmentStatus status) {
        Assignment assignment = new Assignment();
        assignment.setParcelId(UUID.randomUUID());
        assignment.setCourierId(courier.getId());
        assignment.setAssignedByUserId(UUID.randomUUID());
        assignment.setStatus(status);
        assignmentRepository.save(assignment);
    }
}
