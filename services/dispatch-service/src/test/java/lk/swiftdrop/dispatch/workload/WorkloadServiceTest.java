package lk.swiftdrop.dispatch.workload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.swiftdrop.common.error.ServiceUnavailableException;
import lk.swiftdrop.dispatch.assignment.AssignmentStatus;
import lk.swiftdrop.dispatch.courier.AuthServiceClient;
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

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private WorkloadService workloadService;

    @Test
    void addsEachCouriersFirstNameKeepingTheOrder() {
        CourierLoad idle = load(0);
        CourierLoad busy = load(3);
        given(workloadRepository.findWorkload(AssignmentStatus.ACTIVE)).willReturn(List.of(idle, busy));
        given(authServiceClient.firstName(idle.userId())).willReturn(Optional.of("Kasun"));
        given(authServiceClient.firstName(busy.userId())).willReturn(Optional.empty());

        List<CourierWorkloadResponse> workload = workloadService.getWorkload();

        assertThat(workload).extracting(CourierWorkloadResponse::courierId)
                .containsExactly(idle.courierId(), busy.courierId());
        assertThat(workload).extracting(CourierWorkloadResponse::firstName).containsExactly("Kasun", null);
        assertThat(workload.get(1).activeDeliveries()).isEqualTo(3);
    }

    @Test
    void authServiceDownStillReturnsTheWorkloadWithoutAskingAgain() {
        given(workloadRepository.findWorkload(AssignmentStatus.ACTIVE)).willReturn(List.of(load(0), load(1), load(2)));
        given(authServiceClient.firstName(any())).willThrow(new ServiceUnavailableException("auth-service is down"));

        List<CourierWorkloadResponse> workload = workloadService.getWorkload();

        assertThat(workload).hasSize(3).extracting(CourierWorkloadResponse::firstName).containsOnlyNulls();
        verify(authServiceClient, times(1)).firstName(any());
    }

    private static CourierLoad load(long activeDeliveries) {
        return new CourierLoad(UUID.randomUUID(), UUID.randomUUID(), CourierStatus.AVAILABLE, activeDeliveries);
    }
}
