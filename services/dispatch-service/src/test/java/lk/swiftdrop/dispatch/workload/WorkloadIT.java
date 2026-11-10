package lk.swiftdrop.dispatch.workload;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import lk.swiftdrop.dispatch.assignment.Assignment;
import lk.swiftdrop.dispatch.assignment.AssignmentRepository;
import lk.swiftdrop.dispatch.assignment.AssignmentStatus;
import lk.swiftdrop.dispatch.courier.Courier;
import lk.swiftdrop.dispatch.courier.CourierRepository;
import lk.swiftdrop.dispatch.courier.CourierStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class WorkloadIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private WorkloadService workloadService;

    @Autowired
    private CourierRepository courierRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Test
    void countsActiveDeliveriesPerCourierLeastBusyFirst() {
        Courier busy = courier(CourierStatus.BUSY);
        Courier idle = courier(CourierStatus.AVAILABLE);
        assign(busy, AssignmentStatus.ASSIGNED);
        assign(busy, AssignmentStatus.IN_PROGRESS);
        assign(busy, AssignmentStatus.COMPLETED);
        assign(idle, AssignmentStatus.FAILED);

        List<CourierWorkloadResponse> workload = workloadService.getWorkload();

        assertThat(workload).extracting(CourierWorkloadResponse::courierId).containsExactly(idle.getId(), busy.getId());
        assertThat(workload).extracting(CourierWorkloadResponse::activeDeliveries).containsExactly(0L, 2L);
        assertThat(workload.get(1).status()).isEqualTo(CourierStatus.BUSY);
    }

    private Courier courier(CourierStatus status) {
        Courier courier = new Courier();
        courier.setUserId(UUID.randomUUID());
        courier.setStatus(status);
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
