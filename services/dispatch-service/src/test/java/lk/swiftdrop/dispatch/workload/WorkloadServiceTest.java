package lk.swiftdrop.dispatch.workload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.UUID;
import lk.swiftdrop.dispatch.assignment.AssignmentStatus;
import lk.swiftdrop.dispatch.courier.CourierStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkloadServiceTest {

    @Mock
    private WorkloadRepository workloadRepository;

    @InjectMocks
    private WorkloadService workloadService;

    @Test
    void countsOnlyAssignedAndInProgressDeliveries() {
        List<CourierWorkloadResponse> rows = List.of(
                new CourierWorkloadResponse(UUID.randomUUID(), UUID.randomUUID(), CourierStatus.AVAILABLE, 0));
        given(workloadRepository.findWorkload(AssignmentStatus.ACTIVE)).willReturn(rows);

        assertThat(workloadService.getWorkload()).isEqualTo(rows);
    }
}
